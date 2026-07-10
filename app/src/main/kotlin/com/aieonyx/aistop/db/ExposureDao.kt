// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * M4 ExposureLog — Room DAO.
 * All queries scoped to 30-day retention window by default.
 * Free tier: view only. Paid tier: export + retention controls.
 */
@Dao
interface ExposureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ExposureEvent): Long

    /** All events within retention window, newest first. */
    @Query("SELECT * FROM exposure_events WHERE ts > :sinceTs ORDER BY ts DESC")
    fun observeRecent(sinceTs: Long): Flow<List<ExposureEvent>>

    /** All events for export (paid tier). */
    @Query("SELECT * FROM exposure_events ORDER BY ts DESC")
    suspend fun getAllForExport(): List<ExposureEvent>

    /** Count events by type — for dashboard metrics. */
    @Query("SELECT COUNT(*) FROM exposure_events WHERE eventType = :type AND ts > :sinceTs")
    suspend fun countByType(type: String, sinceTs: Long): Int

    /** Total blocked + redacted today — dashboard stat. */
    @Query("""
        SELECT COUNT(*) FROM exposure_events 
        WHERE eventType IN ('PASTE_BLOCKED','PASTE_REDACTED') 
        AND ts > :sinceTs
    """)
    suspend fun countBlockedToday(sinceTs: Long): Int

    /** Scrubs today — for free tier 3/day limit. */
    @Query("""
        SELECT COUNT(*) FROM exposure_events 
        WHERE eventType = 'SCRUB_SHARE' 
        AND ts > :sinceTs
    """)
    suspend fun countScrubsToday(sinceTs: Long): Int

    /** Purge events older than retention cutoff. */
    @Query("DELETE FROM exposure_events WHERE ts < :cutoffTs")
    suspend fun purgeOlderThan(cutoffTs: Long): Int

    /** Total event count. */
    @Query("SELECT COUNT(*) FROM exposure_events")
    suspend fun totalCount(): Int
}
