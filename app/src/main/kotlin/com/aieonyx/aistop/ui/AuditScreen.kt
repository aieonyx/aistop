// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aieonyx.aistop.R
import com.aieonyx.aistop.core.PermissionScanner
import com.aieonyx.aistop.db.EdisonExposureDatabase as ExposureDatabase
import com.aieonyx.aistop.ui.theme.AiStopTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val appColors = mapOf(
    "com.openai.chatgpt"           to Color(0xFF10A37F),
    "com.google.android.apps.bard" to Color(0xFF1A1A2E),
    "com.microsoft.copilot"        to Color(0xFF1A1A2E),
    "com.anthropic.claude"         to Color(0xFFCC785C),
    "com.grammarly.android"        to Color(0xFF15C39A),
    "com.notion.id"                to Color(0xFF1A1A2E),
    "com.perplexity.app"           to Color(0xFF1A1A2E),
    "ai.perplexity.app"            to Color(0xFF1A1A2E),
)

@Composable
fun AuditScreen() {
    val context = LocalContext.current
    val colors  = AiStopTheme.colors
    val typo    = AiStopTheme.typography

    var apps          by remember { mutableStateOf<List<PermissionScanner.AuditedApp>>(emptyList()) }
    var blockedToday  by remember { mutableStateOf(0) }
    var scrubsToday   by remember { mutableStateOf(0) }
    var loading       by remember { mutableStateOf(true) }
    var selectedApp   by remember { mutableStateOf<PermissionScanner.AuditedApp?>(null) }
    var sovereignMode by remember { mutableStateOf(false) }

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
        withContext(Dispatchers.IO) {
            val imeActive = try {
                val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                    as android.view.inputmethod.InputMethodManager
                imm.enabledInputMethodList.any { it.packageName == context.packageName }
            } catch (e: Exception) { false }

            val scanned = try {
                PermissionScanner.scanInstalledAiApps(context.packageManager)
                    .sortedBy { it.trustScore }
            } catch (e: Exception) { emptyList() }

            val midnight = midnightTs()
            val (blocked, scrubs) = try {
                val dao = ExposureDatabase.getInstance(context).exposureDao()
                Pair(dao.countBlockedToday(midnight), dao.countScrubsToday(midnight))
            } catch (e: Exception) { Pair(0, 0) }

            withContext(Dispatchers.Main) {
                sovereignMode = imeActive
                apps          = scanned
                blockedToday  = blocked
                scrubsToday   = scrubs
                loading       = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // ── Header ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("AI STOP", style = typo.h1, color = colors.textPrimary)
                    Text(
                        "SOVEREIGN AI GUARD",
                        style = typo.labelSmall,
                        color = colors.accentSecondary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(colors.success, RoundedCornerShape(4.dp))
                    )
                    Text("ON-DEVICE", style = typo.labelSmall, color = colors.success)
                }
            }
        }

        // ── Sovereign Mode card ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(colors.primaryContainer, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_nav_shield),
                        contentDescription = null,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colors.accentPrimary),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "SOVEREIGN MODE",
                        style = typo.label,
                        color = colors.textPrimary
                    )
                    Text(
                        if (sovereignMode) "AI Stop keyboard is active"
                        else "Enable AI Stop keyboard for full protection",
                        style = typo.caption,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked         = sovereignMode,
                    onCheckedChange = {},
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = colors.onPrimary,
                        checkedTrackColor   = colors.accentPrimary,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.surface2
                    )
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        // ── Stats card ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AuditStatItem(
                    value = apps.count { it.trustScore < 40 }.toString(),
                    label = "Apps\nat risk",
                    color = colors.danger
                )
                Box(Modifier.width(1.dp).height(44.dp).background(colors.divider))
                AuditStatItem(
                    value = blockedToday.toString(),
                    label = "Paste events\nblocked",
                    color = colors.warning
                )
                Box(Modifier.width(1.dp).height(44.dp).background(colors.divider))
                AuditStatItem(
                    value = scrubsToday.toString(),
                    label = "Scrubs done\ntoday",
                    color = colors.success
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        // ── Section header ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .background(colors.accentSecondary)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("AI APPS AUDIT", style = typo.label, color = colors.textPrimary)
                }
                Text(
                    "${apps.size} FOUND",
                    style = typo.labelSmall,
                    color = colors.textSecondary
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
                    CircularProgressIndicator(
                        color       = colors.accentPrimary,
                        strokeWidth = 2.dp
                    )
                }
            }
        } else if (apps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "NO MONITORED AI APPS FOUND",
                            style     = typo.label,
                            color     = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Install ChatGPT, Gemini, Claude, Copilot, Grammarly, Perplexity, Notion, or Poe and it will appear here.",
                            style     = typo.caption,
                            color     = colors.disabled,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
                ) {
                    apps.forEachIndexed { index, app ->
                        AuditAppRow(app, colors, typo, onClick = { selectedApp = app })
                        if (index < apps.size - 1) {
                            HorizontalDivider(
                                color     = colors.divider,
                                thickness = 1.dp,
                                modifier  = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                "LAST AUDIT: JUST NOW  ·  RE-AUDIT",
                style    = typo.caption,
                color    = colors.disabled,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun AuditStatItem(value: String, label: String, color: Color) {
    val typo = AiStopTheme.typography
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style      = AiStopTheme.typography.display,
            color      = color,
            lineHeight = 36.sp
        )
        Text(
            label,
            style     = AiStopTheme.typography.caption,
            color     = AiStopTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AuditAppRow(
    app:     PermissionScanner.AuditedApp,
    colors:  com.aieonyx.aistop.ui.theme.AiStopColors,
    typo:    com.aieonyx.aistop.ui.theme.AiStopTypography,
    onClick: () -> Unit
) {
    val scoreColor = when {
        app.trustScore < 40 -> colors.danger
        app.trustScore < 70 -> colors.warning
        else                -> colors.success
    }
    val scoreLabel = when {
        app.trustScore < 40 -> "HIGH RISK"
        app.trustScore < 70 -> "CAUTION"
        else                -> "TRUSTED"
    }
    val context = LocalContext.current
    val bgColor = appColors[app.packageName] ?: Color(0xFF1A1A2E)

    // Load real installed app icon
    var appIcon by remember(app.packageName) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    LaunchedEffect(app.packageName) {
        withContext(Dispatchers.IO) {
            appIcon = try {
                val drawable = context.packageManager.getApplicationIcon(app.packageName)
                android.graphics.Bitmap.createBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888).also { bmp ->
                    val canvas = android.graphics.Canvas(bmp)
                    drawable.setBounds(0, 0, 96, 96)
                    drawable.draw(canvas)
                }
            } catch (e: Exception) { null }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (appIcon != null) Color.Transparent else bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (appIcon != null) {
                androidx.compose.foundation.Image(
                    bitmap             = appIcon!!.asImageBitmap(),
                    contentDescription = app.label,
                    modifier           = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    app.label.take(1).uppercase(),
                    style = typo.h3,
                    color = Color.White
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(app.label, style = typo.label, color = colors.textPrimary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${app.trustScore}",
                style = typo.h2,
                color = scoreColor
            )
            Text(scoreLabel, style = typo.labelSmall, color = scoreColor)
        }
        Text("›", style = typo.h2, color = colors.textSecondary)
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
