// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import com.aieonyx.aistop.vpn.VpnIntegration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

const val TAB_PROTECT = 0
const val TAB_AUDIT   = 1
const val TAB_SHIELD  = 2
const val TAB_MORE    = 3

class MainActivity : androidx.fragment.app.FragmentActivity() {

    companion object {
        const val ACTION_OPEN_TAB = "com.aieonyx.aistop.OPEN_TAB"
        const val EXTRA_TAB       = "tab"
        const val TAB_MORE        = 3

        // StateFlow observed by Composable — no recreate() needed
        private val _pendingTab = MutableStateFlow<Int?>(null)
        val pendingTab = _pendingTab.asStateFlow()
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            VpnIntegration.start(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        handleIntent(intent)
        setContent {
            AiStopRoot(
                onRequestVpn = { VpnIntegration.requestAndStart(this, vpnPermissionLauncher) },
                onStopVpn    = { VpnIntegration.stop(this) }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_TAB) {
            val tab = intent.getIntExtra(EXTRA_TAB, -1)
            if (tab >= 0) _pendingTab.value = tab
        }
    }
}

@Composable
fun AiStopRoot(
    onRequestVpn: () -> Unit = {},
    onStopVpn:    () -> Unit = {}
) {
    val context  = LocalContext.current
    var darkMode by remember { mutableStateOf(loadDarkMode(context)) }
    AiStopTheme(darkTheme = darkMode) {
        AiStopApp(
            darkMode      = darkMode,
            onToggleTheme = { darkMode = !darkMode; saveDarkMode(context, darkMode) },
            onRequestVpn  = onRequestVpn,
            onStopVpn     = onStopVpn
        )
    }
}

@Composable
fun AiStopApp(
    darkMode: Boolean,
    onToggleTheme: () -> Unit,
    onRequestVpn: () -> Unit = {},
    onStopVpn:    () -> Unit = {}
) {
    val context        = LocalContext.current
    val colors         = AiStopTheme.colors
    var showOnboarding by remember { mutableStateOf(!isOnboardingComplete(context)) }
    var selectedTab    by remember { mutableStateOf(TAB_PROTECT) }

    // Observe pending tab from notification deep-link
    val pendingTab by MainActivity.pendingTab.collectAsState()
    LaunchedEffect(pendingTab) {
        pendingTab?.let { tab ->
            selectedTab = tab
            MainActivity.pendingTab.value?.let {
                // Clear after consuming
                (MainActivity::class.java.getDeclaredField("_pendingTab")
                    .also { it.isAccessible = true }
                    .get(null) as MutableStateFlow<*>).let {
                    @Suppress("UNCHECKED_CAST")
                    (it as MutableStateFlow<Int?>).value = null
                }
            }
        }
    }

    if (showOnboarding) {
        OnboardingScreen(onComplete = { showOnboarding = false })
        return
    }

    val purple = Color(0xFFA78BFA)

    data class TabDef(val iconRes: Int, val label: String, val idx: Int)
    val tabs = listOf(
        TabDef(R.drawable.ic_nav_shield,   "PROTECT", TAB_PROTECT),
        TabDef(R.drawable.ic_nav_log,      "AUDIT",   TAB_AUDIT),
        TabDef(R.drawable.ic_nav_radar,    "SHIELD",  TAB_SHIELD),
        TabDef(R.drawable.ic_nav_settings, "MORE",    TAB_MORE)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                TAB_PROTECT -> ProtectScreen(
                    darkMode      = darkMode,
                    onToggleTheme = onToggleTheme,
                    onRequestVpn  = onRequestVpn,
                    onStopVpn     = onStopVpn
                )
                TAB_AUDIT  -> AuditScreen()
                TAB_SHIELD -> ShieldScreen()
                TAB_MORE   -> MoreScreen()
            }
        }

        HorizontalDivider(color = colors.divider, thickness = 2.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { tab ->
                val active      = selectedTab == tab.idx
                val isShield    = tab.idx == TAB_SHIELD
                val activeColor = if (isShield) purple else colors.accentPrimary
                val iconColor   = if (active) activeColor else colors.textSecondary
                val textColor   = if (active) (if (isShield) purple else colors.textPrimary)
                                  else colors.textSecondary

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { selectedTab = tab.idx },
                    contentAlignment = Alignment.Center
                ) {
                    if (active) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .width(36.dp)
                                .height(4.dp)
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
