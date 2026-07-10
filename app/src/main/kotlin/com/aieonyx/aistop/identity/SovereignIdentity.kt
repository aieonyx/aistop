// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.identity

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.aieonyx.aistop.jni.AiStopCore
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import android.util.Base64

/**
 * M5 SovereignIdentity — Ed25519 keypair backed by Android Keystore (TEE/StrongBox).
 *
 * SPLIT SIGNING FLOW (v1.1 critical fix from Gemini audit):
 *   Step 1 — Rust:   blake3_hash(payload) → hash bytes
 *   Step 2 — JNI:   hash bytes cross boundary to Kotlin
 *   Step 3 — Kotlin: Keystore Ed25519 sign(hash) → signature bytes  ← THIS CLASS
 *   Step 4 — JNI:   signature bytes return to Rust
 *   Step 5 — Rust:   assemble SignatureBlock { payload, hash, signature }
 *
 * Private key never leaves the TEE. Rust handles all hashing and final assembly.
 */
object SovereignIdentity {

    private const val KEY_ALIAS = "aistop_sovereign_identity_v1"
    private const val KEYSTORE  = "AndroidKeyStore"

    /**
     * Generate Ed25519 keypair on first run.
     * Keystore-backed — private key never exported from TEE.
     * No-op if key already exists.
     */
    fun ensureKeyPair() {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        if (ks.containsAlias(KEY_ALIAS)) return

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("ed25519"))
            .setDigests(KeyProperties.DIGEST_NONE)
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE)
            .apply { initialize(spec) }
            .generateKeyPair()
    }

    /**
     * Return the public key fingerprint (hex) for the export schema.
     */
    fun publicKeyFingerprint(): String {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val cert = ks.getCertificate(KEY_ALIAS) ?: return "no_key"
        val encoded = cert.publicKey.encoded
        return AiStopCore.blake3Hash(Base64.encodeToString(encoded, Base64.NO_WRAP))
            .take(16)
    }

    /**
     * Sign a BLAKE3 hash hex string with the Keystore Ed25519 private key.
     * Explicit cast to PrivateKey — getKey() returns Key interface.
     * This is Step 3 of the split signing flow.
     */
    fun signHash(hashHex: String): String {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        // Explicit cast to PrivateKey — getKey() returns Key interface
        val privateKey = ks.getKey(KEY_ALIAS, null) as? PrivateKey
            ?: error("AI Stop identity key not found — run ensureKeyPair() first")

        val sig = Signature.getInstance("Ed25519").apply {
            initSign(privateKey)
            update(hashHex.toByteArray(Charsets.UTF_8))
        }
        val bytes = sig.sign()
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Full export signing pipeline — orchestrates the split signing flow.
     */
    fun signExport(payloadJson: String): String {
        val hashHex      = AiStopCore.blake3Hash(payloadJson)
        val signatureHex = signHash(hashHex)
        return AiStopCore.assembleSignatureBlock(payloadJson, hashHex, signatureHex)
    }
}
