// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * M4 ExposureLog — Room database.
 * Single instance via companion object.
 * Default retention: 30 days (user-configurable in paid tier).
 * Auto-purge runs on every database open.
 *
 * v2.0 upgrade path: swap Room for EdisonDB mobile backend
 * by replacing this class — ExposureDao interface stays identical.
 */
@Database(
    entities  = [ExposureEvent::class],
    version   = 1,
    exportSchema = false
)
abstract class ExposureDatabase : RoomDatabase() {

    abstract fun exposureDao(): ExposureDao

    companion object {
        private const val DB_NAME = "aistop_exposure.db"
        val RETENTION_MS = TimeUnit.DAYS.toMillis(30)

        @Volatile
        private var INSTANCE: ExposureDatabase? = null

        fun getInstance(context: Context): ExposureDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExposureDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()

                // Auto-purge old events on open
                CoroutineScope(Dispatchers.IO).launch {
                    val cutoff = System.currentTimeMillis() - RETENTION_MS
                    instance.exposureDao().purgeOlderThan(cutoff)
                }

                INSTANCE = instance
                instance
            }
        }

        /** Convenience: log a scrub share event. */
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
