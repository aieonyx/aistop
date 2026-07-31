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

/**
 * AiAppWarning — fires a persistent notification when a known AI app
 * comes to foreground while Sovereign Shield VPN is active.
 *
 * Why: AI apps (Gemini, ChatGPT, DeepSeek, etc.) use hardcoded IPs or
 * DNS-over-HTTPS, bypassing our DNS filter. The user must know Shield
 * cannot protect them inside these apps.
 *
 * Shown once per app session (debounced by lastWarnedPackage).
 * Dismissed automatically when user leaves the AI app.
 */
object AiAppWarning {

    private const val CHANNEL_ID = "aistop_app_warning"
    private const val NOTIF_ID   = 2001

    // Apps that bypass DNS filtering — hardcoded IPs or DoH
    private val bypassingApps: Map<String, BypassInfo> = mapOf(

        // ── Google AI ─────────────────────────────────────────────────────
        "com.google.android.googlequicksearchbox" to BypassInfo(
            label    = "Gemini",
            reason   = "Google AI apps use hardcoded IP addresses that bypass DNS filtering.",
            risk     = "Conversations, clipboard content, and personal data may be sent to Google AI servers and used for model training."
        ),
        "com.google.android.apps.bard" to BypassInfo(
            label    = "Gemini",
            reason   = "Google AI apps use hardcoded IP addresses that bypass DNS filtering.",
            risk     = "Conversations, clipboard content, and personal data may be sent to Google AI servers and used for model training."
        ),
        "com.google.android.apps.gemini" to BypassInfo(
            label    = "Gemini",
            reason   = "Google AI apps use hardcoded IP addresses that bypass DNS filtering.",
            risk     = "Conversations and personal data may be sent to Google AI servers."
        ),

        // ── OpenAI / ChatGPT ──────────────────────────────────────────────
        "com.openai.chatgpt" to BypassInfo(
            label    = "ChatGPT",
            reason   = "ChatGPT uses DNS-over-HTTPS, bypassing Sovereign Shield DNS filter.",
            risk     = "All conversations are sent to OpenAI servers. Data may be used for training unless opted out in settings."
        ),

        // ── Anthropic / Claude ────────────────────────────────────────────
        "com.anthropic.claude" to BypassInfo(
            label    = "Claude",
            reason   = "Claude app uses DNS-over-HTTPS, bypassing Sovereign Shield DNS filter.",
            risk     = "Conversations sent to Anthropic servers. Review privacy settings."
        ),

        // ── Microsoft Copilot ─────────────────────────────────────────────
        "com.microsoft.copilot" to BypassInfo(
            label    = "Microsoft Copilot",
            reason   = "Copilot uses hardcoded Microsoft endpoints bypassing DNS filtering.",
            risk     = "Data may flow between Microsoft and OpenAI infrastructure."
        ),
        "com.microsoft.bing" to BypassInfo(
            label    = "Bing AI",
            reason   = "Bing uses hardcoded Microsoft endpoints bypassing DNS filtering.",
            risk     = "Search queries and AI conversations sent to Microsoft servers."
        ),

        // ── DeepSeek ─────────────────────────────────────────────────────
        "com.deepseek.chat" to BypassInfo(
            label    = "DeepSeek",
            reason   = "DeepSeek uses DNS-over-HTTPS, bypassing Sovereign Shield DNS filter.",
            risk     = "Conversations sent to servers in China. Data used to train DeepSeek models by default. Chinese data jurisdiction applies."
        ),

        // ── Perplexity ────────────────────────────────────────────────────
        "ai.perplexity.app.android" to BypassInfo(
            label    = "Perplexity AI",
            reason   = "Perplexity uses hardcoded endpoints bypassing DNS filtering.",
            risk     = "Search queries and conversations sent to Perplexity servers."
        ),

        // ── Meta AI ───────────────────────────────────────────────────────
        "com.facebook.katana" to BypassInfo(
            label    = "Facebook (Meta AI)",
            reason   = "Facebook uses hardcoded Meta CDN IPs bypassing DNS filtering.",
            risk     = "Meta AI interactions and behavioral data collected for ad targeting and AI training."
        ),
        "com.instagram.android" to BypassInfo(
            label    = "Instagram (Meta AI)",
            reason   = "Instagram uses hardcoded Meta CDN IPs bypassing DNS filtering.",
            risk     = "Meta AI features collect behavioral data for ad targeting."
        ),

        // ── Grammarly ─────────────────────────────────────────────────────
        "com.grammarly.android.keyboard" to BypassInfo(
            label    = "Grammarly",
            reason   = "Grammarly uses DNS-over-HTTPS, bypassing Sovereign Shield DNS filter.",
            risk     = "All typed text is sent to Grammarly servers for AI processing."
        ),

        // ── Kimi ──────────────────────────────────────────────────────────
        "com.moonshot.kimi" to BypassInfo(
            label    = "Kimi AI",
            reason   = "Kimi uses hardcoded endpoints bypassing DNS filtering.",
            risk     = "Conversations sent to Moonshot AI servers in China."
        ),

        // ── Character AI ──────────────────────────────────────────────────
        "ai.character.app" to BypassInfo(
            label    = "Character AI",
            reason   = "Character AI uses DNS-over-HTTPS bypassing DNS filtering.",
            risk     = "Conversations sent to Character AI servers and may be used for training."
        ),

        // ── Poe ───────────────────────────────────────────────────────────
        "com.quora.poe" to BypassInfo(
            label    = "Poe",
            reason   = "Poe aggregates multiple AI models and bypasses DNS filtering.",
            risk     = "Conversations routed through multiple AI providers."
        )
    )

    data class BypassInfo(
        val label:  String,
        val reason: String,
        val risk:   String
    )

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
        if (AppBlockList.isBlocked(packageName)) return true
        val domains = packageDomains[packageName] ?: return false
        return domains.any { domain ->
            val entry = AllowlistManager.getEntry(domain)
            entry != null && entry.duration == AllowlistManager.Duration.PERMANENT
        }
    }

    private var lastWarnedPackage = ""
    private var lastWarnedTimeMs  = 0L
    private const val DEBOUNCE_MS = 30_000L  // warn at most once per 30s per app

    /**
     * Call from SovereignAccessibilityService.onAccessibilityEvent
     * when TYPE_WINDOW_STATE_CHANGED fires with a new package.
     */
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

    private fun showWarning(context: Context, packageName: String, info: BypassInfo) {
        createChannel(context)

        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                putExtra("open_tab", "more")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠ ${info.label} bypasses Sovereign Shield")
            .setContentText("DNS filter cannot block this app.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("DNS filter cannot block this app.\nTap to manage in MORE → App Block List.")
            )
            .setColor(0xFFFBBF24.toInt())
            .setColorized(true)
            .setContentIntent(openIntent)
            .setAutoCancel(false)
            .setOngoing(false)
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
            val ch = NotificationChannel(
                CHANNEL_ID,
                "AI App Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Warns when an AI app bypasses Sovereign Shield"
                enableVibration(false)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(ch)
        }
    }
}
