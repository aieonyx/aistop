// Copyright (c) 2026 Edison Lepiten / AIEONYX
// AI Stop — Sovereign Shield: AI Creep Detector
// Package: com.aieonyx.aistop

package com.aieonyx.aistop.vpn

import android.content.Context
import android.util.Log

/**
 * AI Creep Detector — inspects TLS SNI fields for connections to AI endpoints.
 *
 * Unlike DnsFilter (which blocks at DNS resolution time), this catches
 * connections where DNS was cached or resolved externally. It operates on
 * the SNI hostname extracted from TLS ClientHello packets on port 443.
 *
 * Detection categories:
 *   • AI_CRAWLER   — known AI data collection endpoints (block by default)
 *   • AI_MODEL_API — model inference APIs (flag + block if configured)
 *   • AI_TELEMETRY — AI-adjacent telemetry/beacon (block by default)
 *   • CLEAN        — not AI-related (allow)
 *
 * V-4 ships with blocking of AI_CRAWLER and AI_TELEMETRY.
 * AI_MODEL_API blocking is user-configurable (default: flag only).
 */
class AiCreepDetector(private val context: Context) {

    companion object {
        private const val TAG = "AiCreepDetector"
    }

    enum class Category { AI_CRAWLER, AI_MODEL_API, AI_TELEMETRY, CLEAN }

    data class Detection(
        val hostname: String,
        val category: Category,
        val srcIp: String,
        val dstIp: String,
        val timestampMs: Long = System.currentTimeMillis()
    )

    // Detection log for UI (network data flow map)
    private val detections = mutableListOf<Detection>()
    fun getDetections(): List<Detection> = detections.toList()

    // ── Pattern tables ────────────────────────────────────────────────────────

    private val crawlerPatterns = listOf(
        "openai.com", "oaistatic.com", "oaiusercontent.com",
        "anthropic.com", "claude.ai",
        "generativelanguage.googleapis.com", "bard.google.com", "gemini.google.com",
        "ai.google.dev", "aistudio.google.com",
        "perplexity.ai", "cohere.ai", "mistral.ai",
        "x.ai", "grok.x.ai",
        "huggingface.co", "datasets-server.huggingface.co",
        "commoncrawl.org"
    )

    private val modelApiPatterns = listOf(
        "api.openai.com",
        "api.anthropic.com",
        "api.cohere.ai",
        "api.mistral.ai",
        "api.perplexity.ai",
        "openai.azure.com",                 // Azure OpenAI
        "cognitiveservices.azure.com",      // Azure AI broadly
        "api.together.xyz",                 // Together.ai
        "api.replicate.com",               // Replicate
        "api.groq.com"                     // Groq
    )

    private val telemetryPatterns = listOf(
        "telemetry.openai.com",
        "events.anthropic.com",
        "ml-telemetry.amazonaws.com",
        "beacon.openai.com",
        "sentry.io",                        // used by many AI apps for crash/usage data
        "o.clarity.ms",                    // Microsoft Clarity (behavioral tracking)
        "mixpanel.com",                    // common in AI apps
        "segment.io", "api.segment.io",
        "amplitude.com", "api.amplitude.com"
    )

    // ── Classify ──────────────────────────────────────────────────────────────

    private fun classify(hostname: String): Category {
        val h = hostname.trimEnd('.')
        // Check most specific (telemetry) first
        if (telemetryPatterns.any { h == it || h.endsWith(".$it") }) return Category.AI_TELEMETRY
        if (modelApiPatterns.any  { h == it || h.endsWith(".$it") }) return Category.AI_MODEL_API
        if (crawlerPatterns.any   { h == it || h.endsWith(".$it") }) return Category.AI_CRAWLER
        return Category.CLEAN
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Called by PacketInterceptor after SNI extraction from TLS ClientHello.
     * Returns DROP if the connection should be blocked, ALLOW otherwise.
     */
    fun inspect(sni: String, srcIp: String, dstIp: String): Verdict {
        val category = classify(sni)

        if (category != Category.CLEAN) {
            Log.i(TAG, "AI Creep detected: $sni  category=$category  $srcIp → $dstIp")
            detections.add(Detection(sni, category, srcIp, dstIp))
            // Cap detection log at 500 entries
            if (detections.size > 500) detections.removeAt(0)
        }

        return when (category) {
            Category.AI_CRAWLER, Category.AI_TELEMETRY -> Verdict.DROP
            Category.AI_MODEL_API -> Verdict.ALLOW  // configurable — flag only for now
            Category.CLEAN -> Verdict.ALLOW
        }
    }
}
