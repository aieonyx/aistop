// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import android.os.Bundle
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
        setContent {
            AiStopApp()
        }
    }
}

@Composable
fun AiStopApp() {
    var selectedTab by remember { mutableStateOf(0) }

    val tabs = listOf(
        Pair(R.drawable.ic_nav_dashboard, "Dashboard"),
        Pair(R.drawable.ic_nav_scrub,     "Scrub"),
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
                1 -> ScrubTabScreen()
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SovereignVoid),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Settings",
                color    = SignalWhite,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Phase 6",
                color    = SubText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
