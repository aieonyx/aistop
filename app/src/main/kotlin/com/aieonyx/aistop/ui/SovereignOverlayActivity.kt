// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aieonyx.aistop.core.TrustDatabase
import com.aieonyx.aistop.db.EdisonExposureDatabase as ExposureDatabase
import com.aieonyx.aistop.db.EventType
import com.aieonyx.aistop.jni.AiStopCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Sovereign Overlay — appears automatically when PII detected in clipboard
 * while an AI app is in foreground. No keyboard switch required.
 *
 * Three actions:
 *   BLOCK — clear clipboard, nothing sent
 *   REDACT — replace clipboard with redacted text, user pastes clean version
 *   ALLOW — dismiss, original text remains in clipboard
 */
class SovereignOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text       = intent.getStringExtra("text") ?: ""
        val pkg        = intent.getStringExtra("package") ?: ""
        val detection  = intent.getStringExtra("detection") ?: "{}"
        val appLabel   = TrustDatabase.appLabel(pkg)
        val trustScore = TrustDatabase.entry(pkg).let {
            ((it.retentionScore * 0.4 + it.transparencyScore * 0.3 +
              it.optOutScore * 0.2) * 0.1).toInt().coerceIn(0, 100)
        }
        val piiClasses = parsePiiClasses(detection)
        val redacted   = applyRedaction(text, detection)

        setContent {
            SovereignOverlayScreen(
                text        = text,
                redacted    = redacted,
                appLabel    = appLabel,
                trustScore  = trustScore,
                piiClasses  = piiClasses,
                onBlock     = {
                    clearClipboard()
                    logEvent(pkg, appLabel, EventType.PASTE_BLOCKED, text, piiClasses)
                    finish()
                },
                onRedact    = {
                    setClipboard(redacted)
                    logEvent(pkg, appLabel, EventType.PASTE_REDACTED, text, piiClasses)
                    finish()
                },
                onAllow     = {
                    logEvent(pkg, appLabel, EventType.PASTE_ALLOWED, text, piiClasses)
                    finish()
                }
            )
        }
    }

    private fun clearClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("", ""))
    }

    private fun setClipboard(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("AI Stop Redacted", text))
    }

    private fun logEvent(pkg: String, label: String, type: String,
                         text: String, classes: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            ExposureDatabase.getInstance(this@SovereignOverlayActivity)
                .exposureDao().insert(
                    com.aieonyx.aistop.db.ExposureEvent(
                        ts          = System.currentTimeMillis(),
                        packageName = pkg,
                        appLabel    = label,
                        eventType   = type,
                        preview20   = text.take(20),
                        trustScore  = TrustDatabase.entry(pkg).let {
                            ((it.retentionScore * 0.4 + it.transparencyScore * 0.3 +
                              it.optOutScore * 0.2) * 0.1).toInt().coerceIn(0, 100)
                        },
                        piiClasses  = classes.joinToString(",")
                    )
                )
        }
    }

    private fun parsePiiClasses(json: String): List<String> {
        return try {
            val obj = JSONObject(json)
            if (!obj.has("PiiFound")) return emptyList()
            val matches = obj.getJSONObject("PiiFound").getJSONArray("matches")
            (0 until matches.length())
                .map { matches.getJSONObject(it).getString("class") }
                .distinct()
        } catch (e: Exception) { emptyList() }
    }

    private fun applyRedaction(text: String, json: String): String {
        return try {
            val obj = JSONObject(json)
            if (!obj.has("PiiFound")) return text
            val matches = obj.getJSONObject("PiiFound").getJSONArray("matches")
            var result = text
            val counters = mutableMapOf<String, Int>()
            val sorted = (0 until matches.length())
                .map { matches.getJSONObject(it) }
                .sortedByDescending { it.getInt("start") }
            for (m in sorted) {
                val cls = m.getString("class")
                val start = m.getInt("start")
                val end = m.getInt("end")
                val count = (counters[cls] ?: 0) + 1
                counters[cls] = count
                if (start >= 0 && end <= result.length && start < end)
                    result = result.substring(0, start) + "[${cls}_$count]" + result.substring(end)
            }
            result
        } catch (e: Exception) { text }
    }
}

private val Void  = Color(0xFF080A0D)
private val Blue  = Color(0xFF4F80D4)
private val Red   = Color(0xFFE45F65)
private val Amber = Color(0xFFD7A84B)
private val Teal  = Color(0xFF3EB69F)
private val White = Color(0xFFEDF3FA)
private val Sub   = Color(0x8CEDF3FA)
private val Surf1 = Color(0x0AEDF3FA)

@Composable
fun SovereignOverlayScreen(
    text:       String,
    redacted:   String,
    appLabel:   String,
    trustScore: Int,
    piiClasses: List<String>,
    onBlock:    () -> Unit,
    onRedact:   () -> Unit,
    onAllow:    () -> Unit
) {
    val scoreColor = if (trustScore < 40) Red else Amber

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F1318), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Blue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚠", fontSize = 18.sp)
                }
                Column {
                    Text(
                        "Sensitive data found",
                        color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Pasting into $appLabel",
                        color = Sub, fontSize = 11.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$trustScore",
                        color = scoreColor, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "TRUST SCORE",
                        color = Sub, fontSize = 7.sp, fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // PII chips
            if (piiClasses.isNotEmpty()) {
                Text("PII DETECTED", color = Sub, fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    piiClasses.take(5).forEach { cls ->
                        Surface(
                            color = Red.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                cls, color = Red, fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // BLOCK
                Button(
                    onClick = onBlock,
                    modifier = Modifier.weight(0.28f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Red.copy(alpha = 0.15f),
                        contentColor = Red
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BLOCK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Clear clipboard", fontSize = 7.sp, color = Red.copy(0.7f))
                    }
                }

                // REDACT
                Button(
                    onClick = onRedact,
                    modifier = Modifier.weight(0.44f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue,
                        contentColor = White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("REDACT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Replace with clean text", fontSize = 7.sp,
                            color = White.copy(0.7f))
                    }
                }

                // ALLOW
                Button(
                    onClick = onAllow,
                    modifier = Modifier.weight(0.28f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Surf1,
                        contentColor = Sub
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ALLOW", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Send as is", fontSize = 7.sp, color = Sub)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "AI Stop · On-device detection · No data sent",
                color = Sub.copy(0.5f), fontSize = 9.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
