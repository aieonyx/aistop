// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.aieonyx.aistop.vpn.AllowlistManager
import com.aieonyx.aistop.vpn.BlockedDomains
import com.aieonyx.aistop.vpn.FlowTracker
import com.aieonyx.aistop.vpn.VpnDataBridge
import com.aieonyx.aistop.vpn.VpnIntegration
import kotlinx.coroutines.delay

@Composable
fun ShieldScreen() {
    val context = LocalContext.current
    val colors  = AiStopTheme.colors
    val typo    = AiStopTheme.typography
    val purple  = Color(0xFFA78BFA)
    val red     = Color(0xFFF87171)
    val amber   = Color(0xFFFBBF24)

    var vpnRunning     by remember { mutableStateOf(VpnIntegration.isRunning(context)) }
    var totalBytes     by remember { mutableStateOf(VpnDataBridge.totalIntercepted) }
    var blockLog       by remember { mutableStateOf(VpnDataBridge.blockLog) }
    var flows          by remember { mutableStateOf(VpnDataBridge.flows) }
    var trafficHistory by remember { mutableStateOf(VpnDataBridge.trafficHistory) }
    var allowEntries   by remember { mutableStateOf(AllowlistManager.getAllEntries()) }

    // Unblock sheet state
    var unblockHost     by remember { mutableStateOf<String?>(null) }
    var unblockCategory by remember { mutableStateOf("CRAWLER") }

    LaunchedEffect(Unit) {
        while (true) {
            vpnRunning     = VpnIntegration.isRunning(context)
            totalBytes     = VpnDataBridge.totalIntercepted
            blockLog       = VpnDataBridge.blockLog
            flows          = VpnDataBridge.flows
            trafficHistory = VpnDataBridge.trafficHistory
            allowEntries   = AllowlistManager.getAllEntries()
            delay(2000)
        }
    }

    val totalBlocked = blockLog.values.sum()

    // Show unblock sheet
    unblockHost?.let { host ->
        UnblockSheet(
            hostname = host,
            category = unblockCategory,
            onDismiss = { unblockHost = null },
            onAllowed = { duration ->
                AllowlistManager.allow(context, host, duration)
                allowEntries = AllowlistManager.getAllEntries()
                unblockHost  = null
            }
        )
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
                    Text("SHIELD", style = typo.h1, color = colors.textPrimary)
                    Text("SOVEREIGN THREAT MONITOR", style = typo.labelSmall, color = colors.accentSecondary)
                }
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
                        style = typo.labelSmall, color = badgeColor
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Stats ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShieldStat("BLOCKED",     totalBlocked.toString(),           red,    colors, typo, Modifier.weight(1f))
                ShieldStat("INTERCEPTED", formatBytes(totalBytes),           amber,  colors, typo, Modifier.weight(1f))
                ShieldStat("ALLOWLISTED", allowEntries.size.toString(),      purple, colors, typo, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Traffic chart ──
        item { ShieldHeader("TRAFFIC · LAST ${trafficHistory.size * 2} MIN", colors, typo) }
        item {
            if (trafficHistory.isEmpty()) {
                ShieldEmpty(if (vpnRunning) "Collecting traffic data…" else "Enable Sovereign Shield on PROTECT tab", colors, typo)
            } else {
                TrafficChart(trafficHistory, red, colors, typo)
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── User allowlist ──
        if (allowEntries.isNotEmpty()) {
            item { ShieldHeader("USER ALLOWLIST", colors, typo) }
            items(allowEntries) { entry ->
                AllowRow(
                    entry   = entry,
                    colors  = colors,
                    typo    = typo,
                    onRevoke = {
                        AllowlistManager.revoke(context, entry.hostname)
                        allowEntries = AllowlistManager.getAllEntries()
                    }
                )
                Spacer(Modifier.height(4.dp))
            }
            item { Spacer(Modifier.height(20.dp)) }
        }

        // ── AI threats blocked ──
        item { ShieldHeader("AI THREATS BLOCKED  •  tap to unblock", colors, typo) }

        if (blockLog.isEmpty()) {
            item {
                ShieldEmpty(
                    if (vpnRunning) "No threats detected yet — try visiting an AI site"
                    else "Enable Sovereign Shield to start blocking",
                    colors, typo
                )
            }
        } else {
            val sorted = blockLog.entries.sortedByDescending { it.value }
            items(sorted.take(20)) { (host, count) ->
                val isAllowed  = AllowlistManager.isAllowed(host)
                val entry      = AllowlistManager.getEntry(host)
                val category   = BlockedDomains.categoryOf(host)
                val badgeColor = when {
                    isAllowed             -> amber
                    category == "TELEMETRY" -> amber
                    category == "API"       -> purple
                    else                  -> red
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.06f))
                        .border(1.dp, badgeColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .clickable {
                            if (!isAllowed) {
                                unblockHost     = host
                                unblockCategory = category
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isAllowed) "✓" else "⛔",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            host,
                            style    = typo.caption,
                            color    = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isAllowed && entry != null) {
                            Text(
                                "Allowed — ${entry.remainingLabel()}",
                                style = typo.labelSmall,
                                color = amber,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(0.5.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (isAllowed) "ALLOWED ×$count"
                            else "$category ×$count",
                            style    = typo.labelSmall,
                            color    = badgeColor,
                            fontSize = 9.sp
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // ── Active flows ──
        item { ShieldHeader("ACTIVE FLOWS", colors, typo) }
        if (flows.isEmpty()) {
            item { ShieldEmpty(if (vpnRunning) "No flows yet" else "Enable Sovereign Shield", colors, typo) }
        } else {
            val maxBytes = flows.firstOrNull()?.bytes ?: 1L
            items(flows.take(15)) { flow ->
                FlowItem(flow, maxBytes, colors, typo)
                Spacer(Modifier.height(4.dp))
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "ALL INTERCEPTION RUNS ON-DEVICE · TAP ANY BLOCKED DOMAIN TO TEMPORARILY ALLOW",
                style     = typo.caption,
                color     = colors.textSecondary.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Allow row ─────────────────────────────────────────────────────────────────

@Composable
private fun AllowRow(
    entry: AllowlistManager.AllowEntry,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography,
    onRevoke: () -> Unit
) {
    val amber = Color(0xFFFBBF24)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(amber.copy(alpha = 0.06f))
            .border(1.dp, amber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("✓", fontSize = 12.sp, color = amber, modifier = Modifier.padding(end = 8.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.hostname, style = typo.caption, color = colors.textPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entry.remainingLabel(), style = typo.labelSmall, color = amber, fontSize = 9.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(colors.danger.copy(alpha = 0.15f))
                .border(0.5.dp, colors.danger.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .clickable { onRevoke() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("REVOKE", style = typo.labelSmall, color = colors.danger, fontSize = 9.sp)
        }
    }
}

// ── Traffic chart ─────────────────────────────────────────────────────────────

@Composable
private fun TrafficChart(
    history: List<VpnDataBridge.TrafficWindow>,
    red: Color,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography
) {
    val blue   = Color(0xFF3B82F6)
    val maxVal = history.maxOf { maxOf(it.blocked, it.allowed) }.coerceAtLeast(1L)

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
                    val count = history.size
                    val barW  = (size.width / count) * 0.35f
                    val slotW = size.width / count
                    history.forEachIndexed { i, w ->
                        val x  = i * slotW + slotW * 0.1f
                        val bH = (w.blocked.toFloat() / maxVal) * size.height
                        val aH = (w.allowed.toFloat() / maxVal) * size.height
                        drawRect(red.copy(alpha = 0.8f),
                            Offset(x, size.height - bH), Size(barW, bH))
                        drawRect(blue.copy(alpha = 0.6f),
                            Offset(x + barW + 2f, size.height - aH), Size(barW, aH))
                    }
                }
        )
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            history.take(6).forEach { w ->
                Text(w.label, style = typo.labelSmall, color = colors.textSecondary, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot("Blocked", red.copy(alpha = 0.8f), typo, colors)
            LegendDot("Allowed", blue.copy(alpha = 0.6f), typo, colors)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, style = typo.labelSmall, color = colors.textSecondary, fontSize = 10.sp)
    }
}

// ── Flow row ──────────────────────────────────────────────────────────────────

@Composable
private fun FlowItem(
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
            Box(modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(barColor.copy(alpha = 0.15f))
                .padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text(flow.key.proto, style = typo.labelSmall, color = barColor, fontSize = 9.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text("${flow.key.dstIp}:${flow.key.dstPort}", style = typo.caption,
                color = colors.textPrimary, modifier = Modifier.weight(1f),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatBytes(flow.bytes), style = typo.labelSmall, color = colors.textSecondary)
        }
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(3.dp)
            .clip(RoundedCornerShape(2.dp)).background(colors.outline)) {
            Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight().background(barColor))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun ShieldStat(label: String, value: String, accent: Color,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography,
    modifier: Modifier = Modifier) {
    Column(modifier = modifier
        .clip(RoundedCornerShape(10.dp))
        .background(colors.surface)
        .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
        .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = typo.h2, color = accent)
        Spacer(Modifier.height(2.dp))
        Text(label, style = typo.labelSmall, color = colors.textSecondary,
            textAlign = TextAlign.Center, fontSize = 9.sp)
    }
}

@Composable
private fun ShieldEmpty(text: String,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(8.dp)).background(colors.surface)
        .border(1.dp, colors.outline, RoundedCornerShape(8.dp)).padding(20.dp),
        contentAlignment = Alignment.Center) {
        Text(text, style = typo.caption, color = colors.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ShieldHeader(title: String,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
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
