// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.identity

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.aieonyx.aistop.jni.AiStopCore
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature

/**
 * SovereignIdentity — dual-key signing.
 * Android 13+ : Ed25519 (sovereign)
 * Android 11/12: ECDSA P-256 / SHA256withECDSA (legacy)
 */
object SovereignIdentity {

    private const val KEY_ALIAS_ED25519 = "aistop_sovereign_identity_v2_ed25519"
    private const val KEY_ALIAS_P256    = "aistop_sovereign_identity_v2_p256"
    private const val KEYSTORE          = "AndroidKeyStore"

    private val isEd25519Supported get() = Build.VERSION.SDK_INT >= 33

    private val activeAlias get() =
        if (isEd25519Supported) KEY_ALIAS_ED25519 else KEY_ALIAS_P256

    fun signingAlgorithm(): String =
        if (isEd25519Supported) "Ed25519" else "SHA256withECDSA"

    fun ensureKeyPair() {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        if (ks.containsAlias(activeAlias)) return

        if (isEd25519Supported) {
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS_ED25519,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("ed25519"))
                .setDigests(KeyProperties.DIGEST_NONE)
                .build()
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE)
                .apply { initialize(spec) }
                .generateKeyPair()
        } else {
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS_P256,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE)
                .apply { initialize(spec) }
                .generateKeyPair()
        }
    }

    fun publicKeyFingerprint(): String {
        return try {
            val ks   = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            val cert = ks.getCertificate(activeAlias) ?: return "no_key"
            AiStopCore.blake3Hash(
                Base64.encodeToString(cert.publicKey.encoded, Base64.NO_WRAP)
            ).take(16)
        } catch (e: Exception) { "no_key" }
    }

    fun signHash(hashHex: String): String {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val privateKey = ks.getKey(activeAlias, null) as? PrivateKey
            ?: error("Identity key not found — run ensureKeyPair() first")
        val sig = Signature.getInstance(signingAlgorithm()).apply {
            initSign(privateKey)
            update(hashHex.toByteArray(Charsets.UTF_8))
        }
        return sig.sign().joinToString("") { "%02x".format(it) }
    }

    fun signExport(payloadJson: String): String {
        val hashHex      = AiStopCore.blake3Hash(payloadJson)
        val signatureHex = signHash(hashHex)
        return AiStopCore.assembleSignatureBlock(payloadJson, hashHex, signatureHex)
    }
}
