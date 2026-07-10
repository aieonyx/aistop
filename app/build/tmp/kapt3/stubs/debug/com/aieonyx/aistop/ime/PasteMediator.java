package com.aieonyx.aistop.ime;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\n\u0018\u00002\u00020\u0001:\u0001\u001aB\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\"\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0012\u0010\u000e\u001a\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\u000b0\u000fJ\b\u0010\u0010\u001a\u00020\u0011H\u0002J\u0006\u0010\u0012\u001a\u00020\u0011J*\u0010\u0013\u001a\u00020\u000b2\u0006\u0010\u0014\u001a\u00020\r2\u0006\u0010\u0015\u001a\u00020\r2\u0012\u0010\u000e\u001a\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\u000b0\u000fJ\u0010\u0010\u0016\u001a\u00020\r2\u0006\u0010\u0017\u001a\u00020\rH\u0002J\"\u0010\u0018\u001a\u00020\u000b2\u0006\u0010\u0019\u001a\u00020\r2\u0012\u0010\u000e\u001a\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\u000b0\u000fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\t\u00a8\u0006\u001b"}, d2 = {"Lcom/aieonyx/aistop/ime/PasteMediator;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "interceptState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/aieonyx/aistop/ime/PasteMediator$InterceptResult;", "getInterceptState", "()Lkotlinx/coroutines/flow/MutableStateFlow;", "allowPaste", "", "originalText", "", "commit", "Lkotlin/Function1;", "autoClipboard", "", "blockPaste", "intercept", "text", "targetPackage", "parseResultType", "json", "redactAndPaste", "redactedText", "InterceptResult", "app_debug"})
public final class PasteMediator {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.aieonyx.aistop.ime.PasteMediator.InterceptResult> interceptState = null;
    
    public PasteMediator(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.MutableStateFlow<com.aieonyx.aistop.ime.PasteMediator.InterceptResult> getInterceptState() {
        return null;
    }
    
    public final boolean intercept(@org.jetbrains.annotations.NotNull()
    java.lang.String text, @org.jetbrains.annotations.NotNull()
    java.lang.String targetPackage, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, java.lang.Boolean> commit) {
        return false;
    }
    
    public final boolean allowPaste(@org.jetbrains.annotations.NotNull()
    java.lang.String originalText, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, java.lang.Boolean> commit) {
        return false;
    }
    
    public final void blockPaste() {
    }
    
    public final boolean redactAndPaste(@org.jetbrains.annotations.NotNull()
    java.lang.String redactedText, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, java.lang.Boolean> commit) {
        return false;
    }
    
    private final void autoClipboard() {
    }
    
    private final java.lang.String parseResultType(java.lang.String json) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0003\u0003\u0004\u0005B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0003\u0006\u0007\b\u00a8\u0006\t"}, d2 = {"Lcom/aieonyx/aistop/ime/PasteMediator$InterceptResult;", "", "()V", "NoPii", "PiiDetected", "Unavailable", "Lcom/aieonyx/aistop/ime/PasteMediator$InterceptResult$NoPii;", "Lcom/aieonyx/aistop/ime/PasteMediator$InterceptResult$PiiDetected;", "Lcom/aieonyx/aistop/ime/PasteMediator$InterceptResult$Unavailable;", "app_debug"})
    public static abstract class InterceptResult {
        
        private InterceptResult() {
            super();
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/aieonyx/aistop/ime/PasteMediator$InterceptResult$NoPii;", "Lcom/aieonyx/aistop/ime/PasteMediator$InterceptResult;", "()V", "app_debug"})
        public static final class NoPii extends com.aieonyx.aistop.ime.PasteMediator.InterceptResult {
            @org.jetbrains.annotations.NotNull()
            public static final com.aieonyx.aistop.ime.PasteMediator.InterceptResult.NoPii INSTANCE = null;
            
            private NoPii() {
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\t\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0005J\t\u0010\t\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\n\u001a\u00020\u0003H\u00c6\u0003J\u001d\u0010\u000b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\f\u001a\u00020\r2\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u00d6\u0003J\t\u0010\u0010\u001a\u00020\u0011H\u00d6\u0001J\t\u0010\u0012\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u0007\u00a8\u0006\u0013"}, d2 = {"Lcom/aieonyx/aistop/ime/PasteMediator$InterceptResult$PiiDetected;", "Lcom/aieonyx/aistop/ime/PasteMediator$InterceptResult;", "detectionJson", "", "originalText", "(Ljava/lang/String;Ljava/lang/String;)V", "getDetectionJson", "()Ljava/lang/String;", "getOriginalText", "component1", "component2", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
        public static final class PiiDetected extends com.aieonyx.aistop.ime.PasteMediator.InterceptResult {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String detectionJson = null;
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String originalText = null;
            
            public PiiDetected(@org.jetbrains.annotations.NotNull()
            java.lang.String detectionJson, @org.jetbrains.annotations.NotNull()
            java.lang.String originalText) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getDetectionJson() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getOriginalText() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component2() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.aieonyx.aistop.ime.PasteMediator.InterceptResult.PiiDetected copy(@org.jetbrains.annotations.NotNull()
            java.lang.String detectionJson, @org.jetbrains.annotations.NotNull()
            java.lang.String originalText) {
                return null;
            }
            
            @java.lang.Override()
            public boolean equals(@org.jetbrains.annotations.Nullable()
            java.lang.Object other) {
                return false;
            }
            
            @java.lang.Override()
            public int hashCode() {
                return 0;
            }
            
            @java.lang.Override()
            @org.jetbrains.annotations.NotNull()
            public java.lang.String toString() {
                return null;
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/aieonyx/aistop/ime/PasteMediator$InterceptResult$Unavailable;", "Lcom/aieonyx/aistop/ime/PasteMediator$InterceptResult;", "()V", "app_debug"})
        public static final class Unavailable extends com.aieonyx.aistop.ime.PasteMediator.InterceptResult {
            @org.jetbrains.annotations.NotNull()
            public static final com.aieonyx.aistop.ime.PasteMediator.InterceptResult.Unavailable INSTANCE = null;
            
            private Unavailable() {
            }
        }
    }
}