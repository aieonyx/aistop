// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
package com.aieonyx.aistop.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.aieonyx.aistop.ui.theme.AiStopTheme
import java.util.Locale

private fun getProtectionStatus(context: Context): Triple<Boolean, Boolean, Boolean> {
    val guardActive = try {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        enabled.contains(context.packageName, ignoreCase = true)
    } catch (e: Exception) { false }

    val keyboardActive = try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.enabledInputMethodList.any { it.packageName == context.packageName }
    } catch (e: Exception) { false }

    return Triple(guardActive, keyboardActive, true)
}

@Composable
fun ProtectScreen(
    darkMode: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    val context        = LocalContext.current
    val colors         = AiStopTheme.colors
    val typo           = AiStopTheme.typography
    var showDisclosure by remember { mutableStateOf(false) }
    var showCoverage   by remember { mutableStateOf(false) }
    var showModePicker by remember { mutableStateOf(false) }
    var currentMode    by remember { mutableStateOf(loadSovereignMode(context)) }

    val (guardActive, keyboardActive, scrubActive) = remember {
        getProtectionStatus(context)
    }

    val protectionLevel = when {
        guardActive && keyboardActive -> 2 // FULL
        guardActive || keyboardActive -> 1 // PARTIAL
        else                          -> 0 // NONE
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
    if (showModePicker) {
        ModePickerSheet(
            currentMode = currentMode,
            onSelect    = { mode: SovereignMode ->
                currentMode = mode
                saveSovereignMode(context, mode)
                showModePicker = false
            },
            onDismiss = { showModePicker = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(20.dp))

        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "AI STOP",
                    style = typo.h1,
                    color = colors.textPrimary
                )
                Text(
                    "SOVEREIGN AI GUARD",
                    style = typo.labelSmall,
                    color = colors.accentSecondary
                )
            }
            // Theme toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.outline, RoundedCornerShape(6.dp))
                    .clickable { onToggleTheme() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    if (darkMode) "☀ LIGHT" else "☾ DARK",
                    style = typo.labelSmall,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Protection Status Card ──
        val statusBg: Color
        val statusBorder: Color
        val statusLabel: String
        val statusDetail: String
        when (protectionLevel) {
            2    -> { statusBg = colors.successContainer; statusBorder = colors.success;  statusLabel = "FULLY PROTECTED";    statusDetail = "All interception layers active" }
            1    -> { statusBg = colors.warningContainer; statusBorder = colors.warning;  statusLabel = "PARTIAL PROTECTION"; statusDetail = "Enable all tools for full coverage" }
            else -> { statusBg = colors.dangerContainer;  statusBorder = colors.danger;   statusLabel = "NOT PROTECTED";      statusDetail = "Tap START PROTECTING below" }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(statusBg)
                .border(2.dp, statusBorder, RoundedCornerShape(14.dp))
        ) {
            // Top status rail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(statusBorder)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "🛡",
                        fontSize = 28.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            statusLabel,
                            style = typo.h2,
                            color = colors.textPrimary
                        )
                        Text(
                            statusDetail,
                            style = typo.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    // Mode badge
                    val modeColor = when (currentMode) {
                        SovereignMode.AUTOPILOT -> colors.success
                        SovereignMode.DEFAULT   -> colors.accentPrimary
                        SovereignMode.MANUAL    -> Color(0xFFA78BFA)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(modeColor.copy(alpha = 0.15f))
                            .border(1.dp, modeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            currentMode.badge,
                            style = typo.labelSmall,
                            color = modeColor
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Layer pills
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LayerPill("GUARD",    guardActive,    colors)
                    LayerPill("KEYBOARD", keyboardActive, colors)
                    LayerPill("SCRUB",    scrubActive,    colors)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Mode card ──
        val modeAccent = when (currentMode) {
            SovereignMode.AUTOPILOT -> colors.success
            SovereignMode.DEFAULT   -> colors.accentPrimary
            SovereignMode.MANUAL    -> Color(0xFFA78BFA)
        }
        val modeIcon = when (currentMode) {
            SovereignMode.AUTOPILOT -> "✈"
            SovereignMode.DEFAULT   -> "◉"
            SovereignMode.MANUAL    -> "⚙"
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(modeAccent.copy(alpha = 0.08f))
                .border(2.dp, modeAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .clickable { showModePicker = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(modeIcon, fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    currentMode.displayName.uppercase(),
                    style = typo.label,
                    color = modeAccent
                )
                Text(
                    currentMode.tagline,
                    style = typo.caption,
                    color = colors.textSecondary
                )
            }
            Text(
                "CHANGE →",
                style = typo.labelSmall,
                color = modeAccent
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── CTAs ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Primary CTA
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (guardActive) colors.success else colors.accentPrimary
                    )
                    .clickable { showDisclosure = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (guardActive) "GUARD ACTIVE ✓" else "START PROTECTING",
                    style = typo.label,
                    color = colors.onPrimary
                )
            }

            // Secondary CTA
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface2)
                    .border(2.dp, colors.accentPrimary, RoundedCornerShape(8.dp))
                    .clickable { showCoverage = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "RISK SCORE",
                    style = typo.label,
                    color = colors.textPrimary
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Section: Protection Tools ──
        SectionHeader("ACTIVE DEFENSE", colors, typo)

        listOf(
            Triple("🛡", "SOVEREIGN GUARD",
                if (guardActive) "Monitoring AI app paste events"
                else "Tap to enable in Accessibility Settings"),
            Triple("⌨", "AI STOP KEYBOARD",
                if (keyboardActive) "Type-time interception enabled"
                else "Enable in Settings → Language & Input"),
            Triple("✂", "SCRUBSHARE",
                "Share any text to AI Stop to scrub PII"),
            Triple("🖼", "IMAGE SCRUB",
                "Strip GPS, camera, serial numbers from photos")
        ).forEachIndexed { i, (icon, label, detail) ->
            val isOn = when (i) {
                0 -> guardActive
                1 -> keyboardActive
                else -> true
            }
            val needsAction = when (i) {
                0 -> !guardActive
                1 -> !keyboardActive
                else -> false
            }
            NewToolCard(
                icon        = icon,
                label       = label,
                detail      = detail,
                isOn        = isOn,
                needsAction = needsAction,
                colors      = colors,
                typo        = typo,
                onClick     = when (i) {
                    0 -> ({ showDisclosure = true })
                    1 -> ({ context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) })
                    else -> null
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(20.dp))

        // ── Detection chips ──
        SectionHeader("WHAT'S DETECTED", colors, typo)
        Spacer(Modifier.height(8.dp))

        androidx.compose.foundation.layout.FlowRow(
            modifier              = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement   = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "API KEYS", "PASSWORDS", "SSN", "PASSPORT",
                "IBAN", "CRYPTO", "HEALTH DATA",
                "GPS COORDS", "EXIF DATA", "JWTS", "PEM KEYS"
            ).forEach { item ->
                Box(
                    modifier = Modifier
                        .clip(CutCornerShape(bottomEnd = 6.dp))
                        .background(colors.surface2)
                        .border(1.dp, colors.outline, CutCornerShape(bottomEnd = 6.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        item,
                        style = typo.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "ALL ANALYSIS RUNS ON-DEVICE · NOTHING LEAVES YOUR PHONE",
            style     = typo.caption,
            color     = colors.textSecondary.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LayerPill(label: String, active: Boolean, colors: com.aieonyx.aistop.ui.theme.AiStopColors) {
    val typo  = AiStopTheme.typography
    val color = if (active) colors.success else colors.disabled
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            "${if (active) "✓" else "○"} $label",
            style = typo.labelSmall,
            color = color
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    colors: com.aieonyx.aistop.ui.theme.AiStopColors,
    typo: com.aieonyx.aistop.ui.theme.AiStopTypography
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
private fun NewToolCard(
    icon:        String,
    label:       String,
    detail:      String,
    isOn:        Boolean,
    needsAction: Boolean,
    colors:      com.aieonyx.aistop.ui.theme.AiStopColors,
    typo:        com.aieonyx.aistop.ui.theme.AiStopTypography,
    onClick:     (() -> Unit)?
) {
    val badgeColor = when {
        needsAction -> colors.accentSecondary
        isOn        -> colors.success
        else        -> colors.disabled
    }
    val badgeLabel = when {
        needsAction -> "ACTION"
        isOn        -> "ON"
        else        -> "OFF"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 24.sp, modifier = Modifier.width(38.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = typo.label, color = colors.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(detail, style = typo.caption, color = colors.textSecondary)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeColor)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                badgeLabel,
                style = typo.labelSmall,
                color = colors.onSignal
            )
        }
    }
}

// ── Mode Picker Sheet ─────────────────────────────────────────────────────────

@Composable
fun ModePickerSheet(
    currentMode: SovereignMode,
    onSelect:    (SovereignMode) -> Unit,
    onDismiss:   () -> Unit
) {
    val colors = AiStopTheme.colors
    val typo   = AiStopTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "CHOOSE YOUR MODE",
                style    = typo.h2,
                color    = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surface2)
                    .clickable { onDismiss() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("CANCEL", style = typo.labelSmall, color = colors.textSecondary)
            }
        }

        Text(
            "You can change this at any time.",
            style    = typo.bodySmall,
            color    = colors.textSecondary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
        )

        SovereignMode.entries.forEach { mode ->
            val accent = when (mode) {
                SovereignMode.AUTOPILOT -> colors.success
                SovereignMode.DEFAULT   -> colors.accentPrimary
                SovereignMode.MANUAL    -> androidx.compose.ui.graphics.Color(0xFFA78BFA)
            }
            val icon = when (mode) {
                SovereignMode.AUTOPILOT -> "✈"
                SovereignMode.DEFAULT   -> "◉"
                SovereignMode.MANUAL    -> "⚙"
            }
            val selected = currentMode == mode

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) accent.copy(alpha = 0.09f)
                        else colors.surface
                    )
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) accent else colors.outline,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(mode) }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 22.sp, modifier = Modifier.width(36.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            mode.displayName.uppercase(),
                            style      = typo.label,
                            color      = if (selected) colors.textPrimary else colors.textSecondary
                        )
                        Text(
                            mode.tagline,
                            style = typo.caption,
                            color = accent
                        )
                    }
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", style = typo.caption, color = colors.onSignal)
                        }
                    }
                }
                if (selected) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        mode.detail,
                        style = typo.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}
