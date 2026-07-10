// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.core

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
 *   - Retention: Gemini Apps Privacy Hub — default 3-year retention
 *   - Human review: documented review process for default conversations
 *   - Opt-out: requires manual navigation through multiple settings screens
 */
object TrustDatabase {

    const val DB_VERSION = "1.0.0"

    data class TrustEntry(
        val retentionScore:    Int,  // 0-100
        val transparencyScore: Int,
        val optOutScore:       Int,
        val dataAsOf:          String,
        val source:            String,
        val notes:             String,
    )

    private val DB: Map<String, TrustEntry> = mapOf(

        "com.openai.chatgpt" to TrustEntry(
            retentionScore    = 30,
            transparencyScore = 40,
            optOutScore       = 25,
            dataAsOf          = "2026-07",
            source            = "https://openai.com/policies/privacy-policy",
            notes             = "Default retention up to 30 days chat history. " +
                                "Free tier trains on data by default. " +
                                "Opt-out buried in settings."
        ),

        "com.google.android.apps.bard" to TrustEntry(
            retentionScore    = 20,  // 3-year default per Gemini Apps Privacy Hub
            transparencyScore = 45,
            optOutScore       = 30,
            dataAsOf          = "2026-07",
            source            = "https://support.google.com/gemini/answer/13594961",
            notes             = "Default 3-year retention (Gemini Apps Privacy Hub). " +
                                "Default human review enabled. " +
                                "Opt-out requires manual navigation through multiple settings."
        ),

        "com.microsoft.copilot" to TrustEntry(
            retentionScore    = 35,
            transparencyScore = 45,
            optOutScore       = 35,
            dataAsOf          = "2026-07",
            source            = "https://privacy.microsoft.com/privacystatement",
            notes             = "Retention varies by account type. " +
                                "Enterprise accounts have stronger controls."
        ),

        "com.anthropic.claude" to TrustEntry(
            retentionScore    = 55,
            transparencyScore = 65,
            optOutScore       = 60,
            dataAsOf          = "2026-07",
            source            = "https://www.anthropic.com/privacy",
            notes             = "API data explicitly excluded from training. " +
                                "Consumer tier retains conversations."
        ),

        "com.grammarly.android" to TrustEntry(
            retentionScore    = 40,
            transparencyScore = 45,
            optOutScore       = 40,
            dataAsOf          = "2026-07",
            source            = "https://www.grammarly.com/privacy-policy",
            notes             = "Flagged in 2026 Incogni study as high data-collection risk. " +
                                "Broad accessibility access."
        ),

        "com.notion.id" to TrustEntry(
            retentionScore    = 55,
            transparencyScore = 55,
            optOutScore       = 50,
            dataAsOf          = "2026-07",
            source            = "https://www.notion.so/Privacy-Policy",
            notes             = "AI features opt-in. Data used to improve AI features by default."
        ),

        "com.perplexity.app" to TrustEntry(
            retentionScore    = 60,
            transparencyScore = 60,
            optOutScore       = 55,
            dataAsOf          = "2026-07",
            source            = "https://www.perplexity.ai/privacy",
            notes             = "More transparent retention policy than most. " +
                                "Search queries retained for service improvement."
        ),

        "ai.perplexity.app" to TrustEntry(
            retentionScore    = 60,
            transparencyScore = 60,
            optOutScore       = 55,
            dataAsOf          = "2026-07",
            source            = "https://www.perplexity.ai/privacy",
            notes             = "Same policy as com.perplexity.app."
        ),
    )

    fun entry(pkg: String): TrustEntry = DB[pkg] ?: TrustEntry(
        retentionScore    = 50,
        transparencyScore = 50,
        optOutScore       = 50,
        dataAsOf          = "2026-07",
        source            = "Unknown",
        notes             = "No reviewed policy available for this package."
    )

    fun dataAsOf(pkg: String): String = entry(pkg).dataAsOf
}
