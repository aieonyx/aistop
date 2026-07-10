// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * M4 ExposureLog — Room entity.
 * NEVER stores original clipboard text.
 * Only metadata: event type, app, PII classes, 20-char preview, trust score.
 * Encrypted at rest via EncryptedSharedPreferences key + SQLCipher (v2.0).
 * v1.1: AES-256-GCM encryption layer in Rust core handles export signing.
 */
@Entity(tableName = "exposure_events")
data class ExposureEvent(
    @PrimaryKey(autoGenerate = true)
    val id:         Long    = 0,
    val ts:         Long,           // epoch ms
    val packageName: String,        // target AI app package
    val appLabel:   String,         // display name
    val eventType:  String,         // PASTE_ALLOWED|PASTE_BLOCKED|PASTE_REDACTED|SCRUB_SHARE|CLIP_AUTOCLEAR
    val preview20:  String,         // first 20 chars only — NEVER full content
    val trustScore: Int,            // trust score at time of event
    val piiClasses: String,         // JSON array: ["EMAIL","PHONE",...]
)

/** Event type constants — mirrors Rust store::EventType */
object EventType {
    const val PASTE_ALLOWED   = "PASTE_ALLOWED"
    const val PASTE_BLOCKED   = "PASTE_BLOCKED"
    const val PASTE_REDACTED  = "PASTE_REDACTED"
    const val SCRUB_SHARE     = "SCRUB_SHARE"
    const val CLIP_AUTOCLEAR  = "CLIP_AUTOCLEAR"
}
