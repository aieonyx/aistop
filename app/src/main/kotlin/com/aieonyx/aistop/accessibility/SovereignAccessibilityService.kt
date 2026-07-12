// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.aieonyx.aistop.core.TrustDatabase
import com.aieonyx.aistop.jni.AiStopCore
import com.aieonyx.aistop.db.ExposureDatabase
import com.aieonyx.aistop.db.ExposureEvent
import com.aieonyx.aistop.db.EventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Sovereign Accessibility Service.
 *
 * Monitors when AI apps come to foreground and watches for paste events.
 * When a paste is detected into a known AI app, intercepts and shows
 * the SovereignOverlay decision sheet.
 *
 * This eliminates the need to manually switch keyboards — the overlay
 * appears automatically when pasting into AI apps.
 *
 * Zero keystroke logging. Only monitors window focus changes and
 * paste events for known AI app packages.
 */
class SovereignAccessibilityService : AccessibilityService() {

    private var currentPackage = ""
    private var clipboardManager: ClipboardManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_FOCUSED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
            packageNames = TrustDatabase.KNOWN_AI_PACKAGES.toTypedArray()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in TrustDatabase.KNOWN_AI_PACKAGES) return

        currentPackage = pkg

        // Monitor for paste events via clipboard content change
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

        CoroutineScope(Dispatchers.IO).launch {
            val result = runCatching { AiStopCore.piiDetect(text) }.getOrElse { return@launch }
            val hasPii = try {
                JSONObject(result).has("PiiFound")
            } catch (e: Exception) { false }

            if (hasPii) {
                // Log the detection
                ExposureDatabase.getInstance(this@SovereignAccessibilityService)
                    .exposureDao().insert(
                        ExposureEvent(
                            ts          = System.currentTimeMillis(),
                            packageName = targetPackage,
                            appLabel    = TrustDatabase.appLabel(targetPackage),
                            eventType   = EventType.PASTE_BLOCKED,
                            preview20   = text.take(20),
                            trustScore  = TrustDatabase.entry(targetPackage).let {
                                ((it.retentionScore * 0.4 + it.transparencyScore * 0.3 +
                                  it.optOutScore * 0.2) * 0.1).toInt().coerceIn(0, 100)
                            },
                            piiClasses  = parsePiiClasses(result)
                        )
                    )

                // Launch overlay warning
                val intent = Intent(this@SovereignAccessibilityService,
                    com.aieonyx.aistop.ui.SovereignOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("text",       text)
                    putExtra("package",    targetPackage)
                    putExtra("detection",  result)
                }
                startActivity(intent)
            }
        }
    }

    private fun parsePiiClasses(json: String): String {
        return try {
            val obj = JSONObject(json)
            if (!obj.has("PiiFound")) return ""
            val matches = obj.getJSONObject("PiiFound").getJSONArray("matches")
            (0 until matches.length())
                .map { matches.getJSONObject(it).getString("class") }
                .distinct()
                .joinToString(",")
        } catch (e: Exception) { "" }
    }

    override fun onInterrupt() {}
}
