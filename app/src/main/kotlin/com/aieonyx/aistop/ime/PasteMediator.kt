// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ime

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import com.aieonyx.aistop.jni.AiStopCore
import com.aieonyx.aistop.ui.SovereignMode
import com.aieonyx.aistop.ui.loadSovereignMode
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * PasteMediator — intercept decision engine.
 *
 * SovereignMode wiring:
 *   AutoPilot → auto-block all PII, no UI, silent log
 *   Default   → block high-confidence only (API keys, creds, SSN, PEM)
 *               low-confidence PII passes through with silent log
 *   Manual    → always surface overlay, user decides every time
 */
class PasteMediator(private val context: Context) {

    sealed class InterceptResult {
        data class PiiDetected(val detectionJson: String, val originalText: String) : InterceptResult()
        data class SilentBlock(val originalText: String, val reason: String) : InterceptResult()
        object NoPii       : InterceptResult()
        object Unavailable : InterceptResult()
    }

    val interceptState = MutableStateFlow<InterceptResult>(InterceptResult.NoPii)

    // High-confidence PII classes — blocked in Default mode without prompt
    private val highConfidenceClasses = setOf(
        "ApiKey", "AwsKey", "GitHubToken", "JWT", "PemKey",
        "Password", "Ssn", "CreditCard", "CryptoWallet", "PassportNumber"
    )

    fun intercept(
        text:          String,
        targetPackage: String,
        commit:        (String) -> Boolean
    ): Boolean {
        if (text.isBlank()) return commit(text)

        val resultJson = try {
            AiStopCore.piiDetect(text)
        } catch (e: Exception) {
            return commit(text)
        }

        return when (parseResultType(resultJson)) {
            "NoPiiFound"  -> { interceptState.value = InterceptResult.NoPii; commit(text) }
            "Unavailable" -> { interceptState.value = InterceptResult.Unavailable; commit(text) }
            "PiiFound"    -> handlePiiFound(resultJson, text, commit)
            else          -> commit(text)
        }
    }

    private fun handlePiiFound(
        resultJson: String,
        text:       String,
        commit:     (String) -> Boolean
    ): Boolean {
        val mode = loadSovereignMode(context)
        return when (mode) {

            SovereignMode.AUTOPILOT -> {
                // Silent block — no UI, clipboard cleared, logged by AccessibilityService
                interceptState.value = InterceptResult.SilentBlock(text, "AutoPilot")
                autoClipboard()
                false
            }

            SovereignMode.DEFAULT -> {
                val detectedClasses = parsePiiClasses(resultJson).toSet()
                val isHighConfidence = detectedClasses.any { it in highConfidenceClasses }
                if (isHighConfidence) {
                    // Block and surface overlay for high-confidence only
                    interceptState.value = InterceptResult.PiiDetected(resultJson, text)
                    false
                } else {
                    // Low-confidence: pass through, log silently
                    interceptState.value = InterceptResult.NoPii
                    commit(text)
                }
            }

            SovereignMode.MANUAL -> {
                // Always surface overlay — user decides
                interceptState.value = InterceptResult.PiiDetected(resultJson, text)
                false
            }
        }
    }

    fun allowPaste(originalText: String, commit: (String) -> Boolean): Boolean {
        interceptState.value = InterceptResult.NoPii
        return commit(originalText)
    }

    fun blockPaste() {
        interceptState.value = InterceptResult.NoPii
        autoClipboard()
    }

    fun redactAndPaste(redactedText: String, commit: (String) -> Boolean): Boolean {
        interceptState.value = InterceptResult.NoPii
        val result = commit(redactedText)
        if (result) autoClipboard()
        return result
    }

    // Clears clipboard silently — suppresses Android 13+ toast
    private fun autoClipboard() {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("", "")
            val extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
            clip.description.extras = extras
            cm.setPrimaryClip(clip)
        } catch (e: Exception) { }
    }

    private fun parseResultType(json: String): String {
        return try {
            val obj = JSONObject(json)
            when {
                obj.has("NoPiiFound")  -> "NoPiiFound"
                obj.has("PiiFound")    -> "PiiFound"
                obj.has("Unavailable") -> "Unavailable"
                else                   -> "NoPiiFound"
            }
        } catch (e: Exception) { "NoPiiFound" }
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
}
