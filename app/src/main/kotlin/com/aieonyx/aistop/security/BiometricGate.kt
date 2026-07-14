// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * BiometricGate — sovereign settings protection.
 *
 * Wraps any sensitive action with biometric or PIN verification.
 * Uses Android BiometricPrompt API with hardware-backed authentication.
 *
 * Falls back gracefully:
 *   Fingerprint/Face → BIOMETRIC_STRONG
 *   No biometric → DEVICE_CREDENTIAL (PIN/Pattern/Password)
 *   No lock screen → action allowed (warn user to set up lock screen)
 *
 * Usage:
 *   BiometricGate.authenticate(activity, "Change Mode") {
 *       // executes only after successful auth
 *       changeSovereignMode(newMode)
 *   }
 */
object BiometricGate {

    sealed class AuthResult {
        object Success  : AuthResult()
        object Failure  : AuthResult()
        object Error    : AuthResult()
        object NoHardware : AuthResult()
    }

    fun isAvailable(context: Context): Boolean {
        val bm = BiometricManager.from(context)
        return bm.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
               BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity:    FragmentActivity,
        actionTitle: String,
        subtitle:    String = "Verify your identity to continue",
        onResult:    (AuthResult) -> Unit
    ) {
        val bm = BiometricManager.from(activity)
        val canAuth = bm.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            // No biometric or PIN set up — allow but warn
            onResult(AuthResult.NoHardware)
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(AuthResult.Success)
            }
            override fun onAuthenticationFailed() {
                onResult(AuthResult.Failure)
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(AuthResult.Error)
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val info   = BiometricPrompt.PromptInfo.Builder()
            .setTitle("AI Stop — $actionTitle")
            .setSubtitle(subtitle)
            .setDescription("Sovereign settings are protected. Verify your identity to proceed.")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(info)
    }
}

/**
 * Composable helper — gate any Composable action behind biometric.
 *
 * Usage in Compose:
 *   val gate = rememberBiometricGate()
 *   Button(onClick = { gate.request("Change Mode") { doChange() } }) { ... }
 *   gate.Content() // place once in the Composable tree
 */
class BiometricGateState(private val activity: FragmentActivity) {
    private var pendingAction: (() -> Unit)? = null
    private var pendingTitle:  String        = ""
    var showFailed  by mutableStateOf(false)
    var showNoHardware by mutableStateOf(false)

    fun request(title: String, action: () -> Unit) {
        pendingTitle  = title
        pendingAction = action
        showFailed    = false
        showNoHardware = false

        BiometricGate.authenticate(activity, title) { result ->
            when (result) {
                is BiometricGate.AuthResult.Success     -> {
                    pendingAction?.invoke()
                    pendingAction = null
                }
                is BiometricGate.AuthResult.NoHardware -> {
                    // No lock screen — allow action but show warning
                    showNoHardware = true
                    pendingAction?.invoke()
                    pendingAction = null
                }
                else -> {
                    showFailed    = true
                    pendingAction = null
                }
            }
        }
    }
}

@Composable
fun rememberBiometricGate(): BiometricGateState {
    val context  = LocalContext.current
    val activity = context as? FragmentActivity
        ?: error("BiometricGate requires a FragmentActivity context")
    return remember(activity) { BiometricGateState(activity) }
}
