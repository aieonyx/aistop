// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import android.content.Context
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aieonyx.aistop.R

private val ObVoid  = Color(0xFF080A0D)
private val ObBlue  = Color(0xFF4F80D4)
private val ObTeal  = Color(0xFF3EB69F)
private val ObWhite = Color(0xFFEDF3FA)
private val ObSub   = Color(0x8CEDF3FA)
private val ObSurf1 = Color(0x0AEDF3FA)
private val ObSurf2 = Color(0x14EDF3FA)

private data class OnboardPage(
    val imageRes: Int,
    val title:    String,
    val body:     String
)

private val PAGES = listOf(
    OnboardPage(
        imageRes = R.drawable.ob_new_1,
        title    = "AI apps are watching what you type.",
        body     = "Every paste into ChatGPT, Gemini, or Copilot sends your data to their servers. AI Stop intercepts it first — on your device, before it leaves."
    ),
    OnboardPage(
        imageRes = R.drawable.ob_new_2,
        title    = "Your words are scanned for secrets.",
        body     = "API keys, passwords, passport numbers, health data — AI models process all of it. AI Stop detects and blocks sensitive data automatically."
    ),
    OnboardPage(
        imageRes = R.drawable.ob_new_3,
        title    = "Enable the Sovereign Guard.",
        body     = "AI Stop uses Android Accessibility Services to monitor paste events in AI apps. Enable it once — protection runs silently in the background."
    ),
    OnboardPage(
        imageRes = R.drawable.ob_new_4,
        title    = "See every exposure. Score every AI.",
        body     = "The audit log shows exactly what data was intercepted and when. Every AI app gets a Trust Score based on their real privacy policies."
    ),
    OnboardPage(
        imageRes = R.drawable.ob_new_5,
        title    = "You are the sovereign.",
        body     = "Choose how AI Stop protects you. You can change this any time from the Protect tab."
    )
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context    = LocalContext.current
    var page       by remember { mutableStateOf(0) }
    var chosenMode by remember { mutableStateOf(SovereignMode.DEFAULT) }
    val isLastPage = page == PAGES.lastIndex
    val current    = PAGES[page]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObVoid)
    ) {
        // Skip (pages 1-4 only)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (!isLastPage) {
                TextButton(onClick = {
                    saveSovereignMode(context, SovereignMode.DEFAULT)
                    markOnboardingComplete(context)
                    onComplete()
                }) {
                    Text("Skip", color = ObSub, fontSize = 13.sp)
                }
            } else {
                Spacer(Modifier.height(36.dp))
            }
        }

        // Illustration
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter            = painterResource(current.imageRes),
                contentDescription = current.title,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxSize(if (isLastPage) 0.72f else 0.88f)
            )
        }

        // Bottom panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isLastPage) Modifier.verticalScroll(rememberScrollState())
                    else Modifier
                )
                .padding(horizontal = 28.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                PAGES.indices.forEach { i ->
                    val width by animateDpAsState(
                        targetValue = if (i == page) 22.dp else 6.dp,
                        label       = "dot_$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(width, 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (i == page) ObBlue else ObSub.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Text(
                current.title,
                color      = ObWhite,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                current.body,
                color      = ObSub,
                fontSize   = 14.sp,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp
            )

            // Mode picker — last page only
            if (isLastPage) {
                Spacer(Modifier.height(20.dp))
                SovereignMode.entries.forEach { mode ->
                    ModeSelectorCard(
                        mode     = mode,
                        selected = chosenMode == mode,
                        onSelect = { chosenMode = mode }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (!isLastPage) {
                        page++
                    } else {
                        saveSovereignMode(context, chosenMode)
                        markOnboardingComplete(context)
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLastPage) ObTeal else ObBlue
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (isLastPage) "Start protecting" else "Next",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = ObWhite
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Built on sovereign computing principles by AIEONYX",
                color     = ObSub.copy(0.35f),
                fontSize  = 10.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ModeSelectorCard(
    mode:     SovereignMode,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val accentColor = when (mode) {
        SovereignMode.AUTOPILOT -> ObTeal
        SovereignMode.DEFAULT   -> ObBlue
        SovereignMode.MANUAL    -> Color(0xFFA78BFA)
    }
    val icon = when (mode) {
        SovereignMode.AUTOPILOT -> "✈"
        SovereignMode.DEFAULT   -> "◉"
        SovereignMode.MANUAL    -> "⚙"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accentColor.copy(alpha = 0.08f) else ObSurf1)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accentColor else ObSurf2,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp, modifier = Modifier.width(32.dp))
        Column(Modifier.weight(1f)) {
            Text(
                mode.displayName,
                color      = if (selected) ObWhite else ObSub,
                fontSize   = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(Modifier.height(2.dp))
            Text(
                mode.tagline,
                color      = if (selected) accentColor else ObSub.copy(alpha = 0.6f),
                fontSize   = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            if (selected) {
                Spacer(Modifier.height(4.dp))
                Text(
                    mode.detail,
                    color      = ObSub,
                    fontSize   = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(accentColor, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = ObWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun markOnboardingComplete(context: Context) {
    context.getSharedPreferences("aistop_prefs", Context.MODE_PRIVATE)
        .edit().putBoolean("onboarding_complete", true).apply()
}

fun isOnboardingComplete(context: Context): Boolean =
    context.getSharedPreferences("aistop_prefs", Context.MODE_PRIVATE)
        .getBoolean("onboarding_complete", false)
