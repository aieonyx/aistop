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

    const val DB_VERSION      = "1.0.0"
    const val DATASET_DATE    = "2026-07"
    const val DATASET_VERSION = "aistop-trust-db-v1.0.0"
    const val DATASET_NOTE    = "Scores derived from public privacy policies. Not legal advice."

    val KNOWN_AI_PACKAGES = setOf(
        // Tier 1 — Major AI assistants
        "com.openai.chatgpt",
        "com.google.android.apps.bard",
        "com.microsoft.copilot",
        "com.anthropic.claude",
        "com.grammarly.android",
        "com.notion.id",
        "com.perplexity.app",
        "ai.perplexity.app",
        // Tier 2 — AI productivity and creative tools
        "com.jasper.android",
        "com.writesonic.app",
        "io.talkpal.ai",
        "com.pixverse.app",
        "com.character.ai",
        "ai.character.app",
        "com.inflection.pi",
        "com.poe.android",
        "com.otter.ai",
        "com.fireflies.ai",
        "com.eleutheriai.lm",
        "com.mistral.android",
        "com.cohere.android",
        "com.you.app",
        "com.ideogram.ai",
        "com.midjourney.android",
        "com.runway.app",
        "com.heygen.android",
        "com.klingai.android",
        "ai.pixverse.app"
    )

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
    data class ScoreItem(val label: String, val detail: String, val score: Int)

    fun brandColor(pkg: String): Long = when (pkg) {
        "com.openai.chatgpt"            -> 0xFF10A37FL
        "com.google.android.apps.bard"  -> 0xFF1A1A2EL
        "com.microsoft.copilot"         -> 0xFF1A1A2EL
        "com.anthropic.claude"          -> 0xFFCC785CL
        "com.grammarly.android"         -> 0xFF15C39AL
        else                            -> 0xFF1A1A2EL
    }

    fun getBreakdown(pkg: String): List<ScoreItem> = when (pkg) {
        "com.openai.chatgpt" -> listOf(
            ScoreItem("Data Retention", "Retains data up to 3 years with account", 15),
            ScoreItem("Transparency", "Opt-out requires multiple steps", 35),
            ScoreItem("Opt-out Controls", "Training opt-out available but not default", 20),
            ScoreItem("Third-party Sharing", "Data shared with Microsoft and partners", 10)
        )
        "com.google.android.apps.bard" -> listOf(
            ScoreItem("Data Retention", "Gemini activity stored 18 months by default", 25),
            ScoreItem("Transparency", "Privacy policy is detailed and accessible", 65),
            ScoreItem("Opt-out Controls", "Auto-delete available; activity can be paused", 55),
            ScoreItem("Third-party Sharing", "Data used within Google ecosystem", 40)
        )
        "com.microsoft.copilot" -> listOf(
            ScoreItem("Data Retention", "Retention period not clearly disclosed", 10),
            ScoreItem("Transparency", "Policy references multiple Microsoft products", 30),
            ScoreItem("Opt-out Controls", "Limited opt-out; tied to Microsoft account", 15),
            ScoreItem("Third-party Sharing", "Data may be shared with OpenAI and partners", 20)
        )
        "com.anthropic.claude" -> listOf(
            ScoreItem("Data Retention", "Retention period not explicitly disclosed", 20),
            ScoreItem("Transparency", "Policy readable but incomplete on retention", 45),
            ScoreItem("Opt-out Controls", "No clear training opt-out for free tier", 30),
            ScoreItem("Third-party Sharing", "Limited third-party sharing disclosed", 50)
        )
        "com.grammarly.android" -> listOf(
            ScoreItem("Data Retention", "Retains text snippets for service improvement", 35),
            ScoreItem("Transparency", "Clear policy with specific data categories", 65),
            ScoreItem("Opt-out Controls", "Premium users have more control", 50),
            ScoreItem("Third-party Sharing", "No sale of personal data stated", 60)
        )
        else -> listOf(
            ScoreItem("Data Retention", "Retention policy not assessed", 50),
            ScoreItem("Transparency", "Privacy policy not assessed", 50),
            ScoreItem("Opt-out Controls", "Opt-out options not assessed", 50),
            ScoreItem("Third-party Sharing", "Sharing practices not assessed", 50)
        )
    }

    fun getImplications(pkg: String): List<String> = when (pkg) {
        "com.openai.chatgpt" -> listOf(
            "Text you paste may be used to train future AI models",
            "Data retained for up to 3 years with an account",
            "Opt-out from training requires Settings > Data Controls",
            "Microsoft has access to your conversation data"
        )
        "com.google.android.apps.bard" -> listOf(
            "Gemini activity stored for 18 months by default",
            "Human reviewers may read conversations to improve quality",
            "Auto-delete can be set to 3 or 18 months",
            "Data integrated with your broader Google account"
        )
        "com.microsoft.copilot" -> listOf(
            "Retention period for conversations is not clearly disclosed",
            "Data may flow between Microsoft and OpenAI infrastructure",
            "Opt-out options are limited compared to competitors",
            "Enterprise and consumer data handled under different policies"
        )
        "com.anthropic.claude" -> listOf(
            "Free tier conversations may be used for model improvement",
            "No clear opt-out from training for non-enterprise users",
            "Retention period not explicitly disclosed in consumer policy",
            "Pro and Enterprise tiers offer stronger data protections"
        )
        "com.grammarly.android" -> listOf(
            "Text you type is processed on Grammarly servers",
            "Snippets retained for service improvement",
            "Broad accessibility permissions required for full functionality",
            "No sale of personal data per current privacy policy"
        )
        else -> listOf(
            "Privacy policy has not been fully assessed",
            "Exercise caution when sharing sensitive information",
            "Review the app privacy policy directly for full details"
        )
    }

    fun appLabel(pkg: String): String = when (pkg) {
        "com.openai.chatgpt"            -> "ChatGPT"
        "com.google.android.apps.bard"  -> "Gemini"
        "com.microsoft.copilot"         -> "Copilot"
        "com.anthropic.claude"          -> "Claude"
        "com.grammarly.android"         -> "Grammarly"
        "com.notion.id"                 -> "Notion"
        "com.perplexity.app",
        "ai.perplexity.app"             -> "Perplexity"
        "io.talkpal.ai"                 -> "Talkpal"
        "com.character.ai",
        "ai.character.app"              -> "Character.AI"
        "com.inflection.pi"             -> "Pi"
        "com.poe.android"               -> "Poe"
        else                            -> pkg.substringAfterLast(".")
    }

}
