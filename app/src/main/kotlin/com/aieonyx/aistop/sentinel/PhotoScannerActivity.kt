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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import com.aieonyx.aistop.ui.theme.AiStopTheme
import com.aieonyx.aistop.ui.theme.AiStopColors
import com.aieonyx.aistop.ui.theme.AiStopTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PhotoScannerActivity — intercepts photo shares.
 * Registered as image share target in AndroidManifest.
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
                    uri       = imageUri,
                    onScrub   = { scrubAndFinish(imageUri) },
                    onSendAsIs = { finish() },
                    onCancel  = { finish() }
                )
            }
        }
    }

    private fun scrubAndFinish(uri: Uri) {
        try {
            contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                // Remove location
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
                exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null)
                exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, null)
                exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, null)
                // Remove device info
                exif.setAttribute(ExifInterface.TAG_MAKE, null)
                exif.setAttribute(ExifInterface.TAG_MODEL, null)
                exif.setAttribute(ExifInterface.TAG_BODY_SERIAL_NUMBER, null)
                exif.setAttribute(ExifInterface.TAG_LENS_SERIAL_NUMBER, null)
                // Remove timestamp
                exif.setAttribute(ExifInterface.TAG_DATETIME, null)
                exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, null)
                exif.saveAttributes()
            }
        } catch (e: Exception) { }
        finish()
    }
}

// ── EXIF scan ─────────────────────────────────────────────────────────────────

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

// ── UI ────────────────────────────────────────────────────────────────────────

@Composable
fun PhotoScannerScreen(
    uri:        Uri,
    onScrub:    () -> Unit,
    onSendAsIs: () -> Unit,
    onCancel:   () -> Unit
) {
    val context  = LocalContext.current
    val colors   = AiStopTheme.colors
    val typo     = AiStopTheme.typography
    var findings by remember { mutableStateOf<ExifFindings?>(null) }
    var scanning by remember { mutableStateOf(true) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            val result = scanExif(context, uri)
            withContext(Dispatchers.Main) {
                findings = result
                scanning = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(colors.surface)
                .padding(20.dp)
        ) {
            if (scanning) {
                Text(
                    "Scanning photo metadata...",
                    style    = typo.body,
                    color    = colors.textSecondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                val f = findings!!

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🖼", fontSize = 28.sp)
                    Column {
                        Text(
                            if (f.hasAnyData) "METADATA DETECTED" else "NO METADATA FOUND",
                            style = typo.label,
                            color = if (f.hasAnyData) colors.warning else colors.success
                        )
                        Text(
                            if (f.hasAnyData)
                                "This photo contains data that could reveal your identity or location."
                            else
                                "This photo appears clean — no sensitive metadata found.",
                            style = typo.caption,
                            color = colors.textSecondary
                        )
                    }
                }

                if (f.hasAnyData) {
                    Spacer(Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surface2)
                            .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (f.hasGps)
                            MetaRow("📍 GPS", "${f.gpsLat}, ${f.gpsLon}", colors.danger, colors, typo)
                        if (f.cameraModel != null)
                            MetaRow("📷 Camera", "${f.cameraMake ?: ""} ${f.cameraModel}", colors.warning, colors, typo)
                        if (f.timestamp != null)
                            MetaRow("🕐 Taken", f.timestamp, colors.warning, colors, typo)
                        if (f.serial != null)
                            MetaRow("🔢 Serial", f.serial, colors.warning, colors, typo)
                    }

                    Spacer(Modifier.height(20.dp))

                    // SCRUB button
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
                        Text("Send as-is (metadata included)", style = typo.label, color = colors.textSecondary)
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
                        Text("Continue", style = typo.label, color = colors.onPrimary)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "AI Stop · On-device scan · No data sent",
                style    = typo.caption,
                color    = colors.disabled,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MetaRow(
    label:  String,
    value:  String,
    color:  Color,
    colors: AiStopColors,
    typo:   AiStopTypography
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, style = typo.bodySmall, color = colors.textPrimary, modifier = Modifier.weight(0.4f))
        Text(value, style = typo.caption,   color = color,              modifier = Modifier.weight(0.6f))
    }
}
