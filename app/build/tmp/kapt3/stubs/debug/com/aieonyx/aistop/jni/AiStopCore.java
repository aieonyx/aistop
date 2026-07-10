package com.aieonyx.aistop.jni;

/**
 * JNI bridge to aistop-core Rust library.
 * Thin wrapper only — no business logic here.
 * All logic lives in the Rust core (platform-free).
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u000f\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J!\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\u0004H\u0086 J\u0011\u0010\b\u001a\u00020\u00042\u0006\u0010\t\u001a\u00020\u0004H\u0086 J!\u0010\n\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\u0004H\u0086 J\u0011\u0010\u000b\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u0004H\u0086 J!\u0010\r\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u00042\u0006\u0010\u0010\u001a\u00020\u0004H\u0086 J\u0011\u0010\u0011\u001a\u00020\u00042\u0006\u0010\u0012\u001a\u00020\u0004H\u0086 \u00a8\u0006\u0013"}, d2 = {"Lcom/aieonyx/aistop/jni/AiStopCore;", "", "()V", "assembleSignatureBlock", "", "payloadJson", "hashHex", "signatureHex", "blake3Hash", "payload", "finaliseExport", "piiDetect", "text", "prepareExport", "eventsJson", "devicePubkey", "exportedAt", "trustComputeBatch", "profilesJson", "app_debug"})
public final class AiStopCore {
    @org.jetbrains.annotations.NotNull()
    public static final com.aieonyx.aistop.jni.AiStopCore INSTANCE = null;
    
    private AiStopCore() {
        super();
    }
    
    /**
     * Compute trust scores for a JSON array of AppRiskProfile. Returns JSON array of TrustResult.
     */
    @org.jetbrains.annotations.NotNull()
    public final native java.lang.String trustComputeBatch(@org.jetbrains.annotations.NotNull()
    java.lang.String profilesJson) {
        return null;
    }
    
    /**
     * Detect PII in text. Returns DetectionResult JSON. Never throws — Unavailable on null input.
     */
    @org.jetbrains.annotations.NotNull()
    public final native java.lang.String piiDetect(@org.jetbrains.annotations.NotNull()
    java.lang.String text) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final native java.lang.String blake3Hash(@org.jetbrains.annotations.NotNull()
    java.lang.String payload) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final native java.lang.String assembleSignatureBlock(@org.jetbrains.annotations.NotNull()
    java.lang.String payloadJson, @org.jetbrains.annotations.NotNull()
    java.lang.String hashHex, @org.jetbrains.annotations.NotNull()
    java.lang.String signatureHex) {
        return null;
    }
    
    /**
     * Build export payload and hash. Returns { payload, hash } JSON.
     */
    @org.jetbrains.annotations.NotNull()
    public final native java.lang.String prepareExport(@org.jetbrains.annotations.NotNull()
    java.lang.String eventsJson, @org.jetbrains.annotations.NotNull()
    java.lang.String devicePubkey, @org.jetbrains.annotations.NotNull()
    java.lang.String exportedAt) {
        return null;
    }
    
    /**
     * Assemble final signed export after Keystore signing.
     */
    @org.jetbrains.annotations.NotNull()
    public final native java.lang.String finaliseExport(@org.jetbrains.annotations.NotNull()
    java.lang.String payloadJson, @org.jetbrains.annotations.NotNull()
    java.lang.String hashHex, @org.jetbrains.annotations.NotNull()
    java.lang.String signatureHex) {
        return null;
    }
}