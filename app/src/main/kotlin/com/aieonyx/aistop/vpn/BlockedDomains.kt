// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.vpn

/**
 * BlockedDomains — sovereign blocklist for Sovereign Shield DNS filter.
 * Covers AI platforms, crawlers, telemetry, ad networks that feed AI training.
 * Suffix matches applied automatically (e.g. "openai.com" also blocks "api.openai.com").
 */
object BlockedDomains {

    val exact: Set<String> = setOf(

        // ── OpenAI / ChatGPT ──────────────────────────────────────────────────
        "openai.com", "oaistatic.com", "oaiusercontent.com",
        "chatgpt.com", "chat.openai.com",
        "api.openai.com", "platform.openai.com",
        "telemetry.openai.com", "beacon.openai.com",
        "sora.com",

        // ── Anthropic / Claude ────────────────────────────────────────────────
        "anthropic.com", "claude.ai",
        "api.anthropic.com", "console.anthropic.com",
        "events.anthropic.com",

        // ── Google AI ─────────────────────────────────────────────────────────
        "generativelanguage.googleapis.com",
        "bard.google.com", "gemini.google.com",
        "ai.google.dev", "aistudio.google.com",
        "makersuite.google.com", "labs.google.com",
        "deepmind.com", "deepmind.google",

        // ── Microsoft / Copilot / Bing AI ─────────────────────────────────────
        "copilot.microsoft.com", "copilot.bing.com",
        "sydney.bing.com", "edgeservices.bing.com",
        "cognitive.microsoft.com",
        "openai.azure.com",
        "ml.azure.com",

        // ── Meta AI ───────────────────────────────────────────────────────────
        "ai.meta.com", "llama.meta.com",
        "metaai.com",

        // ── Perplexity ────────────────────────────────────────────────────────
        "perplexity.ai", "www.perplexity.ai",
        "api.perplexity.ai",

        // ── Cohere ───────────────────────────────────────────────────────────
        "cohere.ai", "cohere.com", "api.cohere.ai",

        // ── Mistral ───────────────────────────────────────────────────────────
        "mistral.ai", "api.mistral.ai", "console.mistral.ai",

        // ── xAI / Grok ───────────────────────────────────────────────────────
        "x.ai", "grok.x.ai", "api.x.ai",

        // ── Stability AI ──────────────────────────────────────────────────────
        "stability.ai", "api.stability.ai", "dreamstudio.ai",

        // ── Midjourney ────────────────────────────────────────────────────────
        "midjourney.com", "cdn.midjourney.com",

        // ── Runway ────────────────────────────────────────────────────────────
        "runwayml.com", "api.runwayml.com",

        // ── ElevenLabs ────────────────────────────────────────────────────────
        "elevenlabs.io", "api.elevenlabs.io",

        // ── HuggingFace ───────────────────────────────────────────────────────
        "huggingface.co", "api.huggingface.co",
        "datasets-server.huggingface.co",
        "huggingface.com",

        // ── Together AI ───────────────────────────────────────────────────────
        "together.ai", "api.together.xyz", "together.xyz",

        // ── Replicate ─────────────────────────────────────────────────────────
        "replicate.com", "api.replicate.com",

        // ── Groq ─────────────────────────────────────────────────────────────
        "groq.com", "api.groq.com",

        // ── AI21 Labs ─────────────────────────────────────────────────────────
        "ai21.com", "api.ai21.com",

        // ── Inflection AI ─────────────────────────────────────────────────────
        "inflection.ai", "pi.ai",

        // ── Character AI ──────────────────────────────────────────────────────
        "character.ai", "beta.character.ai",

        // ── Poe ───────────────────────────────────────────────────────────────
        "poe.com",

        // ── You.com ───────────────────────────────────────────────────────────
        "you.com",

        // ── Jasper ────────────────────────────────────────────────────────────
        "jasper.ai",

        // ── Writer ────────────────────────────────────────────────────────────
        "writer.com",

        // ── Scale AI (data labeling for AI training) ──────────────────────────
        "scale.com", "api.scale.com",

        // ── Common Crawl (web scraping for AI training) ───────────────────────
        "commoncrawl.org", "cc-index.commoncrawl.org",

        // ── LAION (dataset) ───────────────────────────────────────────────────
        "laion.ai",

        // ── OpenWebText / EleutherAI ──────────────────────────────────────────
        "pile.eleuther.ai", "eleuther.ai",

        // ── Cohere training data ──────────────────────────────────────────────
        "txt.cohere.ai",

        // ── AWS AI/ML services ────────────────────────────────────────────────
        "ml-telemetry.amazonaws.com",
        "sagemaker.amazonaws.com",
        "rekognition.amazonaws.com",
        "comprehend.amazonaws.com",
        "polly.amazonaws.com",
        "transcribe.amazonaws.com",
        "bedrock.amazonaws.com",

        // ── Google Analytics / Ads (feeds AI ad targeting) ────────────────────
        "google-analytics.com", "analytics.google.com",
        "googletagmanager.com", "googletagservices.com",
        "adservice.google.com", "doubleclick.net",
        "googlesyndication.com",

        // ── Facebook / Meta tracking ──────────────────────────────────────────
        "connect.facebook.net", "graph.facebook.com",
        "pixel.facebook.com", "an.facebook.com",

        // ── Behavioral telemetry / analytics ─────────────────────────────────
        "mixpanel.com", "api.mixpanel.com",
        "segment.io", "api.segment.io", "cdn.segment.com",
        "amplitude.com", "api.amplitude.com", "api2.amplitude.com",
        "heap.io", "heapanalytics.com",
        "fullstory.com", "rs.fullstory.com",
        "hotjar.com", "static.hotjar.com",
        "mouseflow.com",
        "logrocket.com",
        "sentry.io", "o1.sentry.io",
        "datadog-browser-agent.com",

        // ── Microsoft Clarity ─────────────────────────────────────────────────
        "o.clarity.ms", "clarity.ms",

        // ── Crash / error reporting used by AI apps ────────────────────────────
        "bugsnag.com", "notify.bugsnag.com",
        "rollbar.com", "api.rollbar.com",
        "crashlytics.com",

        // ── Apple AI ─────────────────────────────────────────────────────────
        "apple-intelligence.apple.com",

        // ── Samsung AI ────────────────────────────────────────────────────────
        "ai.samsung.com", "bixby.samsung.com",

        // ── Grammarly (AI writing, data collection) ───────────────────────────
        "grammarly.com", "api.grammarly.com",

        // ── Notion AI ─────────────────────────────────────────────────────────
        "notion.so",

        // ── Typeface AI ──────────────────────────────────────────────────────
        "typeface.ai",

        // ── Adobe Firefly ────────────────────────────────────────────────────────────
        "firefly.adobe.com",

        // ── DeepSeek ────────────────────────────────────────────────────────────────
        "deepseek.com", "chat.deepseek.com", "api.deepseek.com",

        // ── Kimi (Moonshot AI) ───────────────────────────────────────────────────
        "kimi.ai", "moonshot.cn", "api.moonshot.cn",

        // ── Qwen / Alibaba AI ────────────────────────────────────────────────────
        "qwen.aliyun.com", "tongyi.aliyun.com", "dashscope.aliyuncs.com",

        // ── Baidu / Ernie ────────────────────────────────────────────────────────
        "yiyan.baidu.com", "wenxin.baidu.com", "aistudio.baidu.com",
        "ernie.baidu.com", "qianfan.baidubce.com",

        // ── Zhipu AI / ChatGLM ───────────────────────────────────────────────────
        "zhipuai.cn", "open.bigmodel.cn", "chatglm.cn",

        // ── MiniMax ──────────────────────────────────────────────────────────────
        "minimax.chat", "api.minimax.chat",

        // ── 01.AI (Yi) ───────────────────────────────────────────────────────────
        "01.ai", "api.01.ai",

        // ── StepFun ──────────────────────────────────────────────────────────────
        "stepfun.com", "api.stepfun.com",

        // ── Baichuan AI ──────────────────────────────────────────────────────────
        "baichuan-ai.com", "api.baichuan-ai.com",

        // ── ByteDance AI / Doubao ─────────────────────────────────────────────
        "doubao.com", "volcengine.com", "ark.cn-beijing.volces.com",

        // ── Tencent AI / Hunyuan ─────────────────────────────────────────────
        "hunyuan.tencent.com", "cloud.tencent.com",

        // ── iFlytek / Spark ──────────────────────────────────────────────────
        "xinghuo.xfyun.cn", "spark-api.xf-yun.com",

        // ── Pika (AI video) ──────────────────────────────────────────────────
        "pika.art",

        // ── Suno (AI music) ──────────────────────────────────────────────────
        "suno.ai", "suno.com",

        // ── Udio (AI music) ──────────────────────────────────────────────────
        "udio.com",

        // ── Luma AI ──────────────────────────────────────────────────────────
        "lumalabs.ai", "api.lumalabs.ai",

        // ── Kling AI ─────────────────────────────────────────────────────────
        "klingai.com",

        // ── Hailuo AI ────────────────────────────────────────────────────────
        "hailuoai.com",
        "firefly.adobe.com"
    )

