package com.aieonyx.aistop.core;

/**
 * M1 PermissionAuditor — Kotlin side.
 * Uses explicit <queries> manifest entries (primary path).
 * QUERY_ALL_PACKAGES intentionally not used (Play Store hard block per Gemini audit).
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010$\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u0001\u0014B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\b\u001a\u00020\u00052\u0006\u0010\t\u001a\u00020\u0005H\u0002J*\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00010\u000b2\u0006\u0010\f\u001a\u00020\u00052\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004H\u0002J\u0014\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000f0\u00042\u0006\u0010\u0010\u001a\u00020\u0011J\u0016\u0010\u0012\u001a\u00020\u00132\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004H\u0002R\u0017\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\u0015"}, d2 = {"Lcom/aieonyx/aistop/core/PermissionScanner;", "", "()V", "KNOWN_AI_PACKAGES", "", "", "getKNOWN_AI_PACKAGES", "()Ljava/util/List;", "bandLabel", "band", "buildRiskProfile", "", "pkg", "perms", "scanInstalledAiApps", "Lcom/aieonyx/aistop/core/PermissionScanner$AuditedApp;", "pm", "Landroid/content/pm/PackageManager;", "scorePermissions", "", "AuditedApp", "app_debug"})
public final class PermissionScanner {
    
    /**
     * Known AI app packages — mirrors the <queries> manifest entries.
     */
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<java.lang.String> KNOWN_AI_PACKAGES = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.aieonyx.aistop.core.PermissionScanner INSTANCE = null;
    
    private PermissionScanner() {
        super();
    }
    
    /**
     * Known AI app packages — mirrors the <queries> manifest entries.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getKNOWN_AI_PACKAGES() {
        return null;
    }
    
    /**
     * Scan all known AI packages, return audit results.
     * Only inspects packages declared in <queries> — no broad scan.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.aieonyx.aistop.core.PermissionScanner.AuditedApp> scanInstalledAiApps(@org.jetbrains.annotations.NotNull()
    android.content.pm.PackageManager pm) {
        return null;
    }
    
    private final java.lang.String bandLabel(java.lang.String band) {
        return null;
    }
    
    /**
     * Build AppRiskProfile JSON for a package.
     * Uses TrustDatabase for policy-based scores (retention, transparency, opt-out).
     * Uses permission list for the permissions score.
     */
    private final java.util.Map<java.lang.String, java.lang.Object> buildRiskProfile(java.lang.String pkg, java.util.List<java.lang.String> perms) {
        return null;
    }
    
    /**
     * Score a permission list — more critical permissions = lower score.
     */
    private final int scorePermissions(java.util.List<java.lang.String> perms) {
        return 0;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0010\b\n\u0002\b\u0017\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001BC\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00030\u0006\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\u0006\u0010\t\u001a\u00020\u0003\u0012\u0006\u0010\n\u001a\u00020\u0003\u0012\u0006\u0010\u000b\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\fJ\t\u0010\u0017\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0018\u001a\u00020\u0003H\u00c6\u0003J\u000f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00030\u0006H\u00c6\u0003J\t\u0010\u001a\u001a\u00020\bH\u00c6\u0003J\t\u0010\u001b\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001c\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001d\u001a\u00020\u0003H\u00c6\u0003JU\u0010\u001e\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\u000e\b\u0002\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00030\u00062\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\u00032\b\b\u0002\u0010\n\u001a\u00020\u00032\b\b\u0002\u0010\u000b\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\u001f\u001a\u00020 2\b\u0010!\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\"\u001a\u00020\bH\u00d6\u0001J\t\u0010#\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u000b\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u000eR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u000eR\u0017\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00030\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\t\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u000eR\u0011\u0010\n\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u000eR\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016\u00a8\u0006$"}, d2 = {"Lcom/aieonyx/aistop/core/PermissionScanner$AuditedApp;", "", "packageName", "", "label", "permissions", "", "trustScore", "", "trustBand", "trustLabel", "dataAsOf", "(Ljava/lang/String;Ljava/lang/String;Ljava/util/List;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "getDataAsOf", "()Ljava/lang/String;", "getLabel", "getPackageName", "getPermissions", "()Ljava/util/List;", "getTrustBand", "getTrustLabel", "getTrustScore", "()I", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"})
    public static final class AuditedApp {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String packageName = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String label = null;
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<java.lang.String> permissions = null;
        private final int trustScore = 0;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String trustBand = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String trustLabel = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String dataAsOf = null;
        
        public AuditedApp(@org.jetbrains.annotations.NotNull()
        java.lang.String packageName, @org.jetbrains.annotations.NotNull()
        java.lang.String label, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.String> permissions, int trustScore, @org.jetbrains.annotations.NotNull()
        java.lang.String trustBand, @org.jetbrains.annotations.NotNull()
        java.lang.String trustLabel, @org.jetbrains.annotations.NotNull()
        java.lang.String dataAsOf) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getPackageName() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getLabel() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.String> getPermissions() {
            return null;
        }
        
        public final int getTrustScore() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTrustBand() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTrustLabel() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getDataAsOf() {
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
        public final java.util.List<java.lang.String> component3() {
            return null;
        }
        
        public final int component4() {
            return 0;
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
        public final java.lang.String component7() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.aieonyx.aistop.core.PermissionScanner.AuditedApp copy(@org.jetbrains.annotations.NotNull()
        java.lang.String packageName, @org.jetbrains.annotations.NotNull()
        java.lang.String label, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.String> permissions, int trustScore, @org.jetbrains.annotations.NotNull()
        java.lang.String trustBand, @org.jetbrains.annotations.NotNull()
        java.lang.String trustLabel, @org.jetbrains.annotations.NotNull()
        java.lang.String dataAsOf) {
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