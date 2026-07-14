// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.sentinel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aieonyx.aistop.R
import com.aieonyx.aistop.db.EdisonExposureDatabase as ExposureDatabase
import com.aieonyx.aistop.db.EventType
import com.aieonyx.aistop.db.ExposureEvent
import com.aieonyx.aistop.jni.AiStopCore
import com.aieonyx.aistop.ui.MainActivity
import com.aieonyx.aistop.ui.SovereignMode
import com.aieonyx.aistop.ui.loadSovereignMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * ClipboardSentinelService — always-on clipboard guardian.
 *
 * Runs as a foreground service. Listens to every clipboard change
 * system-wide and scans for PII before the user pastes anywhere.
 *
 * Mode behaviour:
 *   AutoPilot → silent clear + notification
 *   Default   → notification with action buttons (high-confidence only)
 *   Manual    → notification with action buttons (always)
 */
class ClipboardSentinelService : Service() {

    private var clipboardManager: ClipboardManager? = null
    private var clipListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // Debounce — avoid duplicate scans on rapid clipboard changes
    private var lastScannedText = ""

    companion object {
        const val CHANNEL_SENTINEL    = "aistop_sentinel"
        const val CHANNEL_ALERTS      = "aistop_alerts"
        const val NOTIF_SENTINEL_ID   = 2001
        const val NOTIF_ALERT_ID      = 2002

        const val ACTION_CLEAR        = "com.aieonyx.aistop.ACTION_CLEAR_CLIPBOARD"
        const val ACTION_DISMISS      = "com.aieonyx.aistop.ACTION_DISMISS_ALERT"

        fun start(context: Context) {
            val intent = Intent(context, ClipboardSentinelService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ClipboardSentinelService::class.java)
            context.stopService(intent)
        }

        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE)
                as android.app.ActivityManager
            return manager.getRunningServices(Integer.MAX_VALUE)
                .any { it.service.className == ClipboardSentinelService::class.java.name }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForeground(NOTIF_SENTINEL_ID, buildPersistentNotification())
        startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CLEAR   -> clearClipboard()
            ACTION_DISMISS -> dismissAlert()
        }
        return START_STICKY // restart if killed
    }

    override fun onDestroy() {
        clipListener?.let { clipboardManager?.removePrimaryClipChangedListener(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Clipboard listener ────────────────────────────────────────────────────

    private fun startListening() {
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipListener = ClipboardManager.OnPrimaryClipChangedListener {
            val text = try {
                clipboardManager?.primaryClip
                    ?.getItemAt(0)
                    ?.coerceToText(this)
                    ?.toString() ?: ""
            } catch (e: Exception) { "" }

            if (text.isBlank() || text.length < 4 || text == lastScannedText) return@OnPrimaryClipChangedListener
            lastScannedText = text
            scanClipboard(text)
        }
        clipboardManager?.addPrimaryClipChangedListener(clipListener!!)
    }

    // ── PII scan ──────────────────────────────────────────────────────────────

    private fun scanClipboard(text: String) {
        scope.launch {
            val result = runCatching { AiStopCore.piiDetect(text) }.getOrElse { return@launch }
            val hasPii = try { JSONObject(result).has("PiiFound") } catch (e: Exception) { false }
            if (!hasPii) return@launch

            val piiClasses = parsePiiClasses(result)
            val mode       = loadSovereignMode(this@ClipboardSentinelService)

            // Log to EdisonDB
            logEvent(text, piiClasses, EventType.PASTE_BLOCKED)

            withContext(Dispatchers.Main) {
                when (mode) {
                    SovereignMode.AUTOPILOT -> {
                        clearClipboard()
                        showAutoPilotAlert(piiClasses)
                    }
                    SovereignMode.DEFAULT -> {
                        val highConfidence = setOf(
                            "ApiKey", "AwsKey", "GitHubToken", "JWT",
                            "PemKey", "Password", "Ssn", "CreditCard"
                        )
                        if (piiClasses.any { it in highConfidence }) {
                            showActionAlert(text, piiClasses)
                        }
                    }
                    SovereignMode.MANUAL -> {
                        showActionAlert(text, piiClasses)
                    }
                }
            }
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private fun buildPersistentNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_SENTINEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("AI Stop Sentinel")
            .setContentText("Monitoring clipboard for sensitive data")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showAutoPilotAlert(piiClasses: List<String>) {
        val nm     = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val classes = piiClasses.take(3).joinToString(", ")
        val notif  = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🛡 Clipboard cleared — sensitive data blocked")
            .setContentText("Detected: $classes")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(NOTIF_ALERT_ID, notif)
    }

    private fun showActionAlert(text: String, piiClasses: List<String>) {
        val nm      = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val classes = piiClasses.take(3).joinToString(", ")

        val clearIntent = PendingIntent.getService(
            this, 1,
            Intent(this, ClipboardSentinelService::class.java).apply {
                action = ACTION_CLEAR
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val dismissIntent = PendingIntent.getService(
            this, 2,
            Intent(this, ClipboardSentinelService::class.java).apply {
                action = ACTION_DISMISS
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openIntent = PendingIntent.getActivity(
            this, 3,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠ Sensitive data in clipboard")
            .setContentText("Detected: $classes")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Your clipboard contains: $classes\n\nPreview: ${text.take(40)}…\n\nClear it before pasting into an AI app.")
            )
            .addAction(0, "🗑 Clear clipboard", clearIntent)
            .addAction(0, "Dismiss", dismissIntent)
            .setContentIntent(openIntent)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .build()
        nm.notify(NOTIF_ALERT_ID, notif)
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private fun clearClipboard() {
        try {
            val cm   = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = android.content.ClipData.newPlainText("", "")
            val extras = android.os.PersistableBundle().apply {
                putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
            clip.description.extras = extras
            cm.setPrimaryClip(clip)
            lastScannedText = ""
        } catch (e: Exception) { }
    }

    private fun dismissAlert() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ALERT_ID)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun createChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Persistent sentinel channel (silent, low priority)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SENTINEL,
                "Sentinel Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AI Stop clipboard monitoring status"
                setShowBadge(false)
            }
        )

        // Alert channel (high priority, sound + vibration)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                "Sentinel Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when sensitive data is detected in clipboard"
                enableVibration(true)
                enableLights(true)
            }
        )
    }

    private fun parsePiiClasses(json: String): List<String> {
        return try {
            val obj = JSONObject(json)
            if (!obj.has("PiiFound")) return emptyList()
            val matches = obj.getJSONObject("PiiFound").getJSONArray("matches")
            (0 until matches.length())
                .map { matches.getJSONObject(it).getString("class") }
                .distinct()
        } catch (e: Exception) { emptyList() }
    }

    private fun logEvent(text: String, piiClasses: List<String>, eventType: String) {
        scope.launch {
            runCatching {
                ExposureDatabase.getInstance(this@ClipboardSentinelService)
                    .exposureDao().insert(
                        ExposureEvent(
                            ts          = System.currentTimeMillis(),
                            packageName = "com.aieonyx.aistop.sentinel",
                            appLabel    = "Clipboard Sentinel",
                            eventType   = eventType,
                            preview20   = text.take(20),
                            trustScore  = 0,
                            piiClasses  = piiClasses.joinToString(",")
                        )
                    )
            }
        }
    }
}
