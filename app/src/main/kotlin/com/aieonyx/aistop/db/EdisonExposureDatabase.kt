// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
//
// EdisonExposureDatabase — drop-in replacement for ExposureDatabase (Room).
// Identical companion object API: getInstance(context), RETENTION_MS,
// logScrubShare(). Zero changes required at any call site.
//
// Swap: replace `ExposureDatabase` with `EdisonExposureDatabase` in all
// imports across the 6 call-site files. The .exposureDao() call stays identical.

package com.aieonyx.aistop.db

import android.content.Context
import com.aieonyx.edisondb.EdisonDbAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * EdisonDB-backed exposure log database.
 * Replaces Room ExposureDatabase with sovereign embedded storage.
 * Every write carries an ARPi provenance header (tier=Critical).
 *
 * ARPi = AXON Receptor Protocol Interface — 78-byte provenance header
 * on every EdisonDB write. Tamper-evident. BLAKE3-signed. Monotonic counter.
 */
class EdisonExposureDatabase private constructor(context: Context) {

    private val dao: ExposureDao = EdisonDbExposureDao()

    fun exposureDao(): ExposureDao = dao

    companion object {
        val RETENTION_MS: Long = TimeUnit.DAYS.toMillis(30)

        @Volatile
        private var INSTANCE: EdisonExposureDatabase? = null

        fun getInstance(context: Context): EdisonExposureDatabase {
            return INSTANCE ?: synchronized(this) {
                // Open EdisonDB — idempotent if already open
                EdisonDbAndroid.open(context)

                val instance = EdisonExposureDatabase(context)

                // Auto-purge old events on open (mirrors Room behaviour)
                CoroutineScope(Dispatchers.IO).launch {
                    val cutoff = System.currentTimeMillis() - RETENTION_MS
                    instance.exposureDao().purgeOlderThan(cutoff)
                }

                INSTANCE = instance
                instance
            }
        }

        /** Convenience: log a scrub share event. Identical signature to Room version. */
        suspend fun logScrubShare(
            context: Context,
            preview: String,
            piiClasses: List<String>
        ) {
            val dao = getInstance(context).exposureDao()
            dao.insert(
                ExposureEvent(
                    ts          = System.currentTimeMillis(),
                    packageName = "com.aieonyx.aistop",
                    appLabel    = "Scrub & Share",
                    eventType   = EventType.SCRUB_SHARE,
                    preview20   = preview.take(20),
                    trustScore  = 100,
                    piiClasses  = piiClasses.joinToString(",", "[\"", "\"]") { it }
                )
            )
        }
    }
}
