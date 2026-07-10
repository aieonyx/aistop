// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aieonyx.aistop.db.ExposureDatabase
import com.aieonyx.aistop.jni.AiStopCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * M3 ScrubShare — Android share-sheet target.
 * Zero permissions required. Works from any app via standard share menu.
 *
 * Free tier: 3 scrubs/day — detect + show diff + manual copy.
 * Paid tier: unlimited (v1.1 — checked via ScrubQuota).
 *
 * v1.1 upgrade nudge: appears AFTER copy completes, never before.
 */
class ScrubShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract shared text
        val sharedText = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            else               -> ""
        }

        if (sharedText.isBlank()) {
            Toast.makeText(this, "No text to scrub", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            ScrubShareScreen(
                inputText  = sharedText,
                onDismiss  = { finish() }
            )
        }
    }
}

// ── Sovereign color tokens ────────────────────────────────────────────────────
private val SovereignVoid  = Color(0xFF080A0D)
private val SovereignBlue  = Color(0xFF4F80D4)
private val ThreatRed      = Color(0xFFE45F65)
private val CautionAmber   = Color(0xFFD7A84B)
private val VerifiedTeal   = Color(0xFF3EB69F)
private val SignalWhite    = Color(0xFFEDF3FA)
private val Surface1       = Color(0x0AEDF3FA)
private val Surface2       = Color(0x12EDF3FA)
private val SubText        = Color(0x8CEDF3FA)

