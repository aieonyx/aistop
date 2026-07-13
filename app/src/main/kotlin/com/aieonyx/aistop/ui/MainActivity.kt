// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import android.os.Bundle
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aieonyx.aistop.R

private val NavVoid  = Color(0xFF080A0D)
private val NavBlue  = Color(0xFF4F80D4)
private val NavSub   = Color(0x8CEDF3FA)
private val NavSurf  = Color(0x0AEDF3FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        setContent {
            AiStopApp()
        }
    }
}

// Tab indices
private const val TAB_PROTECT = 0
private const val TAB_AUDIT   = 1
private const val TAB_MORE    = 2

@Composable
fun AiStopApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showOnboarding by remember { mutableStateOf(!isOnboardingComplete(context)) }
    var selectedTab    by remember { mutableStateOf(TAB_PROTECT) }

    if (showOnboarding) {
        OnboardingScreen(onComplete = { showOnboarding = false })
        return@AiStopApp
    }

    val tabs = listOf(
        Triple(R.drawable.ic_nav_shield,    "PROTECT", TAB_PROTECT),
        Triple(R.drawable.ic_nav_log,       "AUDIT",   TAB_AUDIT),
        Triple(R.drawable.ic_nav_settings,  "MORE",    TAB_MORE)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavVoid)
    ) {
        // Content
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                TAB_PROTECT -> ProtectScreen()
                TAB_AUDIT   -> AuditScreen()
                TAB_MORE    -> MoreScreen()
            }
        }

        // Bottom nav
        HorizontalDivider(color = NavSurf, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavVoid)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { (iconRes, label, tabIdx) ->
                val active = selectedTab == tabIdx
                Column(
                    modifier = Modifier
                        .clickable { selectedTab = tabIdx }
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = label,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                            if (active) NavBlue else NavSub
                        ),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        label,
                        fontSize   = 8.sp,
                        color      = if (active) NavBlue else NavSub,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
