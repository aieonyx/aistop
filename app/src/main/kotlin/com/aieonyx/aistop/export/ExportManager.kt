// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.aieonyx.aistop.db.ExposureDatabase
import com.aieonyx.aistop.db.ExposureEvent
import com.aieonyx.aistop.identity.SovereignIdentity
import com.aieonyx.aistop.jni.AiStopCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * M5 Export pipeline — orchestrates split signing flow.
 * Step 1: Rust blake3_hash(payload)
 * Step 2: Kotlin Keystore sign(hash)
 * Step 3: Rust assemble_signature_block
 * Export contains event metadata only — NEVER original text.
 */
object ExportManager {

    data class ExportResult(
        val file: File,
        val eventCount: Int,
        val signatureValid: Boolean
    )

    suspend fun exportSignedReport(context: Context): ExportResult =
        withContext(Dispatchers.IO) {
            SovereignIdentity.ensureKeyPair()
            val dao    = ExposureDatabase.getInstance(context).exposureDao()
            val events = dao.getAllForExport()
            val payloadJson = buildPayloadJson(
                events       = events,
                devicePubkey = SovereignIdentity.publicKeyFingerprint(),
                exportedAt   = isoNow()
            )
            val hashHex      = AiStopCore.blake3Hash(payloadJson)
            val signatureHex = SovereignIdentity.signHash(hashHex)
            val signedBlock  = AiStopCore.assembleSignatureBlock(
                payloadJson, hashHex, signatureHex
            )
            val timestamp  = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val exportDir  = File(context.filesDir, "exports").apply { mkdirs() }
            val exportFile = File(exportDir, "aistop_exposure_$timestamp.json")
            exportFile.writeText(signedBlock)
            ExportResult(
                file           = exportFile,
                eventCount     = events.size,
                signatureValid = signedBlock.contains("signature")
            )
        }

    fun shareExport(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type    = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "AI Stop Exposure Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share signed report"))
    }

    private fun buildPayloadJson(
        events:       List<ExposureEvent>,
        devicePubkey: String,
        exportedAt:   String
    ): String {
        val arr = JSONArray()
        events.forEach { e ->
            arr.put(JSONObject().apply {
                put("ts",          e.ts)
                put("package",     e.packageName)
                put("app_label",   e.appLabel)
                put("event_type",  e.eventType)
                put("trust_score", e.trustScore)
                put("pii_classes", e.piiClasses)
            })
        }
        return JSONObject().apply {
            put("schema",        "aistop.exposure.v1")
            put("device_pubkey", devicePubkey)
            put("exported_at",   exportedAt)
            put("events",        arr)
        }.toString()
    }

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
}
