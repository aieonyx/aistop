// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aieonyx.aistop.db.EventType
import com.aieonyx.aistop.db.ExposureDatabase
import com.aieonyx.aistop.db.ExposureEvent

private val Void  = Color(0xFF080A0D)
private val Blue  = Color(0xFF4F80D4)
private val Red   = Color(0xFFE45F65)
private val Amber = Color(0xFFD7A84B)
private val Teal  = Color(0xFF3EB69F)
private val White = Color(0xFFEDF3FA)
private val Sub   = Color(0x8CEDF3FA)
private val Surf1 = Color(0x0AEDF3FA)
private val Surf2 = Color(0x14EDF3FA)

data class PiiStat(val label: String, val count: Int, val color: Color)
data class AppStat(val label: String, val sent: Int, val blocked: Int)

/**
 * Sovereign Stats — visualizes what data left your phone to AI apps.
 * Built entirely from local ExposureLog — zero network calls.
 */
@Composable
fun SovereignStatsScreen() {
    val context = LocalContext.current
    var events  by remember { mutableStateOf<List<ExposureEvent>>(emptyList()) }

    LaunchedEffect(Unit) {
        ExposureDatabase.getInstance(context).exposureDao()
            .observeRecent(System.currentTimeMillis() - ExposureDatabase.RETENTION_MS)
            .collect { events = it }
    }

    // Compute stats
    val totalEvents  = events.size
    val blocked      = events.count { it.eventType == EventType.PASTE_BLOCKED }
    val redacted     = events.count { it.eventType == EventType.PASTE_REDACTED }
    val allowed      = events.count { it.eventType == EventType.PASTE_ALLOWED }
    val scrubbed     = events.count { it.eventType == EventType.SCRUB_SHARE }
    val dataBlocked  = blocked + redacted

    // PII class frequency
    val piiCounts = mutableMapOf<String, Int>()
    events.forEach { e ->
        e.piiClasses.split(",").forEach { cls ->
            val c = cls.trim()
            if (c.isNotEmpty()) piiCounts[c] = (piiCounts[c] ?: 0) + 1
        }
    }
    val piiStats = listOf(
        PiiStat("EMAIL",      piiCounts["EMAIL"] ?: 0,      Red),
        PiiStat("PHONE",      piiCounts["PHONE"] ?: 0,      Amber),
        PiiStat("NAME",       piiCounts["NAME"] ?: 0,       Blue),
        PiiStat("ADDRESS",    piiCounts["ADDRESS"] ?: 0,    Teal),
        PiiStat("DOB",        piiCounts["DOB"] ?: 0,        Color(0xFFB47FD4)),
        PiiStat("ID NUMBER",  piiCounts["ID_NUMBER"] ?: 0,  Color(0xFFD47F7F)),
    ).filter { it.count > 0 }

    // Per-app stats
    val appStats = events
        .groupBy { it.appLabel }
        .map { (label, evts) ->
            AppStat(
                label   = label,
                sent    = evts.count { it.eventType == EventType.PASTE_ALLOWED },
                blocked = evts.count {
                    it.eventType == EventType.PASTE_BLOCKED ||
                    it.eventType == EventType.PASTE_REDACTED
                }
            )
        }
        .sortedByDescending { it.blocked + it.sent }
        .take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Void)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "DATA FLOW",
                    color      = White,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    "What your AI apps see",
                    color    = Sub,
                    fontSize = 11.sp
                )
            }
            Surface(
                color = Teal.copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    "ON-DEVICE",
                    color      = Teal,
                    fontSize   = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (totalEvents == 0) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No data yet",
                        color      = Sub,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Enable AI Stop Keyboard and start using AI apps.",
                        color    = Sub.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Your data flow will appear here.",
                        color    = Sub.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        } else {

            // ── Protection summary donut ──
            SectionTitle("PROTECTION SUMMARY")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .background(Surf1, RoundedCornerShape(14.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Donut chart
                    val total = (blocked + redacted + allowed).toFloat().coerceAtLeast(1f)
                    Box(modifier = Modifier.size(100.dp)) {
                        Canvas(modifier = Modifier.size(100.dp)) {
                            val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            val inset = 7.dp.toPx()
                            val rect = Size(size.width - inset * 2, size.height - inset * 2)
                            var startAngle = -90f

                            // Blocked arc (red)
                            val blockedSweep = 360f * (blocked / total)
                            if (blockedSweep > 0f) {
                                drawArc(Red, startAngle, blockedSweep, false,
                                    topLeft = Offset(inset, inset), size = rect, style = stroke)
                                startAngle += blockedSweep
                            }
                            // Redacted arc (blue)
                            val redactedSweep = 360f * (redacted / total)
                            if (redactedSweep > 0f) {
                                drawArc(Blue, startAngle, redactedSweep, false,
                                    topLeft = Offset(inset, inset), size = rect, style = stroke)
                                startAngle += redactedSweep
                            }
                            // Allowed arc (amber)
                            val allowedSweep = 360f * (allowed / total)
                            if (allowedSweep > 0f) {
                                drawArc(Amber, startAngle, allowedSweep, false,
                                    topLeft = Offset(inset, inset), size = rect, style = stroke)
                            }
                            // Background ring
                            if (total == 1f) {
                                drawArc(Surf2, -90f, 360f, false,
                                    topLeft = Offset(inset, inset), size = rect, style = stroke)
                            }
                        }
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val pct = if (total > 0) ((dataBlocked / total) * 100).toInt() else 0
                            Text(
                                "$pct%",
                                color      = Teal,
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "protected",
                                color    = Sub,
                                fontSize = 8.sp
                            )
                        }
                    }

                    // Legend
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LegendItem(Red,   "Blocked",  blocked)
                        LegendItem(Blue,  "Redacted", redacted)
                        LegendItem(Amber, "Allowed",  allowed)
                        LegendItem(Teal,  "Scrubbed", scrubbed)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── PII types detected ──
            if (piiStats.isNotEmpty()) {
                SectionTitle("PII TYPES INTERCEPTED")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .background(Surf1, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val maxCount = piiStats.maxOf { it.count }.toFloat()
                    piiStats.forEach { stat ->
                        PiiBar(stat, maxCount)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Per-app breakdown ──
            if (appStats.isNotEmpty()) {
                SectionTitle("DATA FLOW BY APP")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .background(Surf1, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    appStats.forEach { stat ->
                        AppFlowRow(stat)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Sovereignty statement ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .background(Blue.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text(
                    "ALL ANALYSIS IS ON-DEVICE",
                    color      = Blue,
                    fontSize   = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "AI Stop never sends your data anywhere. Every detection, every score, every log entry stays on your device. No telemetry. No cloud sync. No exceptions.",
                    color    = Sub,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        color      = Sub,
        fontSize   = 10.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.5.sp,
        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun LegendItem(color: Color, label: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Text("$label  ", color = Sub, fontSize = 11.sp)
        Text("$count", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun PiiBar(stat: PiiStat, maxCount: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stat.label, color = White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("${stat.count}x", color = stat.color, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Surf2, RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(stat.count / maxCount)
                    .height(6.dp)
                    .background(stat.color, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun AppFlowRow(stat: AppStat) {
    val total = (stat.sent + stat.blocked).toFloat().coerceAtLeast(1f)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stat.label, color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${stat.blocked} blocked", color = Red, fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace)
                Text("${stat.sent} sent", color = Amber, fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(Modifier.height(4.dp))
        // Stacked bar: blocked (red) + sent (amber)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Surf2, RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((stat.blocked + stat.sent) / total)
                    .height(6.dp)
                    .background(Amber, RoundedCornerShape(3.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(stat.blocked / total)
                    .height(6.dp)
                    .background(Red, RoundedCornerShape(3.dp))
            )
        }
    }
}
