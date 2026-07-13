// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PVoid  = Color(0xFF080A0D)
private val PBlue  = Color(0xFF4F80D4)
private val PTeal  = Color(0xFF3EB69F)
private val PAmber = Color(0xFFF5A623)
private val PPurp  = Color(0xFFA78BFA)
private val PRed   = Color(0xFFD05050)
private val PWhite = Color(0xFFEDF3FA)
private val PSub   = Color(0x8CEDF3FA)
private val PSurf1 = Color(0x0AEDF3FA)
private val PSurf2 = Color(0x14EDF3FA)

private enum class ProtectionLevel { FULL, PARTIAL, OFF }

private data class ProtectionStatus(
    val level:          ProtectionLevel,
    val guardActive:    Boolean,
    val keyboardActive: Boolean,
    val scrubActive:    Boolean
)

private fun getProtectionStatus(context: Context): ProtectionStatus {
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

    val level = when {
        guardActive && keyboardActive -> ProtectionLevel.FULL
        guardActive || keyboardActive -> ProtectionLevel.PARTIAL
        else                          -> ProtectionLevel.OFF
    }
    return ProtectionStatus(level, guardActive, keyboardActive, scrubActive = true)
}

private fun modeAccentColor(mode: SovereignMode): Color = when (mode) {
    SovereignMode.AUTOPILOT -> PTeal
    SovereignMode.DEFAULT   -> PBlue
    SovereignMode.MANUAL    -> PPurp
}

