// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ime

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import com.aieonyx.aistop.jni.AiStopCore
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow

class PasteMediator(private val context: Context) {

    sealed class InterceptResult {
        data class PiiDetected(val detectionJson: String, val originalText: String) : InterceptResult()
        object NoPii : InterceptResult()
        object Unavailable : InterceptResult()
    }

    val interceptState = MutableStateFlow<InterceptResult>(InterceptResult.NoPii)

    fun intercept(
        text: String,
        targetPackage: String,
        commit: (String) -> Boolean
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
            "PiiFound"    -> { interceptState.value = InterceptResult.PiiDetected(resultJson, text); false }
            else -> commit(text)
        }
    }

    fun allowPaste(originalText: String, commit: (String) -> Boolean): Boolean {
        interceptState.value = InterceptResult.NoPii
        return commit(originalText)
    }

    fun blockPaste() {
        interceptState.value = InterceptResult.NoPii
    }

    fun redactAndPaste(redactedText: String, commit: (String) -> Boolean): Boolean {
        interceptState.value = InterceptResult.NoPii
        val result = commit(redactedText)
        if (result) autoClipboard()
        return result
    }

    // v1.1: EXTRA_IS_SENSITIVE suppresses Android 13+ toast on clipboard clear
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
}
