// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aieonyx.aistop.R

private val Void  = Color(0xFF080A0D)
private val Blue  = Color(0xFF4F80D4)
private val Teal  = Color(0xFF3EB69F)
private val White = Color(0xFFEDF3FA)
private val Sub   = Color(0x8CEDF3FA)
private val Surf1 = Color(0x0AEDF3FA)

data class OnboardPage(
    val imageRes:    Int,
    val title:       String,
    val description: String,
    val ctaLabel:    String,
    val ctaColor:    Color
)

/**
 * Three-screen onboarding flow.
 * Uses ChatGPT-generated illustrations.
 * Shown only on first launch — stored in SharedPreferences.
 */
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var page by remember { mutableStateOf(0) }

    val pages = listOf(
        OnboardPage(
            imageRes    = R.drawable.ob_illus_1_gate,
            title       = "Your words belong to you.",
            description = "AI Stop keeps your input private and protects you from unnecessary AI data sharing. Everything runs on your device.",
            ctaLabel    = "Next",
            ctaColor    = Blue
        ),
        OnboardPage(
            imageRes    = R.drawable.ob_illus_2_keyboard,
            title       = "Enable the protection layer.",
            description = "AI Stop monitors paste events when you use AI apps. Enable the Sovereign Guard in Accessibility Settings for automatic protection — no keyboard switching needed.",
            ctaLabel    = "Next",
            ctaColor    = Blue
        ),
        OnboardPage(
            imageRes    = R.drawable.ob_illus_3_toggle,
            title       = "You're in control.",
            description = "Choose what to protect, monitor what matters, and keep your data yours. Block, redact, or allow — every decision is yours.",
            ctaLabel    = "Get Started",
            ctaColor    = Teal
        )
    )

    val current = pages[page]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Void)
    ) {
        // Skip button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                markOnboardingComplete(context)
                onComplete()
            }) {
                Text("Skip", color = Sub, fontSize = 13.sp)
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
                modifier           = Modifier.fillMaxSize(0.85f)
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                pages.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == page) 20.dp else 6.dp, 6.dp)
                            .clip(if (i == page) RoundedCornerShape(3.dp) else CircleShape)
                            .background(if (i == page) Blue else Sub.copy(alpha = 0.4f))
                    )
                }
            }

            Text(
                current.title,
                color      = White,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                lineHeight = 30.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                current.description,
                color     = Sub,
                fontSize  = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    if (page < pages.size - 1) {
                        page++
                    } else {
                        markOnboardingComplete(context)
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = current.ctaColor
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    current.ctaLabel,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = White
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Built on sovereign computing principles by AIEONYX",
                color    = Sub.copy(0.4f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))
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
