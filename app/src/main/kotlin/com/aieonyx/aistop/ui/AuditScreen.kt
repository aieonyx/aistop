// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aieonyx.aistop.core.PermissionScanner
import com.aieonyx.aistop.db.EventType
import com.aieonyx.aistop.db.ExposureDatabase
import kotlinx.coroutines.launch

private val SovereignVoid  = Color(0xFF080A0D)
private val SovereignBlue  = Color(0xFF4F80D4)
private val ThreatRed      = Color(0xFFE45F65)
private val CautionAmber   = Color(0xFFD7A84B)
private val VerifiedTeal   = Color(0xFF3EB69F)
private val SignalWhite    = Color(0xFFEDF3FA)
private val SubText        = Color(0x8CEDF3FA)
private val Surface1       = Color(0x0AEDF3FA)
private val Surface2       = Color(0x12EDF3FA)

/**
 * Dashboard / AI App Audit screen.
 * Shows installed AI apps with Trust Scores and risk metrics.
 */
@Composable
fun AuditScreen() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var apps         by remember { mutableStateOf<List<PermissionScanner.AuditedApp>>(emptyList()) }
    var blockedToday by remember { mutableStateOf(0) }
    var scrubsToday  by remember { mutableStateOf(0) }
    var loading      by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            // Scan AI apps
            apps = PermissionScanner.scanInstalledAiApps(context.packageManager)

            // Load today stats
            val midnight = midnightTs()
            val dao = ExposureDatabase.getInstance(context).exposureDao()
            blockedToday = dao.countBlockedToday(midnight)
            scrubsToday  = dao.countScrubsToday(midnight)
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SovereignVoid)
    ) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "AI STOP",
                    color      = SignalWhite,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    "SOVEREIGN AI GUARD",
                    color    = SovereignBlue,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
            // On-device indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .background(VerifiedTeal, RoundedCornerShape(3.dp))
                )
                Text(
                    "ON-DEVICE",
                    color      = VerifiedTeal,
                    fontSize   = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // ── Sovereign Mode card ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .background(Surface1, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "SOVEREIGN MODE",
                    color      = SovereignBlue,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "AI Stop is protecting your input",
                    color    = SubText,
                    fontSize = 10.sp
                )
            }
            Switch(
                checked = true,
                onCheckedChange = {},
                colors = SwitchDefaults.colors(
                    checkedThumbColor  = SignalWhite,
                    checkedTrackColor  = SovereignBlue
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Stats row ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                value = apps.count { it.trustScore < 40 }.toString(),
                label = "APPS AT RISK",
                color = ThreatRed,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = blockedToday.toString(),
                label = "BLOCKED TODAY",
                color = CautionAmber,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = scrubsToday.toString(),
                label = "SCRUBS DONE",
                color = VerifiedTeal,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── AI Apps list ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "AI APPS AUDIT",
                color      = SubText,
                fontSize   = 9.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp
            )
            Text(
                "${apps.size} found",
                color      = SubText,
                fontSize   = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        if (loading) {
            Box(
                Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SovereignBlue, strokeWidth = 2.dp)
            }
        } else if (apps.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No monitored AI apps found",
                    color    = SubText,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(apps) { app -> AppRow(app) }
            }
        }
    }
}

@Composable
private fun StatCard(
    value:    String,
    label:    String,
    color:    Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Surface1, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            color      = color,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            label,
            color      = SubText,
            fontSize   = 7.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun AppRow(app: PermissionScanner.AuditedApp) {
    val scoreColor = when {
        app.trustScore < 40 -> ThreatRed
        app.trustScore < 70 -> CautionAmber
        else                -> VerifiedTeal
    }
    val scoreLabel = when {
        app.trustScore < 40 -> "Low Trust"
        app.trustScore < 70 -> "Caution"
        else                -> "Trusted"
    }
    val borderColor = when {
        app.trustScore < 40 -> ThreatRed.copy(alpha = 0.25f)
        app.trustScore < 70 -> CautionAmber.copy(alpha = 0.2f)
        else                -> Surface2
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // App icon placeholder
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Surface2, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                app.label.take(1),
                color      = SovereignBlue,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // App info
        Column(Modifier.weight(1f)) {
            Text(
                app.label,
                color      = SignalWhite,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                app.packageName,
                color      = SubText,
                fontSize   = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Trust score
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${app.trustScore}",
                color      = scoreColor,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                scoreLabel,
                color      = scoreColor,
                fontSize   = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Text("›", color = SubText, fontSize = 14.sp)
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
