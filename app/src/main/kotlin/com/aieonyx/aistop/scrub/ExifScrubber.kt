// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.scrub

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/**
 * EXIF metadata scrubber — P0 feature.
 *
 * Strips GPS coordinates, camera model, device info, and timestamps
 * from images before they are shared to AI apps via ScrubShare.
 *
 * Sensitive EXIF tags removed:
 *   - GPS: latitude, longitude, altitude, speed, direction, timestamp
 *   - Device: make, model, software, serial number
 *   - Location: image description (may contain location names)
 *   - Timestamp: original datetime (privacy-sensitive)
 *
 * Safe tags preserved:
 *   - Image dimensions, orientation, color space
 *   - Compression, bits per sample
 *
 * All processing is on-device. No image data leaves the device.
 */
object ExifScrubber {

    data class ScrubResult(
        val cleanFile:     File,
        val tagsRemoved:   List<String>,
        val hadGps:        Boolean,
        val hadDeviceInfo: Boolean
    )

    // Tags that reveal location
    private val GPS_TAGS = listOf(
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
    )

    // Tags that reveal device identity
    private val DEVICE_TAGS = listOf(
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_CAMERA_OWNER_NAME,
        ExifInterface.TAG_BODY_SERIAL_NUMBER,
        ExifInterface.TAG_LENS_SERIAL_NUMBER,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
    )

    // Tags that reveal timing
    private val TIME_TAGS = listOf(
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
    )

    // Tags that may contain text with location/identity
    private val META_TAGS = listOf(
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_MAKER_NOTE,
    )

    /**
     * Scrub EXIF metadata from an image URI.
     * Returns a clean copy in the app's cache directory.
     */
    fun scrubImage(context: Context, sourceUri: Uri): ScrubResult? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val extension = getExtension(context, sourceUri)

            // Copy to temp file
            val tempFile = File(context.cacheDir, "aistop_scrub_${System.currentTimeMillis()}.$extension")
            FileOutputStream(tempFile).use { out ->
                inputStream.copyTo(out)
            }
            inputStream.close()

            // Read EXIF from temp file
            val exif = ExifInterface(tempFile.absolutePath)
            val removedTags = mutableListOf<String>()
            var hadGps = false
            var hadDeviceInfo = false

            // Check and remove GPS tags
            if (exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null) {
                hadGps = true
            }
            GPS_TAGS.forEach { tag ->
                if (exif.getAttribute(tag) != null) {
                    exif.setAttribute(tag, null)
                    removedTags.add(tag)
                }
            }

            // Check and remove device tags
            if (exif.getAttribute(ExifInterface.TAG_MAKE) != null ||
                exif.getAttribute(ExifInterface.TAG_MODEL) != null) {
                hadDeviceInfo = true
            }
            DEVICE_TAGS.forEach { tag ->
                if (exif.getAttribute(tag) != null) {
                    exif.setAttribute(tag, null)
                    removedTags.add(tag)
                }
            }

            // Remove time tags
            TIME_TAGS.forEach { tag ->
                if (exif.getAttribute(tag) != null) {
                    exif.setAttribute(tag, null)
                    removedTags.add(tag)
                }
            }

            // Remove meta tags
            META_TAGS.forEach { tag ->
                if (exif.getAttribute(tag) != null) {
                    exif.setAttribute(tag, null)
                    removedTags.add(tag)
                }
            }

            // Save cleaned EXIF back to file
            exif.saveAttributes()

            ScrubResult(
                cleanFile     = tempFile,
                tagsRemoved   = removedTags,
                hadGps        = hadGps,
                hadDeviceInfo = hadDeviceInfo
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Quick check — does this image have GPS data?
     */
    fun hasGpsData(context: Context, uri: Uri): Boolean {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return false
            val tempFile = File(context.cacheDir, "aistop_check_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { it.write(stream.readBytes()) }
            stream.close()
            val exif = ExifInterface(tempFile.absolutePath)
            val hasGps = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null
            tempFile.delete()
            hasGps
        } catch (e: Exception) {
            false
        }
    }

    private fun getExtension(context: Context, uri: Uri): String {
        val mime = context.contentResolver.getType(uri) ?: return "jpg"
        return when (mime) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png"               -> "png"
            "image/webp"              -> "webp"
            "image/heic"              -> "heic"
            else                      -> "jpg"
        }
    }
}
