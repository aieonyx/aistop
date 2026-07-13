// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
//
// EdisonDbExposureDao — implements ExposureDao using EdisonDB mobile SDK.
// Drop-in replacement for Room ExposureDao_Impl.
// Every write tagged with ARPi provenance header (tier=Critical).

package com.aieonyx.aistop.db

import com.aieonyx.edisondb.ArpiHeader
import com.aieonyx.edisondb.EdisonDbAndroid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class EdisonDbExposureDao : ExposureDao {

    // ─── Key conventions ──────────────────────────────────────────────────────
    // Record:   "ev:<id>"         → JSON payload
    // Counter:  "meta:seq"        → next auto-increment id (Long as string)
    // Manifest: "meta:manifest"   → JSON array of id strings (newest first)

    companion object {
        private const val KEY_SEQ      = "meta:seq"
        private const val KEY_MANIFEST = "meta:manifest"
        private const val PREFIX_EV    = "ev:"
    }

    // ─── Auto-increment ID ────────────────────────────────────────────────────

    private suspend fun nextId(): Long = withContext(Dispatchers.IO) {
        val current = EdisonDbAndroid.query(KEY_SEQ)?.toLongOrNull() ?: 0L
        val next = current + 1L
        EdisonDbAndroid.insert(KEY_SEQ, next.toString(), tier = 2)
        next
    }

    // ─── Manifest helpers ─────────────────────────────────────────────────────

    private fun readManifest(): MutableList<String> {
        val raw = EdisonDbAndroid.query(KEY_MANIFEST) ?: return mutableListOf()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { arr.getString(it) }.toMutableList()
    }

    private fun writeManifest(ids: List<String>) {
        EdisonDbAndroid.insert(KEY_MANIFEST, JSONArray(ids).toString(), tier = 2)
    }

    // ─── JSON serialisation ───────────────────────────────────────────────────

    private fun ExposureEvent.toJson(): String = JSONObject().apply {
        put("id",          id)
        put("ts",          ts)
        put("packageName", packageName)
        put("appLabel",    appLabel)
        put("eventType",   eventType)
        put("preview20",   preview20)
        put("trustScore",  trustScore)
        put("piiClasses",  piiClasses)
    }.toString()

    private fun JSONObject.toEvent(): ExposureEvent = ExposureEvent(
        id          = getLong("id"),
        ts          = getLong("ts"),
        packageName = getString("packageName"),
        appLabel    = getString("appLabel"),
        eventType   = getString("eventType"),
        preview20   = getString("preview20"),
        trustScore  = getInt("trustScore"),
        piiClasses  = getString("piiClasses"),
    )

    // ─── ExposureDao implementation ───────────────────────────────────────────

    override suspend fun insert(event: ExposureEvent): Long = withContext(Dispatchers.IO) {
        val id = nextId()
        val stored = event.copy(id = id)
        val json = stored.toJson()

        // ARPi header — tier Critical (0) for all AI exposure events
        val header = ArpiHeader.build(json, tier = 0)
        EdisonDbAndroid.insert("$PREFIX_EV$id", json, header)

        // Prepend to manifest (newest first)
        val manifest = readManifest()
        manifest.add(0, id.toString())
        writeManifest(manifest)

        id
    }

    override fun observeRecent(sinceTs: Long): Flow<List<ExposureEvent>> = flow {
        val manifest = readManifest()
        val events = manifest.mapNotNull { idStr ->
            EdisonDbAndroid.query("$PREFIX_EV$idStr")
                ?.let { JSONObject(it).toEvent() }
                ?.takeIf { it.ts > sinceTs }
        }
        emit(events) // already newest-first from manifest order
    }.flowOn(Dispatchers.IO)

    override suspend fun getAllForExport(): List<ExposureEvent> = withContext(Dispatchers.IO) {
        readManifest().mapNotNull { idStr ->
            EdisonDbAndroid.query("$PREFIX_EV$idStr")
                ?.let { JSONObject(it).toEvent() }
        }
    }

    override suspend fun countByType(type: String, sinceTs: Long): Int =
        withContext(Dispatchers.IO) {
            readManifest().count { idStr ->
                EdisonDbAndroid.query("$PREFIX_EV$idStr")
                    ?.let { JSONObject(it).toEvent() }
                    ?.let { it.eventType == type && it.ts > sinceTs } == true
            }
        }

    override suspend fun countBlockedToday(sinceTs: Long): Int =
        withContext(Dispatchers.IO) {
            readManifest().count { idStr ->
                EdisonDbAndroid.query("$PREFIX_EV$idStr")
                    ?.let { JSONObject(it).toEvent() }
                    ?.let {
                        (it.eventType == EventType.PASTE_BLOCKED ||
                         it.eventType == EventType.PASTE_REDACTED) &&
                        it.ts > sinceTs
                    } == true
            }
        }

    override suspend fun countScrubsToday(sinceTs: Long): Int =
        withContext(Dispatchers.IO) {
            readManifest().count { idStr ->
                EdisonDbAndroid.query("$PREFIX_EV$idStr")
                    ?.let { JSONObject(it).toEvent() }
                    ?.let { it.eventType == EventType.SCRUB_SHARE && it.ts > sinceTs } == true
            }
        }

    override suspend fun purgeOlderThan(cutoffTs: Long): Int =
        withContext(Dispatchers.IO) {
            val manifest = readManifest()
            var purged = 0
            val surviving = mutableListOf<String>()
            for (idStr in manifest) {
                val event = EdisonDbAndroid.query("$PREFIX_EV$idStr")
                    ?.let { JSONObject(it).toEvent() }
                if (event != null && event.ts < cutoffTs) {
                    EdisonDbAndroid.delete("$PREFIX_EV$idStr")
                    purged++
                } else {
                    surviving.add(idStr)
                }
            }
            if (purged > 0) writeManifest(surviving)
            purged
        }

    override suspend fun totalCount(): Int = withContext(Dispatchers.IO) {
        readManifest().size
    }
}
