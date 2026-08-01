// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.accessibility

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aieonyx.aistop.ui.MainActivity
import com.aieonyx.aistop.vpn.AllowlistManager
import com.aieonyx.aistop.vpn.AppBlockList
import com.aieonyx.aistop.vpn.VpnIntegration

object AiAppWarning {

    private const val CHANNEL_ID = "aistop_app_warning"
    const val NOTIF_ID   = 2001
    private const val PREFS_EXEMPTED = "aistop_exempted_packages"

    private val bypassingApps: Map<String, BypassInfo> = mapOf(
        "com.google.android.googlequicksearchbox" to BypassInfo("Gemini",
            "gemini.google.com"),
        "com.google.android.apps.bard" to BypassInfo("Gemini",
            "bard.google.com"),
        "com.openai.chatgpt" to BypassInfo("ChatGPT",
            "openai.com"),
        "com.anthropic.claude" to BypassInfo("Claude",
            "anthropic.com"),
        "com.microsoft.copilot" to BypassInfo("Microsoft Copilot",
            "copilot.microsoft.com"),
        "com.microsoft.bing" to BypassInfo("Bing AI",
            "bing.com"),
        "com.deepseek.chat" to BypassInfo("DeepSeek",
            "deepseek.com"),
        "ai.perplexity.app.android" to BypassInfo("Perplexity AI",
            "perplexity.ai"),
        "com.facebook.katana" to BypassInfo("Facebook",
            "graph.facebook.com"),
        "com.instagram.android" to BypassInfo("Instagram",
            "instagram.com"),
        "com.grammarly.android.keyboard" to BypassInfo("Grammarly",
            "grammarly.com"),
        "com.moonshot.kimi" to BypassInfo("Kimi AI",
            "kimi.ai"),
        "ai.character.app" to BypassInfo("Character AI",
            "character.ai"),
        "com.quora.poe" to BypassInfo("Poe",
            "poe.com")
    )

    data class BypassInfo(val label: String, val primaryDomain: String)

    private var lastWarnedPackage = ""
    private var lastWarnedTimeMs  = 0L
    private const val DEBOUNCE_MS = 30_000L

    // ── Exempt list ───────────────────────────────────────────────────────────

    fun exemptPackage(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS_EXEMPTED, Context.MODE_PRIVATE)
        val set   = prefs.getStringSet("exempted", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        set.add(packageName)
        prefs.edit().putStringSet("exempted", set).apply()
    }

    fun revokeExemption(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS_EXEMPTED, Context.MODE_PRIVATE)
        val set   = prefs.getStringSet("exempted", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        set.remove(packageName)
        prefs.edit().putStringSet("exempted", set).apply()
    }

    private fun isExempted(context: Context, packageName: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_EXEMPTED, Context.MODE_PRIVATE)
        return prefs.getStringSet("exempted", emptySet())?.contains(packageName) == true
    }

    // ── Suppress check ────────────────────────────────────────────────────────

    private val packageDomains: Map<String, List<String>> = mapOf(
        "com.google.android.googlequicksearchbox" to listOf("gemini.google.com","bard.google.com","generativelanguage.googleapis.com"),
        "com.google.android.apps.bard"            to listOf("gemini.google.com","bard.google.com"),
        "com.openai.chatgpt"                      to listOf("openai.com","chatgpt.com"),
        "com.anthropic.claude"                    to listOf("anthropic.com","claude.ai"),
        "com.microsoft.copilot"                   to listOf("copilot.microsoft.com"),
        "com.deepseek.chat"                       to listOf("deepseek.com","chat.deepseek.com"),
        "ai.perplexity.app.android"               to listOf("perplexity.ai"),
        "com.facebook.katana"                     to listOf("facebook.com","graph.facebook.com","connect.facebook.net"),
        "com.instagram.android"                   to listOf("instagram.com","facebook.com"),
        "com.grammarly.android.keyboard"          to listOf("grammarly.com"),
        "com.moonshot.kimi"                       to listOf("kimi.ai","moonshot.cn"),
        "ai.character.app"                        to listOf("character.ai"),
        "com.quora.poe"                           to listOf("poe.com")
    )

    private fun isSuppressed(context: Context, packageName: String): Boolean {
        if (isExempted(context, packageName)) return true
        if (AppBlockList.isBlocked(packageName)) return true
        val domains = packageDomains[packageName] ?: return false
        return domains.any { domain ->
            val entry = AllowlistManager.getEntry(domain)
            entry != null && entry.duration == AllowlistManager.Duration.PERMANENT
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun onForegroundAppChanged(context: Context, packageName: String) {
        val info = bypassingApps[packageName] ?: return
        if (!VpnIntegration.isRunning(context)) return
        if (isSuppressed(context, packageName)) return

        val now = System.currentTimeMillis()
        if (packageName == lastWarnedPackage && (now - lastWarnedTimeMs) < DEBOUNCE_MS) return

        lastWarnedPackage = packageName
        lastWarnedTimeMs  = now

        showWarning(context, packageName, info)
    }

    fun onForegroundAppLeft(context: Context, packageName: String) {
        if (bypassingApps.containsKey(packageName)) {
            dismiss(context)
            lastWarnedPackage = ""
        }
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun showWarning(context: Context, packageName: String, info: BypassInfo) {
        createChannel(context)

        // Tap body → open MORE tab in MainActivity
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_TAB
                putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_MORE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // EXEMPT action
        val exemptIntent = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_EXEMPT
                putExtra(NotificationActionReceiver.EXTRA_PACKAGE, packageName)
                putExtra(NotificationActionReceiver.EXTRA_LABEL,   info.label)
                putExtra(NotificationActionReceiver.EXTRA_DOMAIN,  info.primaryDomain)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // BLOCK APP action
        val blockIntent = PendingIntent.getBroadcast(
            context, 2,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_BLOCK_APP
                putExtra(NotificationActionReceiver.EXTRA_PACKAGE, packageName)
                putExtra(NotificationActionReceiver.EXTRA_LABEL,   info.label)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠ ${info.label} bypasses Sovereign Shield")
            .setContentText("DNS filter cannot block this app.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("DNS filter cannot block this app.\nTap EXEMPT to silence · BLOCK to cut network.")
            )
            .setColor(0xFFFBBF24.toInt())
            .setColorized(true)
            .setContentIntent(openIntent)
            .addAction(0, "✓ EXEMPT", exemptIntent)
            .addAction(0, "🚫 BLOCK APP", blockIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, notification)
    }

    fun dismiss(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "AI App Warnings",
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Warns when an AI app bypasses Sovereign Shield"
                enableVibration(false)
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }
}
