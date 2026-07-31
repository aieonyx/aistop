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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aieonyx.aistop.ui.theme.AiStopTheme
import com.aieonyx.aistop.vpn.AppBlockList
import com.aieonyx.aistop.vpn.VpnIntegration

@Composable
fun AppBlockListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val colors  = AiStopTheme.colors
    val typo    = AiStopTheme.typography
    val red     = Color(0xFFF87171)
    val amber   = Color(0xFFFBBF24)

    var searchQuery    by remember { mutableStateOf("") }
    var showPicker     by remember { mutableStateOf(false) }
    var blockedApps    by remember { mutableStateOf(AppBlockList.getBlockedApps(context)) }
    var installedApps  by remember { mutableStateOf(listOf<AppBlockList.BlockedApp>()) }
    val vpnRunning     = VpnIntegration.isRunning(context)

    LaunchedEffect(showPicker) {
        if (showPicker) {
            installedApps = AppBlockList.getInstalledApps(context)
        }
    }

    if (showPicker) {
        // App picker
        val filtered = installedApps.filter {
            searchQuery.isEmpty() ||
            it.label.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SELECT APP TO BLOCK",
                        style    = typo.h2,
                        color    = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surface2)
                            .clickable { showPicker = false; searchQuery = "" }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("CANCEL", style = typo.labelSmall, color = colors.textSecondary)
                    }
                }

                // Search bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle     = typo.bodySmall.copy(color = colors.textPrimary),
                        modifier      = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text("Search apps…", style = typo.bodySmall,
                                    color = colors.textSecondary)
                            }
                            inner()
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            items(filtered) { app ->
                val alreadyBlocked = AppBlockList.isBlocked(app.packageName)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surface)
                        .border(1.dp,
                            if (alreadyBlocked) red.copy(alpha = 0.3f) else colors.outline,
                            RoundedCornerShape(8.dp))
                        .clickable {
                            if (!alreadyBlocked) {
                                AppBlockList.block(context, app.packageName)
                                blockedApps = AppBlockList.getBlockedApps(context)
                                showPicker  = false
                                searchQuery = ""
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(app.label, style = typo.label, color = colors.textPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(app.packageName, style = typo.caption,
                            color = colors.textSecondary, fontSize = 9.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (alreadyBlocked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(red.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("BLOCKED", style = typo.labelSmall, color = red, fontSize = 9.sp)
                        }
                    } else {
                        Text("BLOCK →", style = typo.labelSmall, color = colors.accentPrimary)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
        return
    }

    // Main block list screen
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        item {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("APP BLOCK LIST", style = typo.h1, color = colors.textPrimary)
                    Text("NETWORK BLOCKED WHEN SHIELD IS ON",
                        style = typo.labelSmall, color = colors.accentSecondary)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surface2)
                        .clickable { onBack() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("← BACK", style = typo.labelSmall, color = colors.textSecondary)
                }
            }
            Spacer(Modifier.height(12.dp))

            // Info card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (vpnRunning) colors.successContainer
                        else colors.warningContainer
                    )
                    .border(1.dp,
                        if (vpnRunning) colors.success else colors.warning,
                        RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Text(
                    if (vpnRunning)
                        "🛡 Sovereign Shield is ON — blocked apps have no network access."
                    else
                        "⚠ Enable Sovereign Shield VPN for blocking to take effect.",
                    style = typo.caption,
                    color = colors.textPrimary
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // Blocked apps list
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(4.dp).height(20.dp).background(red))
                Spacer(Modifier.width(10.dp))
                Text("BLOCKED APPS (${blockedApps.size})",
                    style = typo.label, color = colors.textPrimary)
                Spacer(Modifier.width(10.dp))
                HorizontalDivider(color = colors.divider, thickness = 1.dp,
                    modifier = Modifier.weight(1f))
            }
        }

        if (blockedApps.isEmpty()) {
            item {
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
                    Text("No apps blocked yet. Tap ADD APP to block an app.",
                        style = typo.caption, color = colors.textSecondary)
                }
                Spacer(Modifier.height(12.dp))
            }
        } else {
            items(blockedApps) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(red.copy(alpha = 0.06f))
                        .border(1.dp, red.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(app.label, style = typo.label, color = colors.textPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(app.packageName, style = typo.caption,
                            color = colors.textSecondary, fontSize = 9.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.surface2)
                            .border(1.dp, red.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .clickable {
                                AppBlockList.unblock(context, app.packageName)
                                blockedApps = AppBlockList.getBlockedApps(context)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("UNBLOCK", style = typo.labelSmall, color = red, fontSize = 9.sp)
                    }
                }
            }
        }

        // Add app button
        item {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.accentPrimary)
                    .clickable { showPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Text("+ ADD APP", style = typo.label, color = colors.onPrimary)
            }
            Spacer(Modifier.height(16.dp))
        }

        // Restart note
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(amber.copy(alpha = 0.08f))
                    .border(1.dp, amber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    "⚠ Changes take effect after toggling Sovereign Shield OFF then ON.",
                    style = typo.caption,
                    color = colors.textSecondary
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
