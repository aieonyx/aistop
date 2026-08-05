// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aieonyx.aistop.db.EdisonExposureDatabase
import com.aieonyx.aistop.db.EventType
import com.aieonyx.aistop.db.ExposureEvent
import com.aieonyx.aistop.ui.theme.AiStopTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

/**
 * PULSE — live interception monitor.
 *
 * Shows what AI Stop is actually catching, in real time:
 *   • Stats: blocked / redacted / scrubbed counts
 *   • Detection timeline: events per hour bar chart
 *   • Recent interceptions: live feed with app + PII class
 *   • Top targeted apps: which AI apps are pulling the most data
 *
 * Powered entirely by the EdisonDB exposure log — no network access needed.
 */
@Composable
fun PulseScreen() {
    val context = LocalContext.current
    val colors  = AiStopTheme.colors
    val typo    = AiStopTheme.typography

    val purple = Color(0xFFA78BFA)
    val red    = Color(0xFFF87171)
    val amber  = Color(0xFFFBBF24)
    val green  = Color(0xFF34D399)

    var events        by remember { mutableStateOf<List<ExposureEvent>>(emptyList()) }
    var blockedCount  by remember { mutableStateOf(0) }
    var scrubCount    by remember { mutableStateOf(0) }
    var totalCount    by remember { mutableStateOf(0) }
    var loading       by remember { mutableStateOf(true) }

    // Poll the exposure log every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) {
                try {
                    val db  = EdisonExposureDatabase.getInstance(context)
                    val dao = db.exposureDao()
                    val since24h = System.currentTimeMillis() - 24L * 60 * 60 * 1000

                    blockedCount = dao.countBlockedToday(since24h)
                    scrubCount   = dao.countScrubsToday(since24h)
                    totalCount   = dao.totalCount()
                    events       = dao.getAllForExport()
                        .sortedByDescending { it.ts }
                        .take(50)
                } catch (e: Exception) {
                    // DB not ready yet
                }
            }
            loading = false
            delay(3000)
        }
    }

    // Build hourly histogram for last 12 hours
    val hourlyBuckets = remember(events) {
        val now = System.currentTimeMillis()
        val buckets = LongArray(12)
        val labels  = Array(12) { "" }
        val fmt = SimpleDateFormat("HH", Locale.getDefault())
        for (i in 0 until 12) {
            val bucketEnd   = now - (11 - i) * 60L * 60 * 1000
            val bucketStart = bucketEnd - 60L * 60 * 1000
            buckets[i] = events.count { it.ts in bucketStart..bucketEnd }.toLong()
            labels[i]  = fmt.format(Date(bucketEnd))
        }
        buckets.toList() to labels.toList()
    }

    // Top targeted apps
    val topApps = remember(events) {
        events.groupBy { it.appLabel }
            .map { (label, evts) -> label to evts.size }
            .sortedByDescending { it.second }
            .take(5)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // ── Header ──
        item {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("PULSE", style = typo.h1, color = colors.textPrimary)
                    Text("LIVE INTERCEPTION MONITOR",
                        style = typo.labelSmall, color = colors.accentSecondary)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(purple.copy(alpha = 0.12f))
                        .border(1.dp, purple.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("● LIVE", style = typo.labelSmall, color = purple)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Stats row ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PulseStat("BLOCKED",    blockedCount.toString(), red,    colors, typo, Modifier.weight(1f))
                PulseStat("SCRUBBED",   scrubCount.toString(),   amber,  colors, typo, Modifier.weight(1f))
                PulseStat("ALL TIME",   totalCount.toString(),   purple, colors, typo, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Detection timeline ──
        item { PulseHeader("DETECTION TIMELINE · LAST 12H", colors, typo, purple) }
        item {
            if (events.isEmpty()) {
                PulseEmpty(
                    if (loading) "Loading exposure log…"
                    else "No interceptions yet — protection is active and watching",
                    colors, typo
                )
            } else {
                DetectionChart(
                    buckets = hourlyBuckets.first,
                    labels  = hourlyBuckets.second,
                    accent  = purple,
                    colors  = colors,
                    typo    = typo
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Top targeted apps ──
        if (topApps.isNotEmpty()) {
            item { PulseHeader("MOST TARGETED APPS", colors, typo, purple) }
            items(topApps) { (label, count) ->
                val maxCount = topApps.firstOrNull()?.second ?: 1
                TopAppRow(label, count, maxCount, colors, typo, red)
                Spacer(Modifier.height(4.dp))
            }
            item { Spacer(Modifier.height(20.dp)) }
        }

        // ── Recent interceptions ──
        item { PulseHeader("RECENT INTERCEPTIONS", colors, typo, purple) }

        if (events.isEmpty()) {
            item {
                PulseEmpty(
                    if (loading) "Loading…"
                    else "Nothing intercepted yet. Copy a password or API key and paste it into an AI app to see AI Stop work.",
                    colors, typo
                )
            }
        } else {
            items(events.take(30)) { event ->
                EventRow(event, colors, typo, red, amber, green)
                Spacer(Modifier.height(4.dp))
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "ALL ANALYSIS RUNS ON-DEVICE · 30-DAY RETENTION · NOTHING LEAVES YOUR PHONE",
                style     = typo.caption,
                color     = colors.textSecondary.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Detection chart ───────────────────────────────────────────────────────────

@Composable
private fun DetectionChart(
    buckets: List<Long>,
    labels: List<String>,
    accent: Color,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography
) {
    val maxVal = buckets.maxOrNull()?.coerceAtLeast(1L) ?: 1L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .drawBehind {
                    val count = buckets.size
                    val slotW = size.width / count
                    val barW  = slotW * 0.6f
                    buckets.forEachIndexed { i, v ->
                        val h = (v.toFloat() / maxVal) * size.height
                        val x = i * slotW + (slotW - barW) / 2
                        drawRect(
                            color   = accent.copy(alpha = if (v > 0) 0.85f else 0.15f),
                            topLeft = Offset(x, size.height - h.coerceAtLeast(2f)),
                            size    = Size(barW, h.coerceAtLeast(2f))
                        )
                    }
                }
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(0, 3, 6, 9, 11).forEach { i ->
                Text(labels.getOrElse(i) { "" },
                    style = typo.labelSmall, color = colors.textSecondary, fontSize = 9.sp)
            }
        }
    }
}

// ── Top app row ───────────────────────────────────────────────────────────────

@Composable
private fun TopAppRow(
    label: String,
    count: Int,
    maxCount: Int,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography,
    accent: Color
) {
    val fraction = (count.toFloat() / maxCount).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = typo.caption, color = colors.textPrimary,
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$count events", style = typo.labelSmall, color = colors.textSecondary)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(3.dp)
                .clip(RoundedCornerShape(2.dp)).background(colors.outline)
        ) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(accent))
        }
    }
}

// ── Event row ─────────────────────────────────────────────────────────────────

@Composable
private fun EventRow(
    event: ExposureEvent,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography,
    red: Color, amber: Color, green: Color
) {
    val (icon, accent, badge) = when (event.eventType) {
        EventType.PASTE_BLOCKED  -> Triple("⛔", red,   "BLOCKED")
        EventType.PASTE_REDACTED -> Triple("✂",  amber, "REDACTED")
        EventType.PASTE_ALLOWED  -> Triple("✓",  green, "ALLOWED")
        EventType.SCRUB_SHARE    -> Triple("🧼", amber, "SCRUBBED")
        EventType.CLIP_AUTOCLEAR -> Triple("🧹", green, "CLEARED")
        else                     -> Triple("•",  colors.disabled, event.eventType)
    }

    val piiList = remember(event.piiClasses) {
        try {
            val arr = JSONArray(event.piiClasses)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) { emptyList() }
    }

    val timeStr = remember(event.ts) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.ts))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.06f))
            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 13.sp, modifier = Modifier.padding(end = 10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(event.appLabel, style = typo.caption, color = colors.textPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(6.dp))
                Text(timeStr, style = typo.labelSmall,
                    color = colors.textSecondary, fontSize = 9.sp)
            }
            if (piiList.isNotEmpty()) {
                Text(piiList.joinToString(" · "), style = typo.labelSmall,
                    color = accent, fontSize = 9.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(accent.copy(alpha = 0.15f))
                .border(0.5.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(badge, style = typo.labelSmall, color = accent, fontSize = 9.sp)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun PulseStat(
    label: String, value: String, accent: Color,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = typo.h2, color = accent)
        Spacer(Modifier.height(2.dp))
        Text(label, style = typo.labelSmall, color = colors.textSecondary,
            textAlign = TextAlign.Center, fontSize = 9.sp)
    }
}

@Composable
private fun PulseEmpty(
    text: String,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp)).background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(8.dp)).padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = typo.caption, color = colors.textSecondary,
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun PulseHeader(
    title: String,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(20.dp).background(accent))
        Spacer(Modifier.width(10.dp))
        Text(title, style = typo.label, color = colors.textPrimary)
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(color = colors.divider, thickness = 1.dp,
            modifier = Modifier.weight(1f))
    }
}
