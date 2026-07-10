// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aieonyx.aistop.db.EventType
import com.aieonyx.aistop.db.ExposureDatabase
import com.aieonyx.aistop.db.ExposureEvent
import com.aieonyx.aistop.export.ExportManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Sovereign color tokens
private val SovereignVoid  = Color(0xFF080A0D)
private val SovereignBlue  = Color(0xFF4F80D4)
private val ThreatRed      = Color(0xFFE45F65)
private val CautionAmber   = Color(0xFFD7A84B)
private val VerifiedTeal   = Color(0xFF3EB69F)
private val SignalWhite    = Color(0xFFEDF3FA)
private val SubText        = Color(0x8CEDF3FA)
private val Surface1       = Color(0x0AEDF3FA)
private val Surface2       = Color(0x12EDF3FA)

/**
 * M4 Exposure Log screen.
 *
 * Free tier: view 30-day metadata log.
 * Paid tier: signed export (Ed25519 + BLAKE3).
 *
 * Teal = historical ALLOWED events only.
 * Never uses teal for active decisions.
 */
@Composable
fun LogScreen() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var events  by remember { mutableStateOf<List<ExposureEvent>>(emptyList()) }
    var exporting by remember { mutableStateOf(false) }
    var exportMsg by remember { mutableStateOf("") }

    // Load events
    LaunchedEffect(Unit) {
        val midnight = midnightTs()
        ExposureDatabase.getInstance(context)
            .exposureDao()
            .observeRecent(System.currentTimeMillis() - ExposureDatabase.RETENTION_MS)
            .collect { events = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SovereignVoid)
    ) {
        // ── Top bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Exposure Log",
                color      = SignalWhite,
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.weight(1f)
            )
            // Export button — paid tier
            Button(
                onClick = {
                    scope.launch {
                        exporting = true
                        exportMsg = ""
                        try {
                            val result = ExportManager.exportSignedReport(context)
                            exportMsg = "${result.eventCount} events · signed"
                            ExportManager.shareExport(context, result.file)
                        } catch (e: Exception) {
                            exportMsg = "Export failed: ${e.message?.take(40)}"
                        } finally {
                            exporting = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Surface1,
                    contentColor   = SovereignBlue
                ),
                shape    = RoundedCornerShape(8.dp),
                enabled  = !exporting,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    if (exporting) "Signing…" else "Export signed report",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Export sub-label
        Text(
            "Ed25519 · BLAKE3",
            color    = SubText,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (exportMsg.isNotEmpty()) {
            Text(
                exportMsg,
                color    = if (exportMsg.contains("failed")) ThreatRed else VerifiedTeal,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // ── Retention notice ──
        Text(
            "Events stored locally · 30-day retention",
            color    = SubText,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Divider(color = Surface2, thickness = 1.dp)

        // ── Event list ──
        if (events.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No exposure events yet",
                        color      = SubText,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Protection is active",
                        color    = VerifiedTeal,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(events) { event ->
                    EventRow(event)
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: ExposureEvent) {
    val dotColor = when (event.eventType) {
        EventType.PASTE_BLOCKED  -> ThreatRed
        EventType.PASTE_REDACTED -> SovereignBlue
        EventType.PASTE_ALLOWED  -> VerifiedTeal  // teal = historical only
        EventType.SCRUB_SHARE    -> SovereignBlue
        EventType.CLIP_AUTOCLEAR -> CautionAmber
        else                     -> SubText
    }
    val typeLabel = when (event.eventType) {
        EventType.PASTE_BLOCKED  -> "BLOCKED"
        EventType.PASTE_REDACTED -> "REDACTED"
        EventType.PASTE_ALLOWED  -> "ALLOWED"
        EventType.SCRUB_SHARE    -> "SCRUBBED"
        EventType.CLIP_AUTOCLEAR -> "CLIP CLEARED"
        else                     -> event.eventType
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Event dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )

        // Event info
        Column(Modifier.weight(1f)) {
            Text(
                typeLabel,
                color      = dotColor,
                fontSize   = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                event.appLabel,
                color      = SignalWhite,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (event.piiClasses.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    event.piiClasses.split(",").take(3).forEach { cls ->
                        if (cls.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(Surface2, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    cls.trim(),
                                    color      = SubText,
                                    fontSize   = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Timestamp
        Text(
            formatTs(event.ts),
            color      = SubText,
            fontSize   = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatTs(ts: Long): String {
    val now     = System.currentTimeMillis()
    val diffMs  = now - ts
    val diffMin = diffMs / 60_000
    return when {
        diffMin < 60   -> "${diffMin}m ago"
        diffMin < 1440 -> "${diffMin / 60}h ago"
        else           -> SimpleDateFormat("MMM d", Locale.US).format(Date(ts))
    }
}

private fun midnightTs(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
