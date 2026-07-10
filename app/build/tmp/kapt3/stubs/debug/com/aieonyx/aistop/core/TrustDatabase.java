package com.aieonyx.aistop.core;

/**
 * Static trust database — bundled in APK.
 * Updated per app release only. No network fetch in v1.x.
 * Fields: trust_db_version, data_as_of shown in UI per app.
 *
 * Methodology: permissions 40%, retention 30%, transparency 20%, opt-out 10%.
 * All scores are ESTIMATES based on publicly reviewed policies.
 * Disclaimer shown in-app: "AI Stop estimate based on device permissions
 * and reviewed provider policies. Does not reflect inferred server behavior."
 *
 * Gemini Trust Score citations (v1.1 locked per Gemini audit recommendation):
 *  - Retention: Gemini Apps Privacy Hub — default 3-year retention
 *  - Human review: documented review process for default conversations
 *  - Opt-out: requires manual navigation through multiple settings screens
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u0001\u000bB\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\b\u001a\u00020\u00052\u0006\u0010\t\u001a\u00020\u0005J\u000e\u0010\n\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\u0005R\u001a\u0010\u0003\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00060\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0005X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lcom/aieonyx/aistop/core/TrustDatabase;", "", "()V", "DB", "", "", "Lcom/aieonyx/aistop/core/TrustDatabase$TrustEntry;", "DB_VERSION", "dataAsOf", "pkg", "entry", "TrustEntry", "app_debug"})
public final class TrustDatabase {
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String DB_VERSION = "1.0.0";
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Map<java.lang.String, com.aieonyx.aistop.core.TrustDatabase.TrustEntry> DB = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.aieonyx.aistop.core.TrustDatabase INSTANCE = null;
    
    private TrustDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.aieonyx.aistop.core.TrustDatabase.TrustEntry entry(@org.jetbrains.annotations.NotNull()
    java.lang.String pkg) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String dataAsOf(@org.jetbrains.annotations.NotNull()
    java.lang.String pkg) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0013\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001B5\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\u0007\u0012\u0006\u0010\t\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\nJ\t\u0010\u0013\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0014\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0015\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0016\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\u0017\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\u0018\u001a\u00020\u0007H\u00c6\u0003JE\u0010\u0019\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00072\b\b\u0002\u0010\t\u001a\u00020\u0007H\u00c6\u0001J\u0013\u0010\u001a\u001a\u00020\u001b2\b\u0010\u001c\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001d\u001a\u00020\u0003H\u00d6\u0001J\t\u0010\u001e\u001a\u00020\u0007H\u00d6\u0001R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\t\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\fR\u0011\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u000fR\u0011\u0010\b\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\fR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u000f\u00a8\u0006\u001f"}, d2 = {"Lcom/aieonyx/aistop/core/TrustDatabase$TrustEntry;", "", "retentionScore", "", "transparencyScore", "optOutScore", "dataAsOf", "", "source", "notes", "(IIILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "getDataAsOf", "()Ljava/lang/String;", "getNotes", "getOptOutScore", "()I", "getRetentionScore", "getSource", "getTransparencyScore", "component1", "component2", "component3", "component4", "component5", "component6", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"})
    public static final class TrustEntry {
        private final int retentionScore = 0;
        private final int transparencyScore = 0;
        private final int optOutScore = 0;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String dataAsOf = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String source = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String notes = null;
        
        public TrustEntry(int retentionScore, int transparencyScore, int optOutScore, @org.jetbrains.annotations.NotNull()
        java.lang.String dataAsOf, @org.jetbrains.annotations.NotNull()
        java.lang.String source, @org.jetbrains.annotations.NotNull()
        java.lang.String notes) {
            super();
        }
        
        public final int getRetentionScore() {
            return 0;
        }
        
        public final int getTransparencyScore() {
            return 0;
        }
        
        public final int getOptOutScore() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getDataAsOf() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getSource() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getNotes() {
            return null;
        }
        
        public final int component1() {
            return 0;
        }
        
        public final int component2() {
            return 0;
        }
        
        public final int component3() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component4() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component5() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component6() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.aieonyx.aistop.core.TrustDatabase.TrustEntry copy(int retentionScore, int transparencyScore, int optOutScore, @org.jetbrains.annotations.NotNull()
        java.lang.String dataAsOf, @org.jetbrains.annotations.NotNull()
        java.lang.String source, @org.jetbrains.annotations.NotNull()
        java.lang.String notes) {
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
}