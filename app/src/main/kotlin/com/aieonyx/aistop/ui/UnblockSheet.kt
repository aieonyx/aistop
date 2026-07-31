// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import com.aieonyx.aistop.security.BiometricGate
import com.aieonyx.aistop.ui.theme.AiStopTheme
import com.aieonyx.aistop.vpn.AllowlistManager

/**
 * UnblockSheet — bottom sheet shown when user taps a blocked domain in ShieldScreen.
 * Shows domain info, risk warning, duration picker, biometric gate.
 */
@Composable
fun UnblockSheet(
    hostname: String,
    category: String,
    onDismiss: () -> Unit,
    onAllowed: (AllowlistManager.Duration) -> Unit
) {
    val context = LocalContext.current
    val colors  = AiStopTheme.colors
    val typo    = AiStopTheme.typography
    val amber   = Color(0xFFFBBF24)
    val red     = Color(0xFFF87171)
    val purple  = Color(0xFFA78BFA)

    var selectedDuration by remember { mutableStateOf<AllowlistManager.Duration?>(null) }
    var confirming       by remember { mutableStateOf(false) }

    // Risk copy per category
    val riskText = when (category) {
        "CRAWLER"   -> "This domain actively crawls and collects web content for AI training. " +
                       "Allowing it means your DNS queries and any data sent will be visible to their servers."
        "TELEMETRY" -> "This domain collects behavioral and usage data that feeds AI model training. " +
                       "Unblocking it may expose your app usage patterns."
        "API"       -> "This is an AI model inference API. Requests sent here may be logged and " +
                       "used to improve the AI model."
        else        -> "This domain has been flagged for AI data collection or privacy concerns."
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.background)
                .border(1.dp, colors.outline, RoundedCornerShape(16.dp))
        ) {
            // ── Handle bar ──
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.outline)
            )

            Spacer(Modifier.height(16.dp))

            // ── Domain + category ──
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⛔", fontSize = 20.sp, modifier = Modifier.padding(end = 10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(hostname, style = typo.label, color = colors.textPrimary)
                        Text("Blocked by Sovereign Shield", style = typo.caption, color = colors.textSecondary)
                    }
                    val badgeColor = when (category) {
                        "CRAWLER"   -> red
                        "TELEMETRY" -> amber
                        else        -> purple
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(0.5.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(category, style = typo.labelSmall, color = badgeColor, fontSize = 9.sp)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Risk warning ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(amber.copy(alpha = 0.08f))
                        .border(1.dp, amber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("⚠ CAUTION", style = typo.labelSmall, color = amber)
                        Spacer(Modifier.height(4.dp))
                        Text(riskText, style = typo.caption, color = colors.textSecondary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "AIEONYX cannot protect you while this domain is unblocked.",
                            style = typo.caption,
                            color = amber
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Duration picker ──
                Text("ALLOW FOR HOW LONG?", style = typo.labelSmall, color = colors.textSecondary)
                Spacer(Modifier.height(8.dp))

                // 2×3 grid of duration options
                val durations = AllowlistManager.Duration.entries
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    durations.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { dur ->
                                val selected = selectedDuration == dur
                                val accent   = when (dur) {
                                    AllowlistManager.Duration.PERMANENT -> red
                                    AllowlistManager.Duration.SESSION   -> amber
                                    else                                -> colors.accentPrimary
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) accent.copy(alpha = 0.15f)
                                            else colors.surface
                                        )
                                        .border(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) accent else colors.outline,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedDuration = dur }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        dur.label,
                                        style = typo.caption,
                                        color = if (selected) accent else colors.textSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Action buttons ──
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Cancel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("CANCEL", style = typo.label, color = colors.textSecondary)
                    }

                    // Unblock
                    val canUnblock = selectedDuration != null
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (canUnblock) amber else colors.disabled)
                            .clickable(enabled = canUnblock) {
                                if (!confirming) {
                                    confirming = true
                                    val activity = context as? FragmentActivity
                                    if (activity != null) {
                                        BiometricGate.authenticate(
                                            activity    = activity,
                                            actionTitle = "Unblock $hostname",
                                            subtitle    = "Verify identity to temporarily allow this domain"
                                        ) { result ->
                                            when (result) {
                                                is BiometricGate.AuthResult.Success,
                                                is BiometricGate.AuthResult.NoHardware -> {
                                                    onAllowed(selectedDuration!!)
                                                }
                                                else -> { confirming = false }
                                            }
                                        }
                                    } else {
                                        onAllowed(selectedDuration!!)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (confirming) "VERIFYING…" else "UNBLOCK ⚠",
                            style = typo.label,
                            color = if (canUnblock) Color.Black else colors.textSecondary
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
