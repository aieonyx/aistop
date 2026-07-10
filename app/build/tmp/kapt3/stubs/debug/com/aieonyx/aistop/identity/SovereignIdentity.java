package com.aieonyx.aistop.identity;

/**
 * M5 SovereignIdentity — Ed25519 keypair backed by Android Keystore (TEE/StrongBox).
 *
 * SPLIT SIGNING FLOW (v1.1 critical fix from Gemini audit):
 *  Step 1 — Rust:   blake3_hash(payload) → hash bytes
 *  Step 2 — JNI:   hash bytes cross boundary to Kotlin
 *  Step 3 — Kotlin: Keystore Ed25519 sign(hash) → signature bytes  ← THIS CLASS
 *  Step 4 — JNI:   signature bytes return to Rust
 *  Step 5 — Rust:   assemble SignatureBlock { payload, hash, signature }
 *
 * Private key never leaves the TEE. Rust handles all hashing and final assembly.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0006\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010\u0006\u001a\u00020\u0007J\u0006\u0010\b\u001a\u00020\u0004J\u000e\u0010\t\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u0004J\u000e\u0010\u000b\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u0004R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\r"}, d2 = {"Lcom/aieonyx/aistop/identity/SovereignIdentity;", "", "()V", "KEYSTORE", "", "KEY_ALIAS", "ensureKeyPair", "", "publicKeyFingerprint", "signExport", "payloadJson", "signHash", "hashHex", "app_debug"})
public final class SovereignIdentity {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_ALIAS = "aistop_sovereign_identity_v1";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEYSTORE = "AndroidKeyStore";
    @org.jetbrains.annotations.NotNull()
    public static final com.aieonyx.aistop.identity.SovereignIdentity INSTANCE = null;
    
    private SovereignIdentity() {
        super();
    }
    
    /**
     * Generate Ed25519 keypair on first run.
     * Keystore-backed — private key never exported from TEE.
     * No-op if key already exists.
     */
    public final void ensureKeyPair() {
    }
    
    /**
     * Return the public key fingerprint (hex) for the export schema.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String publicKeyFingerprint() {
        return null;
    }
    
    /**
     * Sign a BLAKE3 hash hex string with the Keystore Ed25519 private key.
     * Explicit cast to PrivateKey — getKey() returns Key interface.
     * This is Step 3 of the split signing flow.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String signHash(@org.jetbrains.annotations.NotNull()
    java.lang.String hashHex) {
        return null;
    }
    
    /**
     * Full export signing pipeline — orchestrates the split signing flow.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String signExport(@org.jetbrains.annotations.NotNull()
    java.lang.String payloadJson) {
        return null;
    }
}