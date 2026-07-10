// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MainActivity — Phase 2 stub.
 * Full Dashboard UI built in Phase 3.
 * ScrubShare already functional via share sheet.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF080A0D)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "AI STOP",
                        color      = Color(0xFFEDF3FA),
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "SOVEREIGN AI GUARD",
                        color    = Color(0xFF4F80D4),
                        fontSize = 11.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Phase 2 — Share text to 'Scrub with AI Stop'",
                        color    = Color(0x8CEDF3FA),
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Full dashboard coming in Phase 3",
                        color    = Color(0x8CEDF3FA),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
