// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import android.os.Bundle
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import com.aieonyx.aistop.R
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SovereignVoid = Color(0xFF080A0D)
private val SovereignBlue = Color(0xFF4F80D4)
private val SubText       = Color(0x8CEDF3FA)
private val Surface1      = Color(0x0AEDF3FA)
private val SignalWhite   = Color(0xFFEDF3FA)

/**
 * MainActivity — full Dashboard with bottom navigation.
 * Phase 5: Dashboard, Scrub, Log, Settings tabs.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // FLAG_SECURE: prevent screenshots and screen recording of sensitive data
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        setContent {
            AiStopApp()
        }
    }
}

@Composable
fun AiStopApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showOnboarding by remember { mutableStateOf(!isOnboardingComplete(context)) }
    var selectedTab by remember { mutableStateOf(0) }

    if (showOnboarding) {
        OnboardingScreen(onComplete = { showOnboarding = false })
        return@AiStopApp
    }

    val tabs = listOf(
        Pair(R.drawable.ic_nav_dashboard, "Dashboard"),
        Pair(R.drawable.ic_nav_shield,    "Stats"),
        Pair(R.drawable.ic_nav_log,       "Log"),
        Pair(R.drawable.ic_nav_settings,  "Settings")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SovereignVoid)
    ) {
        // Content area
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AuditScreen()
                1 -> SovereignStatsScreen()
                2 -> LogScreen()
                3 -> SettingsScreen()
            }
        }

        // Bottom navigation
        Divider(color = Surface1, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SovereignVoid)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, (iconRes, label) ->
                val active = selectedTab == index
                Column(
                    modifier = Modifier
                        .clickable { selectedTab = index }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = iconRes),
                        contentDescription = label,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                            if (active) SovereignBlue else SubText
                        ),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        label,
                        fontSize   = 8.sp,
                        color      = if (active) SovereignBlue else SubText,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun ScrubTabScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SovereignVoid),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("\u2702", fontSize = 40.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Scrub & Share",
                color    = SignalWhite,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select text in any app",
                color    = SubText,
                fontSize = 13.sp
            )
            Text(
                "Share to Scrub with AI Stop",
                color    = SubText,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDisclosure by remember { mutableStateOf(false) }
    var showCoverage   by remember { mutableStateOf(false) }
    val packageInfo = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0) }
        catch (e: Exception) { null }
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

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SovereignVoid),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Settings",
                    color = SignalWhite, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Protection section
        item { SettingsSectionHeader("PROTECTION") }
        item {
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDisclosure = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text("Sovereign Guard", color = SignalWhite, fontSize = 13.sp,
                        modifier = Modifier.weight(0.45f))
                    Text("Tap to enable →", color = SovereignBlue, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        modifier = Modifier.weight(0.55f))
                }
                HorizontalDivider(color = Color(0x14EDF3FA))
                SettingsRow("AI Stop Keyboard", "IME-based paste interception", SovereignBlue)
                HorizontalDivider(color = Color(0x14EDF3FA))
                SettingsRow("ScrubShare", "Share sheet PII scrubber", SovereignBlue)
                HorizontalDivider(color = Color(0x14EDF3FA))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCoverage = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Coverage matrix", color = SignalWhite, fontSize = 13.sp,
                        modifier = Modifier.weight(0.45f))
                    Text("What AI Stop covers →", color = SovereignBlue, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        modifier = Modifier.weight(0.55f))
                }
            }
        }

        // Data section
        item { SettingsSectionHeader("DATA") }
        item {
            SettingsCard {
                SettingsRow("Retention period", "30 days (local only)", SubText)
                HorizontalDivider(color = Color(0x14EDF3FA))
                SettingsRow("Export format", "Ed25519 signed JSON", SubText)
                HorizontalDivider(color = Color(0x14EDF3FA))
                SettingsRow("Storage", "On-device only · No cloud", SubText)
            }
        }

        // Methodology section
        item { SettingsSectionHeader("TRUST SCORES") }
        item {
            SettingsCard {
                SettingsRow("Data Retention", "Weight: 40%", SubText)
                HorizontalDivider(color = Color(0x14EDF3FA))
                SettingsRow("Transparency", "Weight: 30%", SubText)
                HorizontalDivider(color = Color(0x14EDF3FA))
                SettingsRow("Opt-out Controls", "Weight: 20%", SubText)
                HorizontalDivider(color = Color(0x14EDF3FA))
                SettingsRow("Third-party Sharing", "Weight: 10%", SubText)
                HorizontalDivider(color = Color(0x14EDF3FA))
                SettingsRow("Source", "Public privacy policies · 2026", SubText)
            }
        }

        // About section
        item { SettingsSectionHeader("ABOUT") }
        item {
            SettingsCard {
                SettingsRow("Version", "v${packageInfo?.versionName ?: "1.0.0"}", SubText)
                HorizontalDivider(color = Color(0x14EDF3FA))
                SettingsRow("Developer", "AIEONYX", SovereignBlue)
                HorizontalDivider(color = Color(0x14EDF3FA))
                SettingsRow("License", "Apache 2.0", SubText)
                HorizontalDivider(color = Color(0x14EDF3FA))
                SettingsRow("Mission", "Revenue funds sovereign open-source computing", SubText)
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Text(
                "Copyright (c) 2026 Edison Lepiten / AIEONYX",
                color = SubText.copy(alpha = 0.4f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        color = SubText,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .background(Color(0x0AEDF3FA), RoundedCornerShape(14.dp)),
        content = content
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun SettingsRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            color    = SignalWhite,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            value,
            color      = valueColor,
            fontSize   = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign  = androidx.compose.ui.text.style.TextAlign.End,
            lineHeight = 15.sp,
            modifier   = Modifier.weight(0.55f)
        )
    }
}
