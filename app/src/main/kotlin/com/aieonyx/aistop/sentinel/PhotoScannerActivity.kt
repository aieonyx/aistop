// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.sentinel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import com.aieonyx.aistop.jni.AiStopCore
import com.aieonyx.aistop.ui.theme.AiStopTheme
import com.aieonyx.aistop.ui.theme.AiStopColors
import com.aieonyx.aistop.ui.theme.AiStopTypography
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * PhotoScannerActivity — full AI-powered photo scanner.
 *
 * Pipeline:
 *   1. EXIF scan — GPS, camera model, serial, timestamp
 *   2. ML Kit OCR — extract text from image content (on-device)
 *   3. Rust PII engine — detect passport numbers, credit cards,
 *      SSNs, API keys, crypto wallets in the extracted text
 *   4. Show combined findings — EXIF + content PII
 *   5. Offer: Scrub EXIF / Save to Vault / Send as-is / Cancel
 *
 * All processing is 100% on-device. No image data leaves the device.
 */
class PhotoScannerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imageUri: Uri? = when (intent?.action) {
            Intent.ACTION_SEND -> @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }
        if (imageUri == null) { finish(); return }

        setContent {
            AiStopTheme(darkTheme = true) {
                PhotoScannerScreen(
                    uri        = imageUri,
                    onScrub    = { scrubExif(imageUri) },
                    onSendAsIs = { finish() },
                    onCancel   = { finish() }
                )
            }
        }
    }

    private fun scrubExif(uri: Uri) {
        try {
            contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,         null)
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,        null)
                exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE,         null)
                exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP,        null)
                exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP,        null)
                exif.setAttribute(ExifInterface.TAG_MAKE,                 null)
                exif.setAttribute(ExifInterface.TAG_MODEL,                null)
                exif.setAttribute(ExifInterface.TAG_BODY_SERIAL_NUMBER,   null)
                exif.setAttribute(ExifInterface.TAG_LENS_SERIAL_NUMBER,   null)
                exif.setAttribute(ExifInterface.TAG_DATETIME,             null)
                exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL,    null)
                exif.saveAttributes()
            }
        } catch (e: Exception) { }
        finish()
    }
}

// ── Data models ────────────────────────────────────────────────────────────────

data class ExifFindings(
    val hasGps:      Boolean,
    val gpsLat:      String?,
    val gpsLon:      String?,
    val cameraModel: String?,
    val cameraMake:  String?,
    val timestamp:   String?,
    val serial:      String?,
    val hasAnyData:  Boolean
)

data class ContentPiiFinding(
    val piiClass: String,
    val masked:   String
)

data class PhotoScanResult(
    val exif:        ExifFindings,
    val contentPii:  List<ContentPiiFinding>,
    val rawText:     String,
    val hasAnyThreat: Boolean
)

// ── Scanning logic ─────────────────────────────────────────────────────────────

fun scanExif(context: Context, uri: Uri): ExifFindings {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif     = ExifInterface(stream)
            val lat      = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
            val lon      = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
            val model    = exif.getAttribute(ExifInterface.TAG_MODEL)
            val make     = exif.getAttribute(ExifInterface.TAG_MAKE)
            val time     = exif.getAttribute(ExifInterface.TAG_DATETIME)
            val serial   = exif.getAttribute(ExifInterface.TAG_BODY_SERIAL_NUMBER)
            ExifFindings(
                hasGps      = lat != null && lon != null,
                gpsLat      = lat,
                gpsLon      = lon,
                cameraModel = model,
                cameraMake  = make,
                timestamp   = time,
                serial      = serial,
                hasAnyData  = lat != null || lon != null || model != null || time != null
            )
        } ?: ExifFindings(false, null, null, null, null, null, null, false)
    } catch (e: Exception) {
        ExifFindings(false, null, null, null, null, null, null, false)
    }
}

