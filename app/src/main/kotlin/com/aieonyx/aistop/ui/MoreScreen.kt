// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MVoid  = Color(0xFF080A0D)
private val MBlue  = Color(0xFF4F80D4)
private val MTeal  = Color(0xFF3EB69F)
private val MWhite = Color(0xFFEDF3FA)
private val MSub   = Color(0x8CEDF3FA)
private val MSurf1 = Color(0x0AEDF3FA)
private val MSurf2 = Color(0x14EDF3FA)

@Composable
fun MoreScreen() {
    val context = LocalContext.current
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
            .background(MVoid),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "More",
                    color      = MWhite,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Export ──
        item { MoreSectionHeader("EXPORT") }
        item {
            MoreCard {
                MoreActionRow(
                    label  = "Export exposure log",
                    detail = "Ed25519 signed JSON · EdisonDB provenance chain",
                    color  = MBlue,
                    action = "Export →"
                ) { /* wire to ExportManager */ }
                HorizontalDivider(color = MSurf2)
                MoreRow("Export format", "BLAKE3 hash + ARPi header per record", MSub)
                HorizontalDivider(color = MSurf2)
                MoreRow("Retention", "30 days · On-device only", MSub)
            }
        }

        // ── PII Scanner ──
        item { MoreSectionHeader("PII SCANNER") }
        item {
            MoreCard {
                MoreActionRow(
                    label  = "Scan clipboard",
                    detail = "Check current clipboard for API keys, passwords, PII",
                    color  = MBlue,
                    action = "Scan →"
                ) { /* wire to PII scan */ }
                HorizontalDivider(color = MSurf2)
                MoreActionRow(
                    label  = "Coverage matrix",
                    detail = "See what AI Stop detects and what it doesn't",
                    color  = MSub,
                    action = "View →"
                ) { showCoverage = true }
            }
        }

        // ── Protection ──
        item { MoreSectionHeader("PROTECTION") }
        item {
            MoreCard {
                MoreActionRow(
                    label  = "Sovereign Guard",
                    detail = "Accessibility-based paste interception for AI apps",
                    color  = MSub,
                    action = "Configure →"
                ) { showDisclosure = true }
                HorizontalDivider(color = MSurf2)
                MoreRow("AI Stop Keyboard", "IME-based type-time interception", MSub)
                HorizontalDivider(color = MSurf2)
                MoreRow("ScrubShare", "Share sheet PII scrubber · Always available", MSub)
            }
        }

        // ── Sovereign Proof ──
        item { MoreSectionHeader("SOVEREIGN PROOF") }
        item {
            MoreCard {
                MoreRow("Storage backend", "EdisonDB · Sovereign embedded DB", MTeal)
                HorizontalDivider(color = MSurf2)
                MoreRow("Provenance", "ARPi 78-byte header · Every record", MTeal)
                HorizontalDivider(color = MSurf2)
                MoreRow("Integrity", "BLAKE3 hash · Tamper-evident log", MTeal)
                HorizontalDivider(color = MSurf2)
                MoreRow("Export signing", "Ed25519 · Non-repudiation guaranteed", MTeal)
                HorizontalDivider(color = MSurf2)
                MoreRow("GDPR Art.17", "Key destruction = erasure · Verified", MTeal)
                HorizontalDivider(color = MSurf2)
                MoreRow("Network", "Zero external calls · On-device only", MTeal)
            }
        }

        // ── Trust Score methodology ──
        item { MoreSectionHeader("TRUST SCORE METHODOLOGY") }
        item {
            MoreCard {
                MoreRow("Data Retention",       "Weight: 40%", MSub)
                HorizontalDivider(color = MSurf2)
                MoreRow("Transparency",         "Weight: 30%", MSub)
                HorizontalDivider(color = MSurf2)
                MoreRow("Opt-out Controls",     "Weight: 20%", MSub)
                HorizontalDivider(color = MSurf2)
                MoreRow("Third-party Sharing",  "Weight: 10%", MSub)
                HorizontalDivider(color = MSurf2)
                MoreRow("Dataset date",         "2026-07", MSub)
                HorizontalDivider(color = MSurf2)
                MoreRow("Source",               "Public privacy policies", MSub)
            }
        }

        // ── About ──
        item { MoreSectionHeader("ABOUT") }
        item {
            MoreCard {
                MoreRow("Version",   "v${packageInfo?.versionName ?: "1.0.0"}", MSub)
                HorizontalDivider(color = MSurf2)
                MoreRow("Developer", "AIEONYX", MBlue)
                HorizontalDivider(color = MSurf2)
                MoreRow("License",   "Apache 2.0", MSub)
                HorizontalDivider(color = MSurf2)
                MoreRow("Storage",   "EdisonDB · Sovereign · Open-source", MBlue)
                HorizontalDivider(color = MSurf2)
                MoreRow("Mission",   "Revenue funds sovereign open-source computing", MSub)
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "Copyright (c) 2026 Edison Lepiten / AIEONYX",
                color      = MSub.copy(alpha = 0.35f),
                fontSize   = 9.sp,
                fontFamily = FontFamily.Monospace,
                textAlign  = TextAlign.Center,
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            )
        }
    }
}

@Composable
private fun MoreSectionHeader(title: String) {
    Text(
        title,
        color         = MSub,
        fontSize      = 10.sp,
        fontFamily    = FontFamily.Monospace,
        letterSpacing = 0.5.sp,
        modifier      = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
    )
}

@Composable
private fun MoreCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .background(MSurf1, RoundedCornerShape(14.dp)),
        content = content
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun MoreRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Text(
            label,
            color    = MWhite,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            value,
            color         = valueColor,
            fontSize      = 11.sp,
            fontFamily    = FontFamily.Monospace,
            textAlign     = androidx.compose.ui.text.style.TextAlign.End,
            lineHeight    = 15.sp,
            modifier      = Modifier.weight(0.55f)
        )
    }
}

@Composable
private fun MoreActionRow(
    label:  String,
    detail: String,
    color:  Color,
    action: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(label,  color = MWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(detail, color = MSub,   fontSize = 10.sp, lineHeight = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            action,
            color      = color,
            fontSize   = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier   = Modifier.padding(top = 2.dp)
        )
    }
}
