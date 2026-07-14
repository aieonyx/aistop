// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.aieonyx.aistop.R
import com.aieonyx.aistop.core.TrustDatabase
import com.aieonyx.aistop.db.EdisonExposureDatabase as ExposureDatabase
import com.aieonyx.aistop.db.EventType
import com.aieonyx.aistop.db.ExposureEvent
import com.aieonyx.aistop.jni.AiStopCore
import com.aieonyx.aistop.vault.SovereignVault
import com.aieonyx.aistop.ui.SovereignMode
import com.aieonyx.aistop.ui.loadSovereignMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Sovereign Accessibility Service — mode-aware interception.
 *
 * AutoPilot → silent block + notification, no overlay
 * Default   → overlay only for high-confidence PII (API keys, SSN, creds)
 * Manual    → overlay always, user decides every time
 *
 * Zero keystroke logging. Monitors window focus + paste events only.
 * Only active for known AI app packages.
 */
class SovereignAccessibilityService : AccessibilityService() {

    private var currentPackage    = ""
    private var clipboardManager: ClipboardManager? = null
    private var lastSentinelText  = "" // debounce global clipboard scan

    // High-confidence PII classes — Default mode blocks without prompt
    private val highConfidenceClasses = setOf(
        "ApiKey", "AwsKey", "GitHubToken", "JWT", "PemKey",
        "Password", "Ssn", "CreditCard", "CryptoWallet", "PassportNumber"
    )

    companion object {
        private const val CHANNEL_ID      = "aistop_autopilot"
        private const val NOTIF_ID        = 1001
        const val ACTION_SAVE_VAULT       = "com.aieonyx.aistop.ACTION_SAVE_TO_VAULT"
        const val EXTRA_TEXT              = "vault_text"
        const val EXTRA_PII_CLASS         = "vault_pii_class"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        createNotificationChannel()

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_FOCUSED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType  = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags         = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                            AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
            // Monitor all packages for global clipboard sentinel
            // Known AI packages filtered in onAccessibilityEvent
            packageNames  = null
        }