suspend fun scanImageContent(context: Context, uri: Uri): List<ContentPiiFinding> {
    return try {
        val image      = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result     = recognizer.process(image).await()
        val text       = result.text
        if (text.isBlank()) return emptyList()

        // Run Rust PII engine on extracted text
        val piiResult = withContext(Dispatchers.IO) {
            runCatching { AiStopCore.piiDetect(text) }.getOrNull()
        } ?: return emptyList()

        val json = runCatching { JSONObject(piiResult) }.getOrNull() ?: return emptyList()
        if (!json.has("PiiFound")) return emptyList()

        val matches = json.getJSONObject("PiiFound").getJSONArray("matches")
        (0 until matches.length()).map { i ->
            val m = matches.getJSONObject(i)
            ContentPiiFinding(
                piiClass = m.getString("class"),
                masked   = m.getString("masked")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

// ── UI ─────────────────────────────────────────────────────────────────────────

@Composable
fun PhotoScannerScreen(
    uri:        Uri,
    onScrub:    () -> Unit,
    onSendAsIs: () -> Unit,
    onCancel:   () -> Unit
) {
    val context = LocalContext.current
    val colors  = AiStopTheme.colors
    val typo    = AiStopTheme.typography

    var scanning  by remember { mutableStateOf(true) }
    var scanPhase by remember { mutableStateOf("Scanning EXIF metadata…") }
    var result    by remember { mutableStateOf<PhotoScanResult?>(null) }

    LaunchedEffect(uri) {
        // Phase 1 — EXIF
        scanPhase = "Scanning EXIF metadata…"
        val exif = withContext(Dispatchers.IO) { scanExif(context, uri) }

        // Phase 2 — ML Kit OCR + Rust PII
        scanPhase = "Scanning image content with AI…"
        val contentPii = scanImageContent(context, uri)

        result = PhotoScanResult(
            exif         = exif,
            contentPii   = contentPii,
            rawText      = "",
            hasAnyThreat = exif.hasAnyData || contentPii.isNotEmpty()
        )
        scanning = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(colors.surface)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            if (scanning) {
                // Loading state
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = colors.accentPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(scanPhase, style = typo.body, color = colors.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "On-device · No data sent",
                        style = typo.caption,
                        color = colors.disabled
                    )
                }
            } else {
                val r = result!!

                // Header
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.spacedBy(10.dp)
                ) {
                    Text(if (r.hasAnyThreat) "⚠" else "✅", fontSize = 28.sp)
                    Column {
                        Text(
                            if (r.hasAnyThreat) "SENSITIVE DATA DETECTED" else "PHOTO IS CLEAN",
                            style = typo.label,
                            color = if (r.hasAnyThreat) colors.danger else colors.success
                        )
                        Text(
                            if (r.hasAnyThreat)
                                "This photo contains data that could identify you."
                            else
                                "No sensitive metadata or content PII found.",
                            style = typo.caption,
                            color = colors.textSecondary
                        )
                    }
                }

                if (r.hasAnyThreat) {
                    Spacer(Modifier.height(16.dp))

                    // EXIF findings
                    if (r.exif.hasAnyData) {
                        ScanSection("📷 METADATA", colors, typo) {
                            if (r.exif.hasGps)
                                FindingRow("GPS Location", "${r.exif.gpsLat}, ${r.exif.gpsLon}", colors.danger, colors, typo)
                            if (r.exif.cameraModel != null)
                                FindingRow("Camera", "${r.exif.cameraMake ?: ""} ${r.exif.cameraModel}", colors.warning, colors, typo)
                            if (r.exif.timestamp != null)
                                FindingRow("Timestamp", r.exif.timestamp, colors.warning, colors, typo)
                            if (r.exif.serial != null)
                                FindingRow("Serial", r.exif.serial, colors.warning, colors, typo)
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // Content PII findings
                    if (r.contentPii.isNotEmpty()) {
                        ScanSection("🔍 CONTENT (AI SCAN)", colors, typo) {
                            r.contentPii.distinctBy { it.piiClass }.forEach { finding ->
                                val icon = when (finding.piiClass) {
                                    "CreditCard", "Iban" -> "💳"
                                    "PassportNumber"     -> "🪪"
                                    "Ssn"                -> "🔢"
                                    "ApiKey", "AwsKey"   -> "🔑"
                                    "CryptoWallet"       -> "🪙"
                                    "Phone"              -> "📱"
                                    else                 -> "⚠"
                                }
                                FindingRow(
                                    "$icon ${finding.piiClass}",
                                    finding.masked,
                                    colors.danger,
                                    colors,
                                    typo
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    Spacer(Modifier.height(8.dp))

                    // Scrub EXIF button (always available if EXIF found)
                    if (r.exif.hasAnyData) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.success)
                                .clickable { onScrub() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛡 SCRUB METADATA & CONTINUE", style = typo.label, color = colors.onSignal)
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Send as-is
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surface2)
                            .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
                            .clickable { onSendAsIs() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (r.contentPii.isNotEmpty()) "Send anyway (content PII included)"
                            else "Send as-is (metadata included)",
                            style = typo.label,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCancel() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", style = typo.caption, color = colors.textSecondary)
                    }
                } else {
                    // Clean photo
                    Spacer(Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.accentPrimary)
                            .clickable { onSendAsIs() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Continue — photo is clean", style = typo.label, color = colors.onPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCancel() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", style = typo.caption, color = colors.textSecondary)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "AI Stop · On-device scan · No data sent · ML Kit OCR + Rust PII engine",
                    style     = typo.caption,
                    color     = colors.disabled,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ScanSection(
    title:   String,
    colors:  AiStopColors,
    typo:    AiStopTypography,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface2)
            .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = typo.labelSmall, color = colors.textSecondary)
        content()
    }
}

@Composable
private fun FindingRow(
    label:  String,
    value:  String,
    color:  Color,
    colors: AiStopColors,
    typo:   AiStopTypography
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Text(label, style = typo.bodySmall, color = colors.textPrimary, modifier = Modifier.weight(0.45f))
        Text(value, style = typo.caption,   color = color,              modifier = Modifier.weight(0.55f))
    }
}
