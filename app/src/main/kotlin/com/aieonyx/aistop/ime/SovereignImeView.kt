// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aieonyx.aistop.core.TrustDatabase
import com.aieonyx.aistop.db.EdisonExposureDatabase as ExposureDatabase
import com.aieonyx.aistop.db.EventType
import com.aieonyx.aistop.jni.AiStopCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Sovereign IME view — keyboard + paste intercept sheet.
 * Sovereign color tokens match the v1.1 UI spec exactly.
 */
class SovereignImeView(
    context: Context,
    private val mediator: PasteMediator
) : FrameLayout(context) {

    // Sovereign color tokens
    private val SovereignVoid  = ComposeColor(0xFF080A0D)
    private val SovereignBlue  = ComposeColor(0xFF4F80D4)
    private val ThreatRed      = ComposeColor(0xFFE45F65)
    private val CautionAmber   = ComposeColor(0xFFD7A84B)
    private val SignalWhite    = ComposeColor(0xFFEDF3FA)
    private val SubText        = ComposeColor(0x8CEDF3FA)
    private val Surface1       = ComposeColor(0x0AEDF3FA)
    private val Surface2       = ComposeColor(0x12EDF3FA)

    private var targetPackage = ""
    private var trustScore = -1
    private var isPasswordField = false

    private val composeView = ComposeView(context)

    init {
        addView(composeView, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        renderKeyboard()
    }

    fun updateContext(pkg: String, score: Int, isPassword: Boolean) {
        targetPackage = pkg
        trustScore = score
        isPasswordField = isPassword
        renderKeyboard()
    }

    fun onInterceptStateChanged(state: PasteMediator.InterceptResult) {
        composeView.setContent {
            when (state) {
                is PasteMediator.InterceptResult.PiiDetected ->
                    IMEWithSheet(state.detectionJson, state.originalText)
                else -> KeyboardOnly()
            }
        }
    }

    private fun renderKeyboard() {
        composeView.setContent { KeyboardOnly() }
    }

    // ── Keyboard only (normal state) ─────────────────────────────────────────

    @Composable
    private fun KeyboardOnly() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ComposeColor(0xFF080A0D))
        ) {
            GuardBar()
            KeyboardRows()
        }
    }

    // ── IME with intercept sheet ──────────────────────────────────────────────

    @Composable
    private fun IMEWithSheet(detectionJson: String, originalText: String) {
        val piiClasses = remember(detectionJson) { parsePiiClasses(detectionJson) }
        val redacted   = remember(originalText, detectionJson) {
            applyRedaction(originalText, detectionJson)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ComposeColor(0xFF0F1318))
        ) {
            // ── App + Trust Score row ──
            AppTrustRow()

            // ── PII chips ──
            if (piiClasses.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    piiClasses.take(4).forEach { cls ->
                        PiiChip(cls, ThreatRed)
                    }
                    if (piiClasses.size > 4) {
                        PiiChip("+${piiClasses.size - 4}", SubText)
                    }
                }
            }

            // ── Detection label ──
            Text(
                if (piiClasses.isEmpty()) "No obvious PII found"
                else "Sensitive data found · ${piiClasses.size} items",
                color    = if (piiClasses.isEmpty()) SubText else SignalWhite,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )

            // ── BLOCK · REDACT · ALLOW buttons ──
            // v1.1: BLOCK free · REDACT paid · ALLOW free · ALLOW never green
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // BLOCK — red, free, 28%
                Button(
                    onClick = {
                        mediator.blockPaste()
                        logEvent(EventType.PASTE_BLOCKED, originalText, piiClasses)
                    },
                    modifier = Modifier.weight(0.28f),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = ThreatRed.copy(alpha = 0.15f),
                        contentColor   = ThreatRed
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BLOCK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Don't send", fontSize = 8.sp, color = ThreatRed.copy(alpha = 0.7f))
                    }
                }

                // REDACT — blue, paid, 44% (dominant)
                Button(
                    onClick = {
                        mediator.redactAndPaste(redacted) { text ->
                            // Commit redacted text via IME
                            val ic = (context as? SovereignIME)
                                ?.getCurrentInputConnection()
                            ic?.commitText(text, 1) ?: false
                        }
                        logEvent(EventType.PASTE_REDACTED, originalText, piiClasses)
                    },
                    modifier = Modifier.weight(0.44f),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = SovereignBlue,
                        contentColor   = SignalWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("REDACT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Clean & send", fontSize = 8.sp,
                            color = SignalWhite.copy(alpha = 0.7f))
                    }
                }

                // ALLOW — neutral, free, 28% — NEVER green
                Button(
                    onClick = {
                        mediator.allowPaste(originalText) { text ->
                            val ic = (context as? SovereignIME)
                                ?.getCurrentInputConnection()
                            ic?.commitText(text, 1) ?: false
                        }
                        logEvent(EventType.PASTE_ALLOWED, originalText, piiClasses)
                    },
                    modifier = Modifier.weight(0.28f),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Surface2,
                        contentColor   = SubText
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ALLOW", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Send as is", fontSize = 8.sp, color = SubText)
                    }
                }
            }

            // ── Guard bar + keyboard ──
            GuardBar(intercepting = true)
            KeyboardRows()
        }
    }

    // ── App + Trust Score row ─────────────────────────────────────────────────

    @Composable
    private fun AppTrustRow() {
        val appLabel = TrustDatabase.DB_VERSION.let {
            // Get display label for known packages
            when (targetPackage) {
                "com.openai.chatgpt"            -> "ChatGPT"
                "com.google.android.apps.bard"  -> "Gemini"
                "com.microsoft.copilot"         -> "Copilot"
                "com.anthropic.claude"          -> "Claude"
                "com.grammarly.android"         -> "Grammarly"
                "com.notion.id"                 -> "Notion"
                "com.perplexity.app",
                "ai.perplexity.app"             -> "Perplexity"
                else -> if (targetPackage.isNotEmpty()) targetPackage else "AI app"
            }
        }

        val bandColor = when {
            trustScore < 0  -> SubText
            trustScore < 40 -> ThreatRed
            trustScore < 70 -> CautionAmber
            else            -> ComposeColor(0xFF3EB69F)
        }
        val bandLabel = when {
            trustScore < 0  -> ""
            trustScore < 40 -> "HIGH RISK"
            trustScore < 70 -> "CAUTION"
            else            -> "STRONG"
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Protecting input to", color = SubText, fontSize = 9.sp)
                Text(
                    appLabel,
                    color      = SignalWhite,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (trustScore >= 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$trustScore",
                        color      = bandColor,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        bandLabel,
                        color      = bandColor,
                        fontSize   = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    // ── Guard bar ─────────────────────────────────────────────────────────────

    @Composable
    private fun GuardBar(intercepting: Boolean = false) {
        val label = when {
            isPasswordField -> "Private field · no logging"
            intercepting    -> "Checking paste locally"
            targetPackage in TrustDatabase.KNOWN_AI_PACKAGES ->
                "Protecting input to ${appLabel(targetPackage)}"
            else -> "AI Stop IME is active"
        }
        val color = if (intercepting) CautionAmber else SovereignBlue

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                color      = color,
                fontSize   = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier   = Modifier.weight(1f)
            )
            // Globe key — always visible for keyboard switching
            TextButton(
                onClick = { /* handled by IME switch mechanism */ },
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("🌐", fontSize = 14.sp)
            }
        }
    }

    // ── Minimal QWERTY keyboard ───────────────────────────────────────────────

    @Composable
    private fun KeyboardRows() {
        val rows = listOf(
            listOf("q","w","e","r","t","y","u","i","o","p"),
            listOf("a","s","d","f","g","h","j","k","l"),
            listOf("⇧","z","x","c","v","b","n","m","⌫"),
            listOf("?123",",","     ",".",  "↵")
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ComposeColor(0xFF080A0D))
                .padding(vertical = 4.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    row.forEach { key ->
                        val weight = when (key) {
                            "     " -> 3f  // spacebar
                            "⇧", "⌫", "?123", "↵" -> 1.5f
                            else -> 1f
                        }
                        val bgColor = when (key) {
                            "↵"  -> SovereignBlue
                            else -> Surface1
                        }
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .height(36.dp)
                                .background(bgColor, RoundedCornerShape(5.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                key.trim(),
                                color    = if (key == "↵") SignalWhite else SignalWhite.copy(0.85f),
                                fontSize = if (key.length > 1) 8.sp else 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Chip composable ───────────────────────────────────────────────────────

    @Composable
    private fun PiiChip(label: String, color: ComposeColor) {
        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(5.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(
                label,
                color      = color,
                fontSize   = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun appLabel(pkg: String) = when (pkg) {
        "com.openai.chatgpt"           -> "ChatGPT"
        "com.google.android.apps.bard" -> "Gemini"
        "com.microsoft.copilot"        -> "Copilot"
        "com.anthropic.claude"         -> "Claude"
        "com.grammarly.android"        -> "Grammarly"
        "com.notion.id"                -> "Notion"
        else                           -> pkg
    }

    private fun parsePiiClasses(json: String): List<String> {
        return try {
            val obj = JSONObject(json)
            if (!obj.has("PiiFound")) return emptyList()
            val matches = obj.getJSONObject("PiiFound").getJSONArray("matches")
            (0 until matches.length())
                .map { matches.getJSONObject(it).getString("class") }
                .distinct()
        } catch (e: Exception) { emptyList() }
    }

    private fun applyRedaction(text: String, json: String): String {
        return try {
            val obj = JSONObject(json)
            if (!obj.has("PiiFound")) return text
            val matches = obj.getJSONObject("PiiFound").getJSONArray("matches")
            var result = text
            val counters = mutableMapOf<String, Int>()
            val sorted = (0 until matches.length())
                .map { matches.getJSONObject(it) }
                .sortedByDescending { it.getInt("start") }
            for (m in sorted) {
                val cls   = m.getString("class")
                val start = m.getInt("start")
                val end   = m.getInt("end")
                val count = (counters[cls] ?: 0) + 1
                counters[cls] = count
                if (start >= 0 && end <= result.length && start < end) {
                    result = result.substring(0, start) +
                             "[${cls}_$count]" +
                             result.substring(end)
                }
            }
            result
        } catch (e: Exception) { text }
    }

    private fun logEvent(type: String, text: String, classes: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            ExposureDatabase.getInstance(context).exposureDao().insert(
                com.aieonyx.aistop.db.ExposureEvent(
                    ts          = System.currentTimeMillis(),
                    packageName = targetPackage,
                    appLabel    = appLabel(targetPackage),
                    eventType   = type,
                    preview20   = text.take(20),
                    trustScore  = trustScore,
                    piiClasses  = classes.joinToString(",")
                )
            )
        }
    }
}