    val suffixes: List<String> = listOf(
        ".openai.com",
        ".anthropic.com",
        ".perplexity.ai",
        ".cohere.ai", ".cohere.com",
        ".deepseek.com",
        ".moonshot.cn",
        ".aliyuncs.com",
        ".baidubce.com",
        ".zhipuai.cn",
        ".volcengine.com",
        ".mistral.ai",
        ".huggingface.co",
        ".stability.ai",
        ".elevenlabs.io",
        ".runwayml.com",
        ".replicate.com",
        ".together.ai",
        ".groq.com",
        ".ai21.com",
        ".character.ai",
        ".deepmind.com",
        ".openai.azure.com",
        ".cognitive.microsoft.com",
        ".sagemaker.amazonaws.com",
        ".bedrock.amazonaws.com",
        ".grammarly.com"
    )

    /**
     * Category for display in ShieldScreen and UnblockSheet.
     */
    fun categoryOf(hostname: String): String {
        val h = hostname.trimEnd('.')
        return when {
            h.contains("telemetry") || h.contains("beacon") ||
            h.contains("analytics") || h.contains("segment") ||
            h.contains("amplitude") || h.contains("mixpanel") ||
            h.contains("hotjar") || h.contains("fullstory") ||
            h.contains("sentry") || h.contains("clarity") ||
            h.contains("bugsnag") || h.contains("rollbar") ||
            h.contains("crashlytics") || h.contains("logrocket") ||
            h.contains("heap") || h.contains("mouseflow") ||
            h.contains("doubleclick") || h.contains("pixel") ||
            h.contains("gtm") || h.contains("googletagmanager") -> "TELEMETRY"

            h.contains("api.") || h.endsWith(".openai.com") ||
            h == "api.anthropic.com" || h == "api.cohere.ai" ||
            h == "api.mistral.ai" || h == "api.perplexity.ai" ||
            h == "api.groq.com" || h == "api.together.xyz" ||
            h == "api.replicate.com" || h == "api.elevenlabs.io" ||
            h.contains("sagemaker") || h.contains("bedrock") ||
            h.contains("cognitive.microsoft") -> "API"

            else -> "CRAWLER"
        }
    }
}
