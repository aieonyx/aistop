// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Void   = Color(0xFF080A0D)
private val Blue   = Color(0xFF4F80D4)
private val Red    = Color(0xFFE45F65)
private val Amber  = Color(0xFFD7A84B)
private val Teal   = Color(0xFF3EB69F)
private val White  = Color(0xFFEDF3FA)
private val Sub    = Color(0x8CEDF3FA)
private val Surf1  = Color(0x0AEDF3FA)

/**
 * Accessibility Service Disclosure Screen — P0 Play Store requirement.
 *
 * Google Play policy requires apps using AccessibilityService to:
 * 1. Prominently disclose what the service monitors
 * 2. Explain why the permission is needed
 * 3. Obtain explicit user consent before directing to system settings
 *
 * This screen satisfies that requirement. It must be shown before
 * directing users to enable the Sovereign Guard accessibility service.
 *
 * Content mirrors the Play Console accessibility declaration.
 */
@Composable
fun AccessibilityDisclosureScreen(onAccept: () -> Unit, onDecline: () -> Unit) {
    val context = LocalContext.current
    var understood by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Void)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                "Before you enable",
                color    = Sub,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Sovereign Guard",
                color      = White,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Accessibility Service Disclosure",
                color    = Blue,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // ── What it monitors ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            DisclosureSection(
                icon  = "👁",
                title = "What Sovereign Guard monitors",
                color = Amber,
                items = listOf(
                    "When a known AI app (ChatGPT, Gemini, Copilot, Claude, Grammarly) comes to the foreground",
                    "Clipboard content changes while an AI app is active — to detect sensitive data before it is pasted",
                    "Window focus changes within monitored AI apps only"
                )
            )

            Spacer(Modifier.height(12.dp))

            DisclosureSection(
                icon  = "🚫",
                title = "What Sovereign Guard does NOT monitor",
                color = Teal,
                items = listOf(
                    "Keystrokes or typing in any app",
                    "Screen content, passwords, or banking apps",
                    "Any app other than the known AI apps listed above",
                    "Your notifications, contacts, or messages",
                    "Any data when no AI app is in the foreground"
                )
            )

            Spacer(Modifier.height(12.dp))

            DisclosureSection(
                icon  = "🔒",
                title = "How your data is protected",
                color = Blue,
                items = listOf(
                    "All analysis happens on your device — nothing is sent to AIEONYX servers",
                    "Clipboard content is never stored — only PII detection results are logged",
                    "You can disable Sovereign Guard at any time in Android Accessibility Settings",
                    "No account, no cloud, no telemetry"
                )
            )

            Spacer(Modifier.height(12.dp))

            // Why we need it
            Surface(
                color  = Surf1,
                shape  = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "WHY THIS PERMISSION IS NEEDED",
                        color      = Sub,
                        fontSize   = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Android only allows clipboard access to the active foreground app or keyboard. " +
                        "Sovereign Guard uses the Accessibility Service to detect when an AI app is active, " +
                        "enabling automatic clipboard protection without requiring you to manually switch keyboards " +
                        "before every paste.",
                        color    = Sub,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Explicit consent checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (understood) Blue.copy(0.08f) else Surf1, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Checkbox(
                    checked = understood,
                    onCheckedChange = { understood = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor   = Blue,
                        uncheckedColor = Sub
                    )
                )
                Text(
                    "I understand that Sovereign Guard will monitor clipboard content " +
                    "only when AI apps are active, and that all processing is on-device.",
                    color    = if (understood) White else Sub,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Enable button
            Button(
                onClick = {
                    // Open Android Accessibility Settings
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                    onAccept()
                },
                enabled  = understood,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = Blue,
                    disabledContainerColor = Surf1
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Enable Sovereign Guard",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (understood) White else Sub
                )
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick  = onDecline,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Not now — use keyboard switching instead",
                    color    = Sub,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "You can enable or disable Sovereign Guard at any time in " +
                "Settings → Accessibility → AI Stop Sovereign Guard",
                color    = Sub.copy(alpha = 0.5f),
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DisclosureSection(
    icon:  String,
    title: String,
    color: Color,
    items: List<String>
) {
    Surface(
        color  = color.copy(alpha = 0.06f),
        shape  = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(icon, fontSize = 16.sp)
                Text(
                    title,
                    color      = color,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(10.dp))
            items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("·", color = color, fontSize = 12.sp)
                    Text(
                        item,
                        color    = Sub,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