        // Global clipboard sentinel — Accessibility Service CAN read clipboard
        // on Android 10+ unlike background services
        clipboardManager?.addPrimaryClipChangedListener {
            val prefs = getSharedPreferences("aistop_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("sentinel_enabled", false)) return@addPrimaryClipChangedListener
            val clip = clipboardManager?.primaryClip ?: return@addPrimaryClipChangedListener
            if (clip.itemCount == 0) return@addPrimaryClipChangedListener
            val text = clip.getItemAt(0).coerceToText(this).toString()
            if (text.isBlank() || text.length < 4 || text == lastSentinelText) return@addPrimaryClipChangedListener
            lastSentinelText = text
            scanClipboardGlobal(text)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return

        // Global sentinel — scan text changes in ANY app
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val prefs = getSharedPreferences("aistop_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("sentinel_enabled", false)) {
                val text = event.text.joinToString(" ").trim()
                if (text.length >= 4 && text != lastSentinelText) {
                    lastSentinelText = text
                    scanClipboardGlobal(text)
                }
            }
            return
        }

        // AI app specific clipboard monitoring
        if (pkg !in TrustDatabase.KNOWN_AI_PACKAGES) return
        currentPackage = pkg

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            checkClipboardForPii(pkg)
        }
    }

    private fun checkClipboardForPii(targetPackage: String) {
        val clip = clipboardManager?.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(this).toString()
        if (text.isBlank() || text.length < 5) return

        val mode = loadSovereignMode(this)

        CoroutineScope(Dispatchers.IO).launch {
            val result = runCatching { AiStopCore.piiDetect(text) }.getOrElse { return@launch }
            val hasPii = try { JSONObject(result).has("PiiFound") } catch (e: Exception) { false }
            if (!hasPii) return@launch

            val piiClasses  = parsePiiClasses(result)
            val trustScore  = calcTrustScore(targetPackage)
            val isHighConf  = piiClasses.any { it in highConfidenceClasses }

            when (mode) {

                SovereignMode.AUTOPILOT -> {
                    // Silent block — clear clipboard, notify, log
                    clearClipboard()
                    logEvent(targetPackage, EventType.PASTE_BLOCKED, text, piiClasses, trustScore)
                    showAutoPilotNotification(targetPackage, piiClasses)
                }

                SovereignMode.DEFAULT -> {
                    if (isHighConf) {
                        // High-confidence → show overlay
                        logEvent(targetPackage, EventType.PASTE_BLOCKED, text, piiClasses, trustScore)
                        launchOverlay(text, targetPackage, result)
                    } else {
                        // Low-confidence → silent log only, pass through
                        logEvent(targetPackage, EventType.PASTE_ALLOWED, text, piiClasses, trustScore)
                    }
                }

                SovereignMode.MANUAL -> {
                    // Always show overlay
                    logEvent(targetPackage, EventType.PASTE_BLOCKED, text, piiClasses, trustScore)
                    launchOverlay(text, targetPackage, result)
                }
            }
        }
    }

    private fun launchOverlay(text: String, targetPackage: String, detection: String) {
        val intent = Intent(
            this,
            com.aieonyx.aistop.ui.SovereignOverlayActivity::class.java
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("text",      text)
            putExtra("package",   targetPackage)
            putExtra("detection", detection)
        }
        startActivity(intent)
    }

    private fun clearClipboard() {
        try {
            clipboardManager?.setPrimaryClip(
                android.content.ClipData.newPlainText("", "")
            )
        } catch (e: Exception) { }
    }

    private fun showAutoPilotNotification(pkg: String, piiClasses: List<String>) {
        val label    = TrustDatabase.appLabel(pkg)
        val classes  = piiClasses.take(3).joinToString(", ")
        val nm       = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif    = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("AI Stop blocked sensitive data")
            .setContentText("$label — $classes cleared from clipboard")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID, notif)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AutoPilot blocks",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifies when AI Stop silently blocks sensitive data in AutoPilot mode"
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun logEvent(
        pkg:        String,
        eventType:  String,
        text:       String,
        piiClasses: List<String>,
        trustScore: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                ExposureDatabase.getInstance(this@SovereignAccessibilityService)
                    .exposureDao().insert(
                        ExposureEvent(
                            ts          = System.currentTimeMillis(),
                            packageName = pkg,
                            appLabel    = TrustDatabase.appLabel(pkg),
                            eventType   = eventType,
                            preview20   = text.take(20),
                            trustScore  = trustScore,
                            piiClasses  = piiClasses.joinToString(",")
                        )
                    )
            }
        }
    }

    private fun calcTrustScore(pkg: String): Int =
        TrustDatabase.entry(pkg).let {
            ((it.retentionScore * 0.4 + it.transparencyScore * 0.3 +
              it.optOutScore * 0.2) * 0.1).toInt().coerceIn(0, 100)
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

    private fun scanClipboardGlobal(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = runCatching { AiStopCore.piiDetect(text) }.getOrElse {
                android.util.Log.e("AiStopSentinel", "PII detect failed: ${it.message}")
                return@launch
            }
            android.util.Log.d("AiStopSentinel", "PII result: $result")
            val hasPii = try { JSONObject(result).has("PiiFound") } catch (e: Exception) { false }
            android.util.Log.d("AiStopSentinel", "Has PII: $hasPii")
            if (!hasPii) return@launch

            val piiClasses = parsePiiClasses(result)
            val mode       = loadSovereignMode(this@SovereignAccessibilityService)
            android.util.Log.d("AiStopSentinel", "PII classes: $piiClasses, mode: $mode")

            // Log to EdisonDB — non-fatal, don't let DB failure kill notification
            runCatching {
                logEvent("clipboard.sentinel", EventType.PASTE_BLOCKED, text, piiClasses, 0)
            }

            val primaryClass = piiClasses.firstOrNull() ?: ""
            when (mode) {
                SovereignMode.AUTOPILOT -> {
                    clearClipboard()
                    lastSentinelText = ""
                    showSentinelNotification(piiClasses, silent = true, text = text, piiClass = primaryClass)
                }
                SovereignMode.DEFAULT -> {
                    val highConf = setOf("ApiKey","AwsKey","GitHubToken","JWT",
                        "PemKey","Password","Ssn","CreditCard","CryptoWallet")
                    if (piiClasses.any { it in highConf }) {
                        showSentinelNotification(piiClasses, silent = false, text = text, piiClass = primaryClass)
                    }
                }
                SovereignMode.MANUAL -> {
                    showSentinelNotification(piiClasses, silent = false, text = text, piiClass = primaryClass)
                }
            }
        }
    }

    private fun showSentinelNotification(
        piiClasses: List<String>,
        silent:     Boolean,
        text:       String = "",
        piiClass:   String = ""
    ) {
        val nm      = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val classes = piiClasses.take(3).joinToString(", ")
        val title   = if (silent) "🛡 Clipboard cleared — sensitive data blocked"
                      else "⚠ Sensitive data detected in clipboard"

        // Save to Vault action
        val vaultIntent = android.app.PendingIntent.getBroadcast(
            this, 3001,
            android.content.Intent(ACTION_SAVE_VAULT).apply {
                `package` = packageName
                putExtra(EXTRA_TEXT, text)
                putExtra(EXTRA_PII_CLASS, piiClass)
            },
            android.app.PendingIntent.FLAG_IMMUTABLE or
            android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Open app action
        val openIntent = android.app.PendingIntent.getActivity(
            this, 3002,
            android.content.Intent(this, com.aieonyx.aistop.ui.MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Detected: $classes")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openIntent)

        // Add Save to Vault action for non-silent (Default/Manual modes)
        // and also for AutoPilot so user can recover cleared data
        if (text.isNotEmpty()) {
            builder.addAction(0, "🔐 Save to Vault", vaultIntent)
        }

        nm.notify(1002, builder.build())
    }

    override fun onInterrupt() {}
}
