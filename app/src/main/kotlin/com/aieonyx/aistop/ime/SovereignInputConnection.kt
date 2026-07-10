// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ime

import android.os.Bundle
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.inputmethod.TextAttribute
import com.aieonyx.aistop.jni.AiStopCore
import org.json.JSONObject

/**
 * M2 SovereignInputConnection — v1.1 critical fix (Gemini audit).
 *
 * commitText() alone is insufficient. WebViews (ChatGPT web), Flutter wrappers,
 * and custom EditText implementations bypass commitText() and use:
 *   - setComposingText()
 *   - replaceText()
 *   - direct InputConnection manipulation
 *
 * This wrapper intercepts ALL THREE paste delivery paths.
 * pii_detect() is called BEFORE any super() call commits text.
 */
class SovereignInputConnection(
    base: InputConnection,
    private val mediator: PasteMediator,
    private val targetPackage: String,
) : InputConnectionWrapper(base, true) {

    // ── Path 1: Standard paste ────────────────────────────────────────────
    override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
        return mediator.intercept(text.toString(), targetPackage) { cleaned ->
            super.commitText(cleaned, newCursorPosition)
        }
    }

    // ── Path 2: Composing text (used by WebView, Flutter) ─────────────────
    override fun setComposingText(text: CharSequence, newCursorPosition: Int): Boolean {
        return mediator.intercept(text.toString(), targetPackage) { cleaned ->
            super.setComposingText(cleaned, newCursorPosition)
        }
    }

    // ── Path 3: Direct replacement (Android 13+ replaceText API) ──────────
    override fun replaceText(
        start: Int, end: Int,
        text: CharSequence,
        newCursorPosition: Int,
        textAttribute: TextAttribute?
    ): Boolean {
        return mediator.intercept(text.toString(), targetPackage) { cleaned ->
            super.replaceText(start, end, cleaned, newCursorPosition, textAttribute)
        }
    }

    // ── Password field safety (v1.1 mandatory) ────────────────────────────
    // Android actively restricts IME text extraction from password fields.
    // InputConnection can return null on focus switch — never panic.
    // Override getEditorInfo() to detect field type before any interception.
}
