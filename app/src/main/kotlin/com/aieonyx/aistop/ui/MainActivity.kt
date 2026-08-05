// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
private const val TAB_PULSE   = 2
private const val TAB_MORE    = 3

class MainActivity : androidx.fragment.app.FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
            darkMode      = darkMode,
            onToggleTheme = { darkMode = !darkMode; saveDarkMode(context, darkMode) }
        )
    }
}

@Composable
fun AiStopApp(
    darkMode: Boolean,
    onToggleTheme: () -> Unit
) {
    val context        = LocalContext.current
    val colors         = AiStopTheme.colors
    var showOnboarding by remember { mutableStateOf(!isOnboardingComplete(context)) }
    var selectedTab    by remember { mutableStateOf(TAB_PROTECT) }

    if (showOnboarding) {
        OnboardingScreen(onComplete = { showOnboarding = false })
        return
    }

    val purple = Color(0xFFA78BFA)

    data class TabDef(val iconRes: Int, val label: String, val idx: Int)
    val tabs = listOf(
        TabDef(R.drawable.ic_nav_shield,   "PROTECT", TAB_PROTECT),
        TabDef(R.drawable.ic_nav_log,      "AUDIT",   TAB_AUDIT),
        TabDef(R.drawable.ic_nav_radar,    "PULSE",   TAB_PULSE),
        TabDef(R.drawable.ic_nav_settings, "MORE",    TAB_MORE)
    )

    Column(
        modifier = Modifier.fillMaxSize().background(colors.background)
    ) {
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                TAB_PROTECT -> ProtectScreen(darkMode = darkMode, onToggleTheme = onToggleTheme)
                TAB_AUDIT   -> AuditScreen()
                TAB_PULSE   -> PulseScreen()
                TAB_MORE    -> MoreScreen()
            }
        }

        HorizontalDivider(color = colors.divider, thickness = 2.dp)
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.surface).height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { tab ->
                val active     = selectedTab == tab.idx
                val isPulse    = tab.idx == TAB_PULSE
                val activeColor = if (isPulse) purple else colors.accentPrimary
                val iconColor   = if (active) activeColor else colors.textSecondary
                val textColor   = if (active) (if (isPulse) purple else colors.textPrimary)
                                  else colors.textSecondary

                Box(
                    modifier = Modifier
                        .weight(1f).fillMaxHeight()
                        .clickable { selectedTab = tab.idx },
                    contentAlignment = Alignment.Center
                ) {
                    if (active) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .width(36.dp).height(4.dp)
                                .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(activeColor)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = tab.iconRes),
                            contentDescription = tab.label,
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(iconColor),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(3.dp))
                        androidx.compose.material3.Text(
                            tab.label,
                            style     = AiStopTheme.typography.labelSmall,
                            color     = textColor,
                            textAlign = TextAlign.Center,
                            fontSize  = 9.sp
                        )
                    }
                }
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
