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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aieonyx.aistop.R
import com.aieonyx.aistop.core.PermissionScanner
import com.aieonyx.aistop.db.EdisonExposureDatabase as ExposureDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Void     = Color(0xFF080A0D)
private val Blue     = Color(0xFF4F80D4)
private val Red      = Color(0xFFE45F65)
private val Amber    = Color(0xFFD7A84B)
private val Teal     = Color(0xFF3EB69F)
private val White    = Color(0xFFEDF3FA)
private val Sub      = Color(0x8CEDF3FA)
private val Surf1    = Color(0x0AEDF3FA)
private val DivColor = Color(0x1AEDF3FA)

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

    var apps         by remember { mutableStateOf<List<PermissionScanner.AuditedApp>>(emptyList()) }
    var blockedToday by remember { mutableStateOf(0) }
    var scrubsToday  by remember { mutableStateOf(0) }
    var loading      by remember { mutableStateOf(true) }
    var selectedApp  by remember { mutableStateOf<PermissionScanner.AuditedApp?>(null) }
    var sovereignMode by remember { mutableStateOf(false) }

    // Show detail if app selected
    selectedApp?.let { app ->
        AppDetailScreen(
            packageName = app.packageName,
            appLabel    = app.label,
            trustScore  = app.trustScore,
            onBack      = { selectedApp = null }
        )
        return@AuditScreen
    }

    // Single LaunchedEffect — no nested scope.launch
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // IME check — wrapped defensively for Samsung
            val imeActive = try {
                val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                    as android.view.inputmethod.InputMethodManager
                imm.enabledInputMethodList.any { it.packageName == context.packageName }
            } catch (e: Exception) { false }

            // Package scan — can throw SecurityException on Samsung One UI
            val scanned = try {
                PermissionScanner.scanInstalledAiApps(context.packageManager)
                    .sortedBy { it.trustScore }
            } catch (e: Exception) { emptyList() }

            // DB queries
            val midnight = midnightTs()
            val (blocked, scrubs) = try {
                val dao = ExposureDatabase.getInstance(context).exposureDao()
                Pair(
                    dao.countBlockedToday(midnight),
                    dao.countScrubsToday(midnight)
                )
            } catch (e: Exception) { Pair(0, 0) }

            // Back to main thread for state updates
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
                        color = White, fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    )
                    Text(
                        "SOVEREIGN AI GUARD",
                        color = Blue, fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(Modifier.size(6.dp).background(Teal, RoundedCornerShape(3.dp)))
                    Text(
                        "ON-DEVICE",
                        color = Teal, fontSize = 8.sp, fontFamily = FontFamily.Monospace
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
                        color = White, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp
                    )
                    Text(
                        if (sovereignMode) "AI Stop keyboard is active"
                        else "Enable AI Stop keyboard for full protection",
                        color = Sub, fontSize = 11.sp
                    )
                }
                Switch(
                    checked         = sovereignMode,
                    onCheckedChange = {},
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = White,
                        checkedTrackColor   = Blue,
                        uncheckedThumbColor = Sub,
                        uncheckedTrackColor = Surf1
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
                Box(Modifier.width(1.dp).height(44.dp).background(DivColor))
                StatItem(
                    value = blockedToday.toString(),
                    label = "Paste events\nblocked",
                    color = Amber
                )
                Box(Modifier.width(1.dp).height(44.dp).background(DivColor))
                StatItem(
                    value = scrubsToday.toString(),
                    label = "Scrubs done\ntoday",
                    color = Teal
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        // ── AI Apps section header ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "AI APPS AUDIT",
                    color = Sub, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp
                )
                Text(
                    "${apps.size} found",
                    color = Sub, fontSize = 11.sp, fontFamily = FontFamily.Monospace
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
                            color = Sub, fontSize = 14.sp, fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Install any monitored AI app (ChatGPT, Gemini, Claude, Copilot, Grammarly, Perplexity, Notion, Character.AI, Poe) and it will appear here.",
                            color = Sub.copy(alpha = 0.6f), fontSize = 11.sp,
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
                        .padding(horizontal = 14.dp)
                        .background(Surf1, RoundedCornerShape(14.dp))
                ) {
                    apps.forEachIndexed { index, app ->
                        AppRow(app, onClick = { selectedApp = app })
                        if (index < apps.size - 1) {
                            HorizontalDivider(
                                color     = DivColor,
                                thickness = 1.dp,
                                modifier  = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Footer ──
        item {
            Text(
                "Last audit: Just now  ·  Re-audit",
                color = Sub.copy(alpha = 0.5f), fontSize = 10.sp,
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
            color = color, fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, lineHeight = 36.sp
        )
        Text(
            label,
            color = Sub, fontSize = 11.sp,
            textAlign = TextAlign.Center, lineHeight = 14.sp
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
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(app.label, color = White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${app.trustScore}",
                color = scoreColor, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
            )
            Text(
                scoreLabel,
                color = scoreColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace
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
