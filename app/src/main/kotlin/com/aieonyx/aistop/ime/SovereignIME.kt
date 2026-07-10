// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aieonyx.aistop.core.TrustDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * M2 Sovereign IME — InputMethodService.
 *
 * v1.1 critical fix (Gemini audit):
 * Wraps full InputConnection via SovereignInputConnection,
 * intercepting ALL three paste delivery paths:
 *   1. commitText()     — standard paste
 *   2. setComposingText() — WebView / Flutter paste
 *   3. replaceText()    — Android 13+ direct replacement
 *
 * Password field behavior:
 *   - Detects TYPE_TEXT_VARIATION_PASSWORD
 *   - Disables all logging and suggestions
 *   - Shows "Private field · no logging" in guard bar
 *
 * Globe key: always visible for keyboard switching.
 * Zero keystroke logging. Zero network I/O.
 * IME process ships WITHOUT internet permission.
 */
class SovereignIME : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private lateinit var mediator: PasteMediator
    private var currentPackage: String = ""
    private var isPasswordField: Boolean = false
    private var sovereignConnection: SovereignInputConnection? = null

    // IME view (keyboard + optional bottom sheet)
    private lateinit var imeView: SovereignImeView

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        mediator = PasteMediator(this)
    }

    override fun onConfigureWindow(win: android.view.Window, isFullscreen: Boolean, isCandidatesOnly: Boolean) {
        super.onConfigureWindow(win, isFullscreen, isCandidatesOnly)
        // Set LifecycleOwner on window decorView before ComposeView attaches
        win.decorView.setViewTreeLifecycleOwner(this)
        win.decorView.setViewTreeSavedStateRegistryOwner(this)
    }

    override fun onCreateInputView(): View {
        // Must set LifecycleOwner on the window BEFORE ComposeView attaches
        window?.window?.decorView?.let {
            it.setViewTreeLifecycleOwner(this)
            it.setViewTreeSavedStateRegistryOwner(this)
        }

        imeView = SovereignImeView(this, mediator)
        imeView.setViewTreeLifecycleOwner(this)
        imeView.setViewTreeSavedStateRegistryOwner(this)

        // Observe paste intercept state and update IME view
        lifecycleScope.launch {
            mediator.interceptState.collectLatest { state ->
                imeView.onInterceptStateChanged(state)
            }
        }

        return imeView
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)

        // Detect target app
        currentPackage = currentInputEditorInfo?.packageName ?: ""

        // Detect password field — mandatory v1.1
        val variation = attribute.inputType and android.text.InputType.TYPE_MASK_VARIATION
        isPasswordField = variation == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                || variation == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || variation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD

        // Update IME view with context
        if (::imeView.isInitialized) {
            val trustScore = TrustDatabase.entry(currentPackage).let {
                // Quick score for display — full score computed in M1
                when {
                    currentPackage in TrustDatabase.KNOWN_AI_PACKAGES -> {
                        val entry = TrustDatabase.entry(currentPackage)
                        ((entry.retentionScore * 0.3 +
                          entry.transparencyScore * 0.2 +
                          entry.optOutScore * 0.1) +
                         40).toInt().coerceIn(0, 100)
                    }
                    else -> -1 // Unknown app
                }
            }
            imeView.updateContext(currentPackage, trustScore, isPasswordField)
        }
    }

    override fun getCurrentInputConnection(): InputConnection? {
        val base = super.getCurrentInputConnection() ?: return null
        return wrapConnection(base)
    }

    private fun wrapConnection(base: InputConnection?): InputConnection? {
        if (base == null) return null
        // Password fields: pass through unwrapped — never intercept
        if (isPasswordField) return base
        val wrapped = SovereignInputConnection(base, mediator, currentPackage)
        sovereignConnection = wrapped
        return wrapped
    }

    override fun onFinishInput() {
        super.onFinishInput()
        sovereignConnection = null
        currentPackage = ""
        isPasswordField = false
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    // ── Key handling ──────────────────────────────────────────────────────────

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Back key while paste pending = BLOCK
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val state = mediator.interceptState.value
            if (state is PasteMediator.InterceptResult.PiiDetected) {
                mediator.blockPaste()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
