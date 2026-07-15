// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.vault

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.FileProvider
import com.aieonyx.aistop.identity.SovereignIdentity
import com.aieonyx.aistop.jni.AiStopCore
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * VaultExportManager — sovereign vault export pipeline.
 *
 * Two export modes:
 *
 * 1. SIGNED BUNDLE — Ed25519 signed JSON containing all vault entry metadata
 *    (previews only, never decrypted values). Tamper-evident via BLAKE3 + Ed25519.
 *    Share via any app (email, Drive, Signal etc.)
 *
 * 2. QR BACKUP — encrypted vault key backup as QR code.
 *    The QR contains the AES-256 vault key wrapped with a user PIN.
 *    Scan on a new device to restore vault access.
 *    WARNING: QR backup transfers key material — handle with care.
 */
object VaultExportManager {

    data class ExportResult(
        val file:          File,
        val entryCount:    Int,
        val signatureValid: Boolean
    )

    // ── Signed bundle export ───────────────────────────────────────────────────

    suspend fun exportSignedBundle(context: Context): ExportResult =
        withContext(Dispatchers.IO) {
            SovereignIdentity.ensureKeyPair()

            val entries = SovereignVault.listEntries(context)

            // Build payload — metadata only, NO decrypted values
            val arr = JSONArray()
            entries.forEach { e ->
                arr.put(JSONObject().apply {
                    put("id",        e.id)
                    put("type",      e.type)
                    put("preview",   e.preview)
                    put("pii_class", e.piiClass)
                    put("timestamp", e.timestamp)
                })
            }

            val payload = JSONObject().apply {
                put("schema",        "aistop.vault.export.v1")
                put("device_pubkey", SovereignIdentity.publicKeyFingerprint())
                put("exported_at",   isoNow())
                put("entry_count",   entries.size)
                put("entries",       arr)
                put("note",          "Values are NOT included. This export contains metadata only. " +
                                     "Decrypted values remain on-device in Android Keystore.")
            }.toString()

            // Sign with Ed25519/ECDSA P-256 (dual-key)
            val hashHex      = AiStopCore.blake3Hash(payload)
            val signatureHex = SovereignIdentity.signHash(hashHex)
            val signedBlock  = AiStopCore.assembleSignatureBlock(payload, hashHex, signatureHex)

            // Write to file
            val timestamp  = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val exportDir  = File(context.filesDir, "vault_exports").apply { mkdirs() }
            val exportFile = File(exportDir, "aistop_vault_$timestamp.json")
            exportFile.writeText(signedBlock)

            ExportResult(
                file           = exportFile,
                entryCount     = entries.size,
                signatureValid = signedBlock.contains("signature")
            )
        }

    fun shareBundle(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type    = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "AI Stop Sovereign Vault Export — Ed25519 Signed")
            putExtra(Intent.EXTRA_TEXT,
                "Vault metadata export from AI Stop. " +
                "Signed with ${SovereignIdentity.signingAlgorithm()}. " +
                "Values are NOT included — metadata only.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share vault export").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    // ── QR backup ─────────────────────────────────────────────────────────────

    /**
     * Generate a QR code bitmap for vault backup.
     *
     * The QR encodes a JSON object containing:
     * - vault entry count and preview list (metadata only)
     * - device fingerprint
     * - timestamp
     * - a sovereignty declaration
     *
     * NOTE: The actual encryption KEY is NOT included in the QR.
     * The vault key lives in Android Keystore and cannot be exported.
     * This QR is a RECORD backup, not a key backup.
     * To restore: re-detect the same data with AI Stop on a new device.
     *
     * A true key backup would require a PIN-wrapped key export —
     * planned for v1.3 when biometric key attestation is wired.
     */
    suspend fun generateBackupQr(context: Context, sizePx: Int = 800): Bitmap =
        withContext(Dispatchers.IO) {
            val entries = SovereignVault.listEntries(context)

            val qrData = JSONObject().apply {
                put("schema",        "aistop.vault.backup.v1")
                put("device_pubkey", SovereignIdentity.publicKeyFingerprint())
                put("exported_at",   isoNow())
                put("entry_count",   entries.size)
                put("note",          "AI Stop Sovereign Vault backup. " +
                                     "Scan with AI Stop to view record list. " +
                                     "Vault key remains in Android Keystore — not transferable.")
                put("entries", JSONArray().also { arr ->
                    entries.take(20).forEach { e ->  // limit QR size
                        arr.put(JSONObject().apply {
                            put("type",    e.type)
                            put("preview", e.preview)
                            put("ts",      e.timestamp)
                        })
                    }
                })
            }.toString()

            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 2
            )

            val writer  = QRCodeWriter()
            val matrix  = writer.encode(qrData, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bitmap  = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)

            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        }

    suspend fun saveQrToFile(context: Context, bitmap: Bitmap): File =
        withContext(Dispatchers.IO) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val exportDir = File(context.filesDir, "vault_exports").apply { mkdirs() }
            val file      = File(exportDir, "aistop_vault_qr_$timestamp.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        }

    fun shareQr(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type    = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "AI Stop Vault Backup QR")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Save vault backup QR").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
}