@Composable
fun ScrubShareScreen(inputText: String, onDismiss: () -> Unit) {
    val context       = LocalContext.current
    val clipboard     = LocalClipboardManager.current
    var copyDone      by remember { mutableStateOf(false) }
    var showNudge     by remember { mutableStateOf(false) }

    // Run PII detection
    val detectionResult = remember(inputText) {
        runCatching { AiStopCore.piiDetect(inputText) }.getOrElse { "{}" }
    }

    val piiClasses = remember(detectionResult) {
        parsePiiClasses(detectionResult)
    }

    val redactedText = remember(inputText, detectionResult) {
        if (piiClasses.isEmpty()) inputText
        else applySimpleRedaction(inputText, detectionResult)
    }

    // Check free tier quota
    val scrubsToday = remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        val midnight = midnightTs()
        scrubsToday.value = ExposureDatabase
            .getInstance(context)
            .exposureDao()
            .countScrubsToday(midnight)
    }

    val isQuotaExceeded = scrubsToday.value >= 3

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SovereignVoid.copy(alpha = 0.96f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F1318), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Scrub & Share",
                    color = SignalWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onDismiss) {
                    Text("✕", color = SubText)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Quota indicator ──
            if (!isQuotaExceeded) {
                Text(
                    "${3 - scrubsToday.value} of 3 free scrubs remaining today",
                    color = SubText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(10.dp))
            } else {
                Surface(
                    color = Surface1,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Daily limit reached — unlock unlimited scrubs for \$2.99",
                        color = CautionAmber,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── PII chips ──
            if (piiClasses.isNotEmpty()) {
                Text("Detected", color = SubText, fontSize = 10.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    piiClasses.take(4).forEach { cls ->
                        PiiChip(cls)
                    }
                    if (piiClasses.size > 4) {
                        PiiChip("+${piiClasses.size - 4}")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Original panel ──
            TextPanel(
                label = "Original",
                text  = buildHighlightedText(inputText, detectionResult)
            )

            Spacer(Modifier.height(8.dp))

            // ── Cleaned panel ──
            TextPanel(
                label      = "Cleaned — safe to share",
                text       = AnnotatedString(redactedText),
                borderColor = SovereignBlue.copy(alpha = 0.3f)
            )

            if (piiClasses.isEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "No obvious PII found — review before sharing",
                    color = SubText,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Copy button ──
            Button(
                onClick = {
                    clipboard.setText(AnnotatedString(redactedText))
                    copyDone = true
                    // Log to ExposureDB
                    CoroutineScope(Dispatchers.IO).launch {
                        ExposureDatabase.logScrubShare(
                            context    = context,
                            preview    = inputText.take(20),
                            piiClasses = piiClasses
                        )
                    }
                    // v1.1: show upgrade nudge AFTER copy, never before
                    if (isQuotaExceeded.not() && scrubsToday.value >= 2) {
                        showNudge = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = SovereignBlue),
                shape    = RoundedCornerShape(10.dp),
                enabled  = !isQuotaExceeded
            ) {
                Text(
                    if (copyDone) "✓ Copied" else "📋 Copy Clean Text",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // Secondary: close
            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = SubText)
            ) {
                Text("Close", fontSize = 13.sp)
            }

            // ── Upgrade nudge — appears after copy, never before ──
            if (showNudge) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color  = Surface1,
                    shape  = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Unlock unlimited scrubs",
                                color      = SovereignBlue,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "One-time payment · \$2.99",
                                color    = SubText,
                                fontSize = 10.sp
                            )
                        }
                        Text("›", color = SubText, fontSize = 18.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Built on sovereign computing principles · AIEONYX",
                color    = SubText,
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
fun PiiChip(label: String) {
    Surface(
        color = ThreatRed.copy(alpha = 0.12f),
        shape = RoundedCornerShape(5.dp)
    ) {
        Text(
            label,
            color      = ThreatRed,
            fontSize   = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TextPanel(
    label:       String,
    text:        AnnotatedString,
    borderColor: Color = ThreatRed.copy(alpha = 0.2f)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(label, color = SubText, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(text, color = SignalWhite, fontSize = 11.sp, lineHeight = 18.sp)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun parsePiiClasses(detectionJson: String): List<String> {
    return try {
        val obj = JSONObject(detectionJson)
        if (!obj.has("PiiFound")) return emptyList()
        val report  = obj.getJSONObject("PiiFound")
        val matches = report.getJSONArray("matches")
        (0 until matches.length())
            .map { matches.getJSONObject(it).getString("class") }
            .distinct()
    } catch (e: Exception) {
        emptyList()
    }
}

private fun applySimpleRedaction(text: String, detectionJson: String): String {
    return try {
        val obj = JSONObject(detectionJson)
        if (!obj.has("PiiFound")) return text
        val report  = obj.getJSONObject("PiiFound")
        val matches = report.getJSONArray("matches")
        var result  = text
        val counters = mutableMapOf<String, Int>()
        // Sort by start descending to avoid offset shifts
        val sortedMatches = (0 until matches.length())
            .map { matches.getJSONObject(it) }
            .sortedByDescending { it.getInt("start") }
        for (m in sortedMatches) {
            val cls   = m.getString("class")
            val start = m.getInt("start")
            val end   = m.getInt("end")
            val count = (counters[cls] ?: 0) + 1
            counters[cls] = count
            val token = "[${cls}_$count]"
            if (start >= 0 && end <= result.length && start < end) {
                result = result.substring(0, start) + token + result.substring(end)
            }
        }
        result
    } catch (e: Exception) {
        text
    }
}

private fun buildHighlightedText(text: String, detectionJson: String): AnnotatedString {
    return try {
        val obj = JSONObject(detectionJson)
        if (!obj.has("PiiFound")) return AnnotatedString(text)
        val report  = obj.getJSONObject("PiiFound")
        val matches = report.getJSONArray("matches")
        val ranges  = (0 until matches.length()).map {
            val m = matches.getJSONObject(it)
            m.getInt("start") to m.getInt("end")
        }.sortedBy { it.first }

        buildAnnotatedString {
            var cursor = 0
            for ((start, end) in ranges) {
                if (start > cursor) append(text.substring(cursor, start))
                withStyle(SpanStyle(color = ThreatRed,
                    background = ThreatRed.copy(alpha = 0.12f))) {
                    append(text.substring(start.coerceAtMost(text.length),
                                          end.coerceAtMost(text.length)))
                }
                cursor = end
            }
            if (cursor < text.length) append(text.substring(cursor))
        }
    } catch (e: Exception) {
        AnnotatedString(text)
    }
}

private fun midnightTs(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
