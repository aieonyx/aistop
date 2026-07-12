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
import com.aieonyx.aistop.core.TrustDatabase

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
 * App Detail Screen — shows full Trust Score breakdown for one AI app.
 * Explains WHY the score is what it is.
 */
@Composable
fun AppDetailScreen(
    packageName: String,
    appLabel:    String,
    trustScore:  Int,
    onBack:      () -> Unit
) {
    val scoreColor = when {
        trustScore < 40 -> Red
        trustScore < 70 -> Amber
        else            -> Teal
    }
    val scoreLabel = when {
        trustScore < 40 -> "HIGH RISK"
        trustScore < 70 -> "CAUTION"
        else            -> "TRUSTED"
    }

    // Score breakdown per app
    val breakdown = TrustDatabase.getBreakdown(packageName)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Void)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("‹ Back", color = Blue, fontSize = 16.sp)
            }
            Spacer(Modifier.weight(1f))
        }

        // ── App header ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App initial badge
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        androidx.compose.ui.graphics.Color(TrustDatabase.brandColor(packageName)),
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    appLabel.take(1).uppercase(),
                    color      = White,
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(appLabel, color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(packageName, color = Sub, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(16.dp))

            // Big score
            Text(
                "$trustScore",
                color      = scoreColor,
                fontSize   = 64.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                lineHeight = 68.sp
            )
            Surface(
                color  = scoreColor.copy(alpha = 0.12f),
                shape  = RoundedCornerShape(6.dp)
            ) {
                Text(
                    scoreLabel,
                    color      = scoreColor,
                    fontSize   = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Score based on public privacy policy · data as of 2026",
                color    = Sub.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Score breakdown ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            Text(
                "SCORE BREAKDOWN",
                color      = Sub,
                fontSize   = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                modifier   = Modifier.padding(bottom = 8.dp)
            )

            breakdown.forEach { item: TrustDatabase.ScoreItem ->
                ScoreRow(item)
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── What this means ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .background(Surf1, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text(
                "WHAT THIS MEANS",
                color      = Sub,
                fontSize   = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(10.dp))

            val implications = TrustDatabase.getImplications(packageName)
            implications.forEach { line: String ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("·", color = scoreColor, fontSize = 12.sp)
                    Text(line, color = White.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Methodology note ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .background(Surf1, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text(
                "METHODOLOGY",
                color      = Sub,
                fontSize   = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Scores are calculated from four weighted criteria assessed against each app's publicly available privacy policy. This is not legal advice.",
                color    = Sub,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CriteriaChip("Data Retention · 40%")
                CriteriaChip("Transparency · 30%")
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CriteriaChip("Opt-out · 20%")
                CriteriaChip("3rd Party · 10%")
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ScoreRow(item: TrustDatabase.ScoreItem) {
    val color = when {
        item.score < 40 -> Red
        item.score < 70 -> Amber
        else            -> Teal
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surf1, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.label, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(item.detail, color = Sub, fontSize = 10.sp, lineHeight = 14.sp)
            }
            Text(
                "${item.score}",
                color      = color,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(8.dp))
        // Score bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Surf2, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(item.score / 100f)
                    .height(4.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun CriteriaChip(label: String) {
    Surface(
        color = Surf2,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            label,
            color    = Sub,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
