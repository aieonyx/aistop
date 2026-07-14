// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aieonyx.aistop.ui.theme.AiStopColors
import com.aieonyx.aistop.ui.theme.AiStopTheme
import com.aieonyx.aistop.ui.theme.AiStopTypography

@Composable
fun MoreScreen() {
    val context     = LocalContext.current
    val colors      = AiStopTheme.colors
    val typo        = AiStopTheme.typography
    var showDisclosure by remember { mutableStateOf(false) }
    var showCoverage   by remember { mutableStateOf(false) }

    val packageInfo = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0) }
        catch (e: Exception) { null }
    }

    if (showDisclosure) {
        AccessibilityDisclosureScreen(
            onAccept  = { showDisclosure = false },
            onDecline = { showDisclosure = false }
        )
        return
    }
    if (showCoverage) {
        CoverageMatrixScreen(onBack = { showCoverage = false })
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MORE", style = typo.h1, color = colors.textPrimary)
            }
        }

        // ── Export ──
        item { MoreSectionHeader("EXPORT", colors, typo) }
        item {
            MoreCard(colors) {
                MoreActionRow("EXPORT EXPOSURE LOG", "Ed25519 signed JSON · EdisonDB chain", colors.accentPrimary, "EXPORT →", colors, typo) {}
                HorizontalDivider(color = colors.divider)
                MoreRow("Export format", "BLAKE3 + ARPi header per record", colors.textSecondary, colors, typo)
                HorizontalDivider(color = colors.divider)
                MoreRow("Retention", "30 days · On-device only", colors.textSecondary, colors, typo)
            }
        }

        // ── PII Scanner ──
        item { MoreSectionHeader("PII SCANNER", colors, typo) }
        item {
            MoreCard(colors) {
                MoreActionRow("SCAN CLIPBOARD", "Check clipboard for API keys, passwords, PII", colors.accentPrimary, "SCAN →", colors, typo) {}
                HorizontalDivider(color = colors.divider)
                MoreActionRow("COVERAGE MATRIX", "See what AI Stop detects", colors.textSecondary, "VIEW →", colors, typo) { showCoverage = true }
            }
        }

        // ── Protection ──
        item { MoreSectionHeader("PROTECTION", colors, typo) }
        item {
            MoreCard(colors) {
                MoreActionRow("SOVEREIGN GUARD", "Accessibility-based paste interception", colors.textSecondary, "CONFIGURE →", colors, typo) { showDisclosure = true }
                HorizontalDivider(color = colors.divider)
                MoreRow("AI Stop Keyboard", "IME-based type-time interception", colors.textSecondary, colors, typo)
                HorizontalDivider(color = colors.divider)
                MoreRow("ScrubShare", "Share sheet PII scrubber · Always available", colors.textSecondary, colors, typo)
            }
        }

        // ── Sovereign Proof ──
        item { MoreSectionHeader("SOVEREIGN PROOF", colors, typo) }
        item {
            MoreCard(colors) {
                MoreRow("Storage backend",  "EdisonDB · Sovereign embedded DB",           colors.success, colors, typo)
                HorizontalDivider(color = colors.divider)
                MoreRow("Provenance",       "ARPi 78-byte header · Every record",         colors.success, colors, typo)
                HorizontalDivider(color = colors.divider)
                MoreRow("Integrity",        "BLAKE3 hash · Tamper-evident log",           colors.success, colors, typo)
                HorizontalDivider(color = colors.divider)
                MoreRow("Export signing",   "Ed25519 · Non-repudiation guaranteed",       colors.success, colors, typo)
                HorizontalDivider(color = colors.divider)
                MoreRow("GDPR Art.17",      "Key destruction = erasure · Verified",       colors.success, colors, typo)
                HorizontalDivider(color = colors.divider)
                MoreRow("Network",          "Zero external calls · On-device only",       colors.success, colors, typo)
            }
        }

        // ── Trust Score methodology ──
        item { MoreSectionHeader("TRUST SCORE METHODOLOGY", colors, typo) }
        item {
            MoreCard(colors) {
                MoreRow("Data Retention",      "Weight: 40%", colors.textSecondary, colors, typo)
                HorizontalDivider(color = colors.divider)
                MoreRow("Transparency",        "Weight: 30%", colors.textSecondary, colors, typo)
                HorizontalDivider(color = colors.divider)
                MoreRow("Opt-out Controls",    "Weight: 20%", colors.textSecondary, colors, typo)
                HorizontalDivider(color = colors.divider)
                MoreRow("Third-party Sharing", "Weight: 10%", colors.textSecondary, colors, typo)
                HorizontalDivider(color = colors.divider)
                MoreRow("Dataset date",        "2026-07",     colors.textSecondary, colors, typo)
            }
        }

        // ── Sovereign Vault ──
        item { VaultScreen() }

        // ── About ──
        item { MoreSectionHeader("ABOUT", colors, typo) }
        item {
            MoreCard(colors) {
                MoreRow("Version",   "v${packageInfo?.versionName ?: "1.0.0"}", colors.textSecondary, colors, typo)
                HorizontalDivider(color = colors.divider)
                MoreRow("Developer", "AIEONYX",                                  colors.accentPrimary,  colors, typo)
                HorizontalDivider(color = colors.divider)
                MoreRow("License",   "Apache 2.0",                              colors.textSecondary,  colors, typo)
                HorizontalDivider(color = colors.divider)
                MoreRow("Storage",   "EdisonDB · Sovereign · Open-source",      colors.accentPrimary,  colors, typo)
                HorizontalDivider(color = colors.divider)
                MoreRow("Mission",   "Revenue funds sovereign open-source computing", colors.textSecondary, colors, typo)
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "COPYRIGHT (C) 2026 EDISON LEPITEN / AIEONYX",
                style     = typo.caption,
                color     = colors.disabled,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
            )
        }
    }
}

@Composable
private fun MoreSectionHeader(title: String, colors: AiStopColors, typo: AiStopTypography) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .background(colors.accentSecondary)
        )
        Spacer(Modifier.width(10.dp))
        Text(title, style = typo.label, color = colors.textPrimary)
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(color = colors.divider, thickness = 1.dp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MoreCard(colors: AiStopColors, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp)),
        content = content
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun MoreRow(label: String, value: String, valueColor: Color, colors: AiStopColors, typo: AiStopTypography) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Text(label, style = typo.bodySmall, color = colors.textPrimary, modifier = Modifier.weight(0.45f))
        Text(
            value,
            style     = typo.caption,
            color     = valueColor,
            textAlign = TextAlign.End,
            modifier  = Modifier.weight(0.55f)
        )
    }
}

@Composable
private fun MoreActionRow(
    label:   String,
    detail:  String,
    color:   Color,
    action:  String,
    colors:  AiStopColors,
    typo:    AiStopTypography,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(label,  style = typo.label,   color = colors.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(detail, style = typo.caption, color = colors.textSecondary)
        }
        Spacer(Modifier.width(8.dp))
        Text(action, style = typo.labelSmall, color = color, modifier = Modifier.padding(top = 2.dp))
    }
}
