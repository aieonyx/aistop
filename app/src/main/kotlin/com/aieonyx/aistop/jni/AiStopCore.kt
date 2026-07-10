// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.jni

/**
 * JNI bridge to aistop-core Rust library.
 * Thin wrapper only — no business logic here.
 * All logic lives in the Rust core (platform-free).
 */
object AiStopCore {

    init {
        System.loadLibrary("aistop_core")
    }

    // ── M6 Trust Scorer ──────────────────────────────────────────────────
    /** Compute trust scores for a JSON array of AppRiskProfile. Returns JSON array of TrustResult. */
    external fun trustComputeBatch(profilesJson: String): String

    // ── M2/M3 PII Detection ──────────────────────────────────────────────
    /** Detect PII in text. Returns DetectionResult JSON. Never throws — Unavailable on null input. */
    external fun piiDetect(text: String): String

    // ── M5 SovereignIdentity — split signing ─────────────────────────────
    // Step 1: Rust computes BLAKE3 hash of payload
    external fun blake3Hash(payload: String): String

    // Step 4: Rust assembles final SignatureBlock after Kotlin signs
    external fun assembleSignatureBlock(
        payloadJson: String,
        hashHex: String,
        signatureHex: String
    ): String

    // ── Export ───────────────────────────────────────────────────────────
    /** Build export payload and hash. Returns { payload, hash } JSON. */
    external fun prepareExport(
        eventsJson: String,
        devicePubkey: String,
        exportedAt: String
    ): String

    /** Assemble final signed export after Keystore signing. */
    external fun finaliseExport(
        payloadJson: String,
        hashHex: String,
        signatureHex: String
    ): String
}
