// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Void  = Color(0xFF080A0D)
private val Blue  = Color(0xFF4F80D4)
private val Red   = Color(0xFFE45F65)
private val Amber = Color(0xFFD7A84B)
private val Teal  = Color(0xFF3EB69F)
private val White = Color(0xFFEDF3FA)
private val Sub   = Color(0x8CEDF3FA)
private val Surf1 = Color(0x0AEDF3FA)
private val Surf2 = Color(0x14EDF3FA)

/**
 * Protection Coverage Matrix — P0 feature.
 *
 * Honest disclosure of what AI Stop can and cannot see.
 * All 4 audits flagged this as essential — users must know
 * the boundaries of protection to trust the app.
 *
 * Status levels:
 *   PROTECTED  — full coverage, teal
 *   PARTIAL    — limited coverage, amber
 *   NOT PROTECTED — no coverage, red
 *   USER ACTION — requires user action, blue
 */
@Composable
fun CoverageMatrixScreen(onBack: () -> Unit) {
    data class CoverageRow(
        val vector:  String,
        val status:  String,
        val note:    String,
        val color:   Color
    )

    val rows = listOf(
        CoverageRow("Paste via AI Stop keyboard",  "PROTECTED",      "Full interception — BLOCK / REDACT / ALLOW", Teal),
        CoverageRow("Scrub & Share (text)",        "PROTECTED",      "Full PII detection before sharing",          Teal),
        CoverageRow("Scrub & Share (image)",       "PROTECTED",      "EXIF GPS + device metadata stripped",        Teal),
        CoverageRow("Clipboard auto-monitoring",   "PROTECTED",      "Sovereign Guard detects AI app foreground",  Teal),
        CoverageRow("Signed export log",           "PROTECTED",      "Ed25519 + BLAKE3 tamper-evident log",        Teal),
        CoverageRow("App paste button (in-app)",   "PARTIAL",        "May bypass IME — use AI Stop keyboard",      Amber),
        CoverageRow("Hardware keyboard paste",     "PARTIAL",        "Device dependent — IME may not intercept",   Amber),
        CoverageRow("Browser AI (web)",            "PARTIAL",        "Browser shows as destination, not AI site",  Amber),
        CoverageRow("Direct file upload (picker)", "USER ACTION",    "Route files through Scrub & Share instead",  Blue),
        CoverageRow("Voice / microphone input",    "NOT PROTECTED",  "Audio streaming not intercepted in v1.1",    Red),
        CoverageRow("Screen recording",            "NOT PROTECTED",  "Cannot observe other apps capturing screen", Red),
        CoverageRow("Network telemetry",           "NOT PROTECTED",  "Background API calls not monitored in v1.1", Red),
        CoverageRow("Typed text (not pasted)",     "NOT PROTECTED",  "Use Scrub & Share before typing sensitive data", Red),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Void)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("‹ Back", color = Blue, fontSize = 16.sp)
            }
            Text(
                "Protection Coverage",
                color      = White,
                fontSize   = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.weight(1f)
            )
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendChip("Protected", Teal)
            LegendChip("Partial", Amber)
            LegendChip("User action", Blue)
            LegendChip("Not covered", Red)
        }

        Spacer(Modifier.height(8.dp))

        // Matrix rows
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surf1, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Status dot
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .size(8.dp)
                            .background(row.color, RoundedCornerShape(4.dp))
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            row.vector,
                            color      = White,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            row.note,
                            color    = Sub,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Surface(
                        color  = row.color.copy(alpha = 0.12f),
                        shape  = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            row.status,
                            color      = row.color,
                            fontSize   = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Honest footer
            Surface(
                color  = Surf1,
                shape  = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "HONEST BOUNDARIES",
                        color      = Sub,
                        fontSize   = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "AI Stop is a paste and share firewall — not a complete AI data blocking solution. " +
                        "Voice input, network telemetry, and direct file uploads are not covered in v1.1. " +
                        "These limitations are honest — we will not claim protection we cannot provide.",
                        color    = Sub,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LegendChip(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Text(label, color = Sub, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}
