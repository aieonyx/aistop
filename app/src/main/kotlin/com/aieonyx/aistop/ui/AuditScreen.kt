// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aieonyx.aistop.R
import com.aieonyx.aistop.core.PermissionScanner
import com.aieonyx.aistop.db.ExposureDatabase
import kotlinx.coroutines.launch

private val Void    = Color(0xFF080A0D)
private val Blue    = Color(0xFF4F80D4)
private val Red     = Color(0xFFE45F65)
private val Amber   = Color(0xFFD7A84B)
private val Teal    = Color(0xFF3EB69F)
private val White   = Color(0xFFEDF3FA)
private val Sub     = Color(0x8CEDF3FA)
private val Surf1   = Color(0x0AEDF3FA)
private val Surf2   = Color(0x14EDF3FA)
private val Divider = Color(0x1AEDF3FA)

// AI app brand colors matching ChatGPT design
private val appColors = mapOf(
    "com.openai.chatgpt"            to Color(0xFF10A37F),
    "com.google.android.apps.bard"  to Color(0xFF1A1A2E),
    "com.microsoft.copilot"         to Color(0xFF1A1A2E),
    "com.anthropic.claude"          to Color(0xFFCC785C),
    "com.grammarly.android"         to Color(0xFF15C39A),
    "com.notion.id"                 to Color(0xFF1A1A2E),
    "com.perplexity.app"            to Color(0xFF1A1A2E),
    "ai.perplexity.app"             to Color(0xFF1A1A2E),
)

@Composable
fun AuditScreen() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var apps         by remember { mutableStateOf<List<PermissionScanner.AuditedApp>>(emptyList()) }
    var blockedToday by remember { mutableStateOf(0) }
    var scrubsToday  by remember { mutableStateOf(0) }
    var loading      by remember { mutableStateOf(true) }
    var selectedApp  by remember { mutableStateOf<PermissionScanner.AuditedApp?>(null) }
    var sovereignMode by remember { mutableStateOf(true) }

    // Check if AI Stop IME is actually enabled
    LaunchedEffect(Unit) {
        val imeManager = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
            as android.view.inputmethod.InputMethodManager
        val enabledMethods = imeManager.enabledInputMethodList
        sovereignMode = enabledMethods.any { it.packageName == "com.aieonyx.aistop" }
    }

    // Show detail screen if app selected
    selectedApp?.let { app ->
        AppDetailScreen(
            packageName = app.packageName,
            appLabel    = app.label,
            trustScore  = app.trustScore,
            onBack      = { selectedApp = null }
        )
        return@AuditScreen
    }

    LaunchedEffect(Unit) {
        scope.launch {
            apps = PermissionScanner.scanInstalledAiApps(context.packageManager)
                .sortedBy { it.trustScore }
            val midnight = midnightTs()
            val dao = ExposureDatabase.getInstance(context).exposureDao()
            blockedToday = dao.countBlockedToday(midnight)
            scrubsToday  = dao.countScrubsToday(midnight)
            loading = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Void),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // ── Top bar ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "AI STOP",
                        color      = White,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "SOVEREIGN AI GUARD",
                        color      = Blue,
                        fontSize   = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        Modifier.size(6.dp)
                            .background(Teal, RoundedCornerShape(3.dp))
                    )
                    Text(
                        "ON-DEVICE",
                        color = Teal, fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // ── Sovereign Mode card ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .background(Surf1, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Shield icon with gate mark
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Blue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter            = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        colorFilter        = androidx.compose.ui.graphics.ColorFilter.tint(Blue),
                        modifier           = Modifier.size(26.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "SOVEREIGN MODE",
                        color      = White,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        "AI Stop is protecting your input",
                        color    = Sub,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked        = true,
                    onCheckedChange = {},
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = White,
                        checkedTrackColor = Blue
                    )
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        // ── Stats card — single wide card ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .background(Surf1, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = apps.count { it.trustScore < 40 }.toString(),
                    label = "Apps at risk",
                    color = Red
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .background(Divider)
                )
                StatItem(
                    value = blockedToday.toString(),
                    label = "Paste events blocked",
                    color = Amber
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .background(Divider)
                )
                StatItem(
                    value = scrubsToday.toString(),
                    label = "Scrubs done today",
                    color = Teal
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        // ── AI Apps Audit section ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "AI APPS AUDIT",
                    color    = Sub,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "${apps.size} found",
                    color    = Sub,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // ── App list ──
        if (loading) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Blue, strokeWidth = 2.dp)
                }
            }
        } else if (apps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .background(Surf1, RoundedCornerShape(14.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No monitored AI apps found",
                            color      = Sub,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Install any monitored AI app (ChatGPT, Gemini, Claude, Copilot, Grammarly, Perplexity, Notion, Character.AI, Poe) and it will appear here.",
                            color    = Sub.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Apps in a single card with dividers
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .background(Surf1, RoundedCornerShape(14.dp))
                ) {
                    apps.forEachIndexed { index, app ->
                        AppRow(app, onClick = { selectedApp = app })
                        if (index < apps.size - 1) {
                            HorizontalDivider(
                                color     = Divider,
                                thickness = 1.dp,
                                modifier  = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Last audit line ──
        item {
            Text(
                "Last audit: Just now  ·  Re-audit",
                color    = Sub.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color      = color,
            fontSize   = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            lineHeight = 36.sp
        )
        Text(
            label,
            color      = Sub,
            fontSize   = 11.sp,
            textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun AppRow(app: PermissionScanner.AuditedApp, onClick: () -> Unit = {}) {
    val scoreColor = when {
        app.trustScore < 40 -> Red
        app.trustScore < 70 -> Amber
        else                -> Teal
    }
    val scoreLabel = when {
        app.trustScore < 40 -> "High Risk"
        app.trustScore < 70 -> "Caution"
        else                -> "Trusted"
    }
    val bgColor = appColors[app.packageName] ?: Color(0xFF1A1A2E)
    val initial = app.label.take(1).uppercase()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App icon — colored rounded square with initial
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initial,
                color      = White,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // App name + package
        Column(Modifier.weight(1f)) {
            Text(
                app.label,
                color      = White,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Score + label
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${app.trustScore}",
                color      = scoreColor,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                scoreLabel,
                color      = scoreColor,
                fontSize   = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Text("›", color = Sub, fontSize = 18.sp)
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
