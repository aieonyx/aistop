// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aieonyx.aistop.R
import com.aieonyx.aistop.ui.theme.AiStopTheme
import com.aieonyx.aistop.ui.theme.loadDarkMode
import com.aieonyx.aistop.ui.theme.saveDarkMode

private const val TAB_PROTECT = 0
private const val TAB_AUDIT   = 1
private const val TAB_MORE    = 2

class MainActivity : androidx.fragment.app.FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        setContent { AiStopRoot() }
    }
}

@Composable
fun AiStopRoot() {
    val context  = LocalContext.current
    var darkMode by remember { mutableStateOf(loadDarkMode(context)) }

    AiStopTheme(darkTheme = darkMode) {
        AiStopApp(
            darkMode   = darkMode,
            onToggleTheme = {
                darkMode = !darkMode
                saveDarkMode(context, darkMode)
            }
        )
    }
}

@Composable
fun AiStopApp(
    darkMode: Boolean,
    onToggleTheme: () -> Unit
) {
    val context       = LocalContext.current
    val colors        = com.aieonyx.aistop.ui.theme.AiStopTheme.colors
    var showOnboarding by remember { mutableStateOf(!isOnboardingComplete(context)) }
    var selectedTab    by remember { mutableStateOf(TAB_PROTECT) }

    if (showOnboarding) {
        OnboardingScreen(onComplete = { showOnboarding = false })
        return
    }

    val tabs = listOf(
        Triple(R.drawable.ic_nav_shield,   "PROTECT", TAB_PROTECT),
        Triple(R.drawable.ic_nav_log,      "AUDIT",   TAB_AUDIT),
        Triple(R.drawable.ic_nav_settings, "MORE",    TAB_MORE)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Content
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                TAB_PROTECT -> ProtectScreen(
                    darkMode      = darkMode,
                    onToggleTheme = onToggleTheme
                )
                TAB_AUDIT   -> AuditScreen()
                TAB_MORE    -> MoreScreen()
            }
        }

        // Bottom nav — top indicator style
        HorizontalDivider(color = colors.divider, thickness = 2.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { (iconRes, label, tabIdx) ->
                val active = selectedTab == tabIdx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { selectedTab = tabIdx },
                    contentAlignment = Alignment.Center
                ) {
                    // Top indicator rail
                    if (active) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .width(48.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(colors.accentPrimary)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = label,
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                if (active) colors.accentPrimary else colors.textSecondary
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        androidx.compose.material3.Text(
                            label,
                            style = com.aieonyx.aistop.ui.theme.AiStopTheme.typography.labelSmall,
                            color = if (active) colors.textPrimary else colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
