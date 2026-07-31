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
import com.aieonyx.aistop.ui.theme.AiStopTheme
import com.aieonyx.aistop.vpn.AiCreepDetector
import com.aieonyx.aistop.vpn.FlowTracker
import com.aieonyx.aistop.vpn.VpnDataBridge
import com.aieonyx.aistop.vpn.VpnIntegration
import kotlinx.coroutines.delay

@Composable
fun ShieldScreen() {
    val context = LocalContext.current
    val colors  = AiStopTheme.colors
    val typo    = AiStopTheme.typography

    val purple = Color(0xFFA78BFA)
    val red    = Color(0xFFF87171)
    val amber  = Color(0xFFFBBF24)

    // Refresh from bridge every 2 seconds
    var vpnRunning      by remember { mutableStateOf(VpnIntegration.isRunning(context)) }
    var totalBytes      by remember { mutableStateOf(VpnDataBridge.totalIntercepted) }
    var blockLog        by remember { mutableStateOf(VpnDataBridge.blockLog) }
    var detections      by remember { mutableStateOf(VpnDataBridge.detections) }
    var flows           by remember { mutableStateOf(VpnDataBridge.flows) }
    var trafficHistory  by remember { mutableStateOf(VpnDataBridge.trafficHistory) }

    LaunchedEffect(Unit) {
        while (true) {
            vpnRunning     = VpnIntegration.isRunning(context)
            totalBytes     = VpnDataBridge.totalIntercepted
            blockLog       = VpnDataBridge.blockLog
            detections     = VpnDataBridge.detections
            flows          = VpnDataBridge.flows
            trafficHistory = VpnDataBridge.trafficHistory
            delay(2000)
        }
    }

    val totalBlocked   = blockLog.values.sum()
    val crawlerCount   = detections.count { it.category == AiCreepDetector.Category.AI_CRAWLER }
    val telemetryCount = detections.count { it.category == AiCreepDetector.Category.AI_TELEMETRY }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {

        // ── Header ──
        item {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("SHIELD", style = typo.h1, color = colors.textPrimary)
                    Text("SOVEREIGN THREAT MONITOR", style = typo.labelSmall, color = colors.accentSecondary)
                }
                // Live / offline badge
                val badgeColor = if (vpnRunning) purple else colors.disabled
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (vpnRunning) "● LIVE" else "○ OFFLINE",
                        style = typo.labelSmall,
                        color = badgeColor
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Stats row ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShieldStatCard("BLOCKED",      totalBlocked.toString(),      red,    colors, typo, Modifier.weight(1f))
                ShieldStatCard("INTERCEPTED",  formatBytes(totalBytes),      amber,  colors, typo, Modifier.weight(1f))
                ShieldStatCard("AI CRAWLERS",  crawlerCount.toString(),      purple, colors, typo, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Traffic chart ──
        item {
            ShieldSectionHeader("TRAFFIC · LAST ${trafficHistory.size * 2} MIN", colors, typo)
        }
        item {
            if (trafficHistory.isEmpty()) {
                ShieldEmptyCard(
                    if (vpnRunning) "Collecting traffic data…" else "Enable Sovereign Shield on the PROTECT tab",
                    colors, typo
                )
            } else {
                TrafficBarChart(history = trafficHistory, red = red, colors = colors, typo = typo)
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── AI threats blocked ──
        item { ShieldSectionHeader("AI THREATS BLOCKED", colors, typo) }

        if (blockLog.isEmpty() && detections.isEmpty()) {
            item {
                ShieldEmptyCard(
                    if (vpnRunning) "No threats detected yet" else "Enable Sovereign Shield to start blocking",
                    colors, typo
                )
            }
        } else {
            // Show DNS blocks first, then SNI detections
            val sortedBlocks = blockLog.entries.sortedByDescending { it.value }
            items(sortedBlocks.take(10)) { (host, count) ->
                BlockRow(hostname = host, count = count, badge = "DNS", badgeColor = red, colors = colors, typo = typo)
                Spacer(Modifier.height(4.dp))
            }
            val sniDetections = detections
                .filter { it.category != AiCreepDetector.Category.CLEAN }
                .takeLast(5)
                .reversed()
            items(sniDetections) { det ->
                val (badge, bc) = when (det.category) {
                    AiCreepDetector.Category.AI_CRAWLER   -> "SNI·CRAWLER"   to red
                    AiCreepDetector.Category.AI_TELEMETRY -> "SNI·TELEMETRY" to amber
                    AiCreepDetector.Category.AI_MODEL_API -> "SNI·API"       to purple
                    else                                  -> "SNI"           to colors.disabled
                }
                BlockRow(hostname = det.hostname, count = null, badge = badge, badgeColor = bc, colors = colors, typo = typo)
                Spacer(Modifier.height(4.dp))
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // ── Active network flows ──
        item { ShieldSectionHeader("ACTIVE FLOWS", colors, typo) }

        if (flows.isEmpty()) {
            item {
                ShieldEmptyCard(
                    if (vpnRunning) "No active flows observed" else "Enable Sovereign Shield",
                    colors, typo
                )
            }
        } else {
            val maxBytes = flows.firstOrNull()?.bytes ?: 1L
            items(flows.take(15)) { flow ->
                FlowRowItem(flow = flow, maxBytes = maxBytes, colors = colors, typo = typo)
                Spacer(Modifier.height(4.dp))
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "ALL INTERCEPTION RUNS ON-DEVICE · NO DATA LEAVES AI STOP",
                style     = typo.caption,
                color     = colors.textSecondary.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Traffic bar chart (native Compose drawBehind) ────────────────────────────

@Composable
private fun TrafficBarChart(
    history: List<VpnDataBridge.TrafficWindow>,
    red: Color,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography
) {
    val maxVal = history.maxOf { maxOf(it.blocked, it.allowed) }.coerceAtLeast(1L)
    val blue   = Color(0xFF3B82F6)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Bar chart drawn with drawBehind
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .drawBehind {
                    val count   = history.size
                    val barW    = (size.width / count) * 0.35f
                    val gap     = (size.width / count) * 0.15f
                    val slotW   = size.width / count

                    history.forEachIndexed { i, window ->
                        val x = i * slotW + gap

                        // Blocked bar (red)
                        val bH = (window.blocked.toFloat() / maxVal) * size.height
                        drawRect(
                            color   = red.copy(alpha = 0.8f),
                            topLeft = Offset(x, size.height - bH),
                            size    = Size(barW, bH)
                        )

                        // Allowed bar (blue)
                        val aH = (window.allowed.toFloat() / maxVal) * size.height
                        drawRect(
                            color   = blue.copy(alpha = 0.6f),
                            topLeft = Offset(x + barW + 2f, size.height - aH),
                            size    = Size(barW, aH)
                        )
                    }
                }
        )

        Spacer(Modifier.height(6.dp))

        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            history.take(6).forEach { w ->
                Text(w.label, style = typo.labelSmall, color = colors.textSecondary, fontSize = 9.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot("Blocked", red.copy(alpha = 0.8f), typo, colors)
            LegendDot("Allowed", Color(0xFF3B82F6).copy(alpha = 0.6f), typo, colors)
        }
    }
}

@Composable
private fun LegendDot(
    label: String,
    color: Color,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, style = typo.labelSmall, color = colors.textSecondary, fontSize = 10.sp)
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun ShieldStatCard(
    label: String,
    value: String,
    accent: Color,
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
        Text(label, style = typo.labelSmall, color = colors.textSecondary, textAlign = TextAlign.Center, fontSize = 9.sp)
    }
}

@Composable
private fun BlockRow(
    hostname: String,
    count: Int?,
    badge: String,
    badgeColor: Color,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(badgeColor.copy(alpha = 0.06f))
            .border(1.dp, badgeColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⛔", fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
        Text(
            hostname,
            style    = typo.caption,
            color    = colors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(badgeColor.copy(alpha = 0.15f))
                .border(0.5.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                if (count != null) "$badge ×$count" else badge,
                style = typo.labelSmall,
                color = badgeColor,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun FlowRowItem(
    flow: FlowTracker.FlowStats,
    maxBytes: Long,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography
) {
    val fraction = (flow.bytes.toFloat() / maxBytes).coerceIn(0f, 1f)
    val barColor = if (flow.key.proto == "TCP") Color(0xFF818CF8) else Color(0xFF34D399)

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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor.copy(alpha = 0.15f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(flow.key.proto, style = typo.labelSmall, color = barColor, fontSize = 9.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "${flow.key.dstIp}:${flow.key.dstPort}",
                style    = typo.caption,
                color    = colors.textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(formatBytes(flow.bytes), style = typo.labelSmall, color = colors.textSecondary)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.outline)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun ShieldEmptyCard(
    text: String,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = typo.caption, color = colors.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ShieldSectionHeader(
    title: String,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(20.dp).background(Color(0xFFA78BFA)))
        Spacer(Modifier.width(10.dp))
        Text(title, style = typo.label, color = colors.textPrimary)
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(color = colors.divider, thickness = 1.dp, modifier = Modifier.weight(1f))
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000     -> "%.1f KB".format(bytes / 1_000.0)
    else               -> "$bytes B"
}