@Composable
fun ProtectScreen() {
    val context        = LocalContext.current
    var showDisclosure by remember { mutableStateOf(false) }
    var showCoverage   by remember { mutableStateOf(false) }
    var showModePicker by remember { mutableStateOf(false) }
    var currentMode    by remember { mutableStateOf(loadSovereignMode(context)) }
    val status         = remember { getProtectionStatus(context) }

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
            onSelect    = { mode ->
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
            .background(PVoid)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("AI Stop", color = PWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Sovereign AI Guard",
                    color = PSub, fontSize = 11.sp, fontFamily = FontFamily.Monospace
                )
            }
            Box(
                modifier = Modifier
                    .background(PSurf1, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("v1.0", color = PSub, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Protection status bar (mode-aware) ──
        ProtectionStatusBar(status, currentMode)

        Spacer(Modifier.height(12.dp))

        // ── Mode card (tappable) ──
        ModeCard(currentMode, onClick = { showModePicker = true })

        Spacer(Modifier.height(16.dp))

        // ── Primary CTAs ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showDisclosure = true },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (status.guardActive) PTeal else PBlue
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (status.guardActive) "Guard active ✓" else "Start protecting",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PWhite
                )
            }
            OutlinedButton(
                onClick = { showCoverage = true },
                modifier = Modifier.weight(1f).height(52.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PSurf2),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PSub)
            ) {
                Text(
                    "See my risk score",
                    fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PWhite
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Protection tools ──
        SectionLabel("PROTECTION TOOLS")

        ToolCard(
            icon   = "🛡",
            label  = "Sovereign Guard",
            detail = if (status.guardActive) "Active — monitoring AI app paste events"
                     else "Tap to enable in Accessibility Settings",
            status = if (status.guardActive) ToolStatus.ON else ToolStatus.ACTION,
            onClick = { showDisclosure = true }
        )
        ToolCard(
            icon   = "⌨",
            label  = "AI Stop Keyboard",
            detail = if (status.keyboardActive) "Active — type-time interception enabled"
                     else "Enable in Settings → System → Language & Input",
            status = if (status.keyboardActive) ToolStatus.ON else ToolStatus.ACTION,
            onClick = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        )
        ToolCard(
            icon    = "✂",
            label   = "ScrubShare",
            detail  = "Share any text to AI Stop to scrub PII before sending",
            status  = ToolStatus.ON,
            onClick = null
        )
        ToolCard(
            icon    = "🖼",
            label   = "Image Scrub",
            detail  = "Strip GPS, camera metadata, and serial numbers from photos",
            status  = ToolStatus.ON,
            onClick = null
        )

        Spacer(Modifier.height(16.dp))

        SectionLabel("WHAT'S DETECTED")
        DetectionChipRow(
            listOf(
                "API keys", "Passwords", "SSN", "Passport",
                "IBAN", "Crypto wallets", "Health data",
                "GPS coords", "EXIF data", "JWTs", "PEM keys"
            )
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "All analysis runs on-device. Nothing leaves your phone.",
            color     = PSub.copy(alpha = 0.5f),
            fontSize  = 10.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(24.dp))
    }
}

// ── Status bar ──

@Composable
private fun ProtectionStatusBar(status: ProtectionStatus, mode: SovereignMode) {
    val (barColor, barLabel, barDetail) = when (status.level) {
        ProtectionLevel.FULL    -> Triple(PTeal,  "FULLY PROTECTED",     "All interception layers active")
        ProtectionLevel.PARTIAL -> Triple(PAmber, "PARTIALLY PROTECTED", "Enable all tools for full coverage")
        ProtectionLevel.OFF     -> Triple(PRed,   "NOT PROTECTED",       "Tap 'Start protecting' below")
    }
    val modeColor = modeAccentColor(mode)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(barColor.copy(alpha = 0.09f), RoundedCornerShape(16.dp))
            .border(1.dp, barColor.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(barColor, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                barLabel,
                color         = barColor,
                fontSize      = 12.sp,
                fontWeight    = FontWeight.Bold,
                fontFamily    = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                modifier      = Modifier.weight(1f)
            )
            // Mode badge
            Box(
                modifier = Modifier
                    .background(modeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .border(1.dp, modeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    mode.badge,
                    color      = modeColor,
                    fontSize   = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(barDetail, color = PSub, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LayerPill("Guard",    status.guardActive)
            LayerPill("Keyboard", status.keyboardActive)
            LayerPill("Scrub",    status.scrubActive)
        }
    }
}

@Composable
private fun LayerPill(label: String, active: Boolean) {
    val color = if (active) PTeal else PSub.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            "${if (active) "✓" else "○"} $label",
            color = color, fontSize = 9.sp, fontFamily = FontFamily.Monospace
        )
    }
}

// ── Mode card ──

@Composable
private fun ModeCard(mode: SovereignMode, onClick: () -> Unit) {
    val accent = modeAccentColor(mode)
    val icon   = when (mode) {
        SovereignMode.AUTOPILOT -> "✈"
        SovereignMode.DEFAULT   -> "◉"
        SovereignMode.MANUAL    -> "⚙"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(accent.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp, modifier = Modifier.width(32.dp))
        Column(Modifier.weight(1f)) {
            Text(
                mode.displayName,
                color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
            Text(
                mode.tagline,
                color = PSub, fontSize = 11.sp, fontFamily = FontFamily.Monospace
            )
        }
        Text(
            "Change →",
            color = accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace
        )
    }
}

// ── Mode picker (full-screen) ──

@Composable
fun ModePickerSheet(
    currentMode: SovereignMode,
    onSelect:    (SovereignMode) -> Unit,
    onDismiss:   () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PVoid)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Choose your mode",
                color = PWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PSub)
            }
        }
        Text(
            "You can change this at any time.",
            color = PSub, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        Spacer(Modifier.height(20.dp))

        SovereignMode.entries.forEach { mode ->
            val accent   = modeAccentColor(mode)
            val icon     = when (mode) {
                SovereignMode.AUTOPILOT -> "✈"
                SovereignMode.DEFAULT   -> "◉"
                SovereignMode.MANUAL    -> "⚙"
            }
            val selected = currentMode == mode

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(
                        if (selected) accent.copy(alpha = 0.09f) else PSurf1,
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) accent else PSurf2,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelect(mode) }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 22.sp, modifier = Modifier.width(36.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            mode.displayName,
                            color      = if (selected) PWhite else PSub,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            mode.tagline,
                            color      = accent,
                            fontSize   = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = PWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    mode.detail,
                    color = PSub, fontSize = 12.sp, lineHeight = 17.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Tool card ──

private enum class ToolStatus { ON, ACTION, OFF }

@Composable
private fun ToolCard(
    icon:    String,
    label:   String,
    detail:  String,
    status:  ToolStatus,
    onClick: (() -> Unit)?
) {
    val statusColor = when (status) {
        ToolStatus.ON     -> PTeal
        ToolStatus.ACTION -> PAmber
        ToolStatus.OFF    -> PSub.copy(alpha = 0.4f)
    }
    val statusLabel = when (status) {
        ToolStatus.ON     -> "ON"
        ToolStatus.ACTION -> "ACTION"
        ToolStatus.OFF    -> "OFF"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(PSurf1, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 22.sp, modifier = Modifier.width(36.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = PWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(detail, color = PSub, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(
                statusLabel,
                color = statusColor, fontSize = 8.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Helpers ──

@Composable
private fun SectionLabel(label: String) {
    Text(
        label,
        color = PSub, fontSize = 10.sp,
        fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetectionChipRow(items: List<String>) {
    FlowRow(
        modifier              = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement   = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .background(PSurf1, RoundedCornerShape(8.dp))
                    .border(1.dp, PSurf2, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(item, color = PSub, fontSize = 11.sp)
            }
        }
    }
}
