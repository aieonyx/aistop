// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.core

import android.content.pm.PackageManager
import com.aieonyx.aistop.jni.AiStopCore
import org.json.JSONArray
import org.json.JSONObject

/**
 * M1 PermissionAuditor — Kotlin side.
 * Uses explicit <queries> manifest entries (primary path).
 * QUERY_ALL_PACKAGES intentionally not used (Play Store hard block per Gemini audit).
 */
object PermissionScanner {

    /** Known AI app packages — mirrors the <queries> manifest entries. */
    val KNOWN_AI_PACKAGES = listOf(
        "com.openai.chatgpt",
        "com.google.android.apps.bard",
        "com.microsoft.copilot",
        "com.anthropic.claude",
        "com.grammarly.android",
        "com.notion.id",
        "com.perplexity.app",
        "ai.perplexity.app"
    )

    data class AuditedApp(
        val packageName:  String,
        val label:        String,
        val permissions:  List<String>,
        val trustScore:   Int,
        val trustBand:    String,   // "Red" | "Amber" | "Green"
        val trustLabel:   String,   // "Low Trust" | "Caution" | "Trusted"
        val dataAsOf:     String,
    )

    /**
     * Scan all known AI packages, return audit results.
     * Only inspects packages declared in <queries> — no broad scan.
     */
    fun scanInstalledAiApps(pm: PackageManager): List<AuditedApp> {
        return KNOWN_AI_PACKAGES.mapNotNull { pkg ->
            try {
                val info = pm.getPackageInfo(
                    pkg,
                    PackageManager.GET_PERMISSIONS
                )
                val label = pm.getApplicationLabel(info.applicationInfo).toString()
                val perms = info.requestedPermissions?.toList() ?: emptyList()

                // Build risk profile and send to Rust scorer
                val profile = buildRiskProfile(pkg, perms)
                val resultJson = AiStopCore.trustComputeBatch(
                    JSONArray().put(JSONObject(profile)).toString()
                )
                val result = JSONArray(resultJson).getJSONObject(0)
                val score  = result.getInt("score")
                val band   = result.getString("band")

                AuditedApp(
                    packageName  = pkg,
                    label        = label,
                    permissions  = perms,
                    trustScore   = score,
                    trustBand    = band,
                    trustLabel   = bandLabel(band),
                    dataAsOf     = TrustDatabase.dataAsOf(pkg),
                )
            } catch (e: PackageManager.NameNotFoundException) {
                null // app not installed — skip silently
            }
        }
    }

    private fun bandLabel(band: String) = when (band) {
        "Red"   -> "Low Trust"
        "Amber" -> "Caution"
        "Green" -> "Trusted"
        else    -> "Unknown"
    }

    /**
     * Build AppRiskProfile JSON for a package.
     * Uses TrustDatabase for policy-based scores (retention, transparency, opt-out).
     * Uses permission list for the permissions score.
     */
    private fun buildRiskProfile(pkg: String, perms: List<String>): Map<String, Any> {
        val permScore = scorePermissions(perms)
        val dbEntry   = TrustDatabase.entry(pkg)
        return mapOf(
            "package"            to pkg,
            "permissions_score"  to permScore,
            "retention_score"    to dbEntry.retentionScore,
            "transparency_score" to dbEntry.transparencyScore,
            "opt_out_score"      to dbEntry.optOutScore,
        )
    }

    /** Score a permission list — more critical permissions = lower score. */
    private fun scorePermissions(perms: List<String>): Int {
        if (perms.isEmpty()) return 90
        var deduction = 0
        for (perm in perms) {
            deduction += when {
                perm.contains("RECORD_AUDIO")
                || perm.contains("READ_CONTACTS")
                || perm.contains("ACCESS_FINE_LOCATION")  -> 20
                perm.contains("READ_CLIPBOARD")
                || perm.contains("CAMERA")
                || perm.contains("READ_CALL_LOG")          -> 15
                perm.contains("READ_EXTERNAL_STORAGE")
                || perm.contains("INTERNET")               ->  5
                else                                       ->  1
            }
        }
        return (100 - deduction).coerceAtLeast(0)
    }
    /**
     * Detect unknown AI-like apps by suspicious permission combinations.
     * Not in known list + INTERNET + sensitive permission = flagged at 45.
     */
    fun scanUnknownAiApps(pm: android.content.pm.PackageManager): List<AuditedApp> {
        val known = TrustDatabase.KNOWN_AI_PACKAGES
        val result = mutableListOf<AuditedApp>()
        val flags = android.content.pm.PackageManager.GET_PERMISSIONS
        val packages = try { pm.getInstalledPackages(flags) }
                       catch (e: Exception) { return emptyList() }
        val skip = setOf("com.android.", "android.", "com.google.android.", "com.samsung.")
        // Only flag apps whose package name or label suggests AI/assistant functionality
        val aiKeywords = setOf(
            "ai", "gpt", "llm", "chat", "assistant", "copilot", "gemini",
            "claude", "perplexity", "notion", "grammarly", "write", "genius",
            "smart", "intelligence", "neural", "bot", "predict", "ml"
        )
        for (pkg in packages) {
            val name = pkg.packageName.lowercase()
            if (pkg.packageName in known) continue
            if (skip.any { name.startsWith(it) }) continue
            // Must contain an AI keyword in package name
            val hasAiKeyword = aiKeywords.any { name.contains(it) }
            if (!hasAiKeyword) continue
            val perms = pkg.requestedPermissions?.toSet() ?: continue
            val hasNet = "android.permission.INTERNET" in perms
            val risky  = "android.permission.RECORD_AUDIO" in perms ||
                         "android.permission.READ_CONTACTS" in perms ||
                         "android.permission.ACCESS_FINE_LOCATION" in perms ||
                         perms.any { it.contains("CLIPBOARD") }
            if (hasNet && risky) {
                val label = try {
                    pm.getApplicationLabel(pkg.applicationInfo).toString()
                } catch (e: Exception) { pkg.packageName }
                result.add(AuditedApp(
                    packageName = pkg.packageName,
                    label       = label,
                    permissions = perms.toList(),
                    trustScore  = 45,
                    trustBand   = "Amber",
                    trustLabel  = "Caution",
                    dataAsOf    = "unassessed"
                ))
            }
        }
        return result.take(5)
    }

}
