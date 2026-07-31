// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.vpn

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

/**
 * AllowlistManager — timed user allowlist for Sovereign Shield DNS filter.
 *
 * Tiers:
 *   TIMED    → in-memory, expires at [expiryMs]. Cleared on reboot.
 *   SESSION  → in-memory, cleared when VPN stops.
 *   PERMANENT → persisted to SharedPreferences (EdisonDB integration future).
 *
 * DnsFilter checks this before the blocklist.
 */
object AllowlistManager {

    private const val TAG   = "AllowlistManager"
    private const val PREFS = "aistop_allowlist"

    enum class Duration(val label: String, val ms: Long) {
        TWO_MIN   ("2 minutes",   2  * 60_000L),
        FIVE_MIN  ("5 minutes",   5  * 60_000L),
        THIRTY_MIN("30 minutes",  30 * 60_000L),
        ONE_HOUR  ("1 hour",      60 * 60_000L),
        SESSION   ("This session", -1L),          // -1 = until VPN stops
        PERMANENT ("Permanent",   Long.MAX_VALUE)  // persisted
    }

    data class AllowEntry(
        val hostname: String,
        val duration: Duration,
        val expiryMs: Long,      // System.currentTimeMillis() + duration.ms; -1 = session
        val addedMs:  Long = System.currentTimeMillis()
    ) {
        val isExpired: Boolean get() = when {
            duration == Duration.SESSION  -> false  // managed externally
            duration == Duration.PERMANENT -> false
            else -> System.currentTimeMillis() > expiryMs
        }

        fun remainingMs(): Long = when {
            duration == Duration.SESSION   -> -1L
            duration == Duration.PERMANENT -> Long.MAX_VALUE
            else -> (expiryMs - System.currentTimeMillis()).coerceAtLeast(0L)
        }

        fun remainingLabel(): String = when {
            duration == Duration.SESSION   -> "SESSION"
            duration == Duration.PERMANENT -> "PERMANENT"
            else -> {
                val ms = remainingMs()
                val m  = ms / 60_000
                val s  = (ms % 60_000) / 1_000
                "%d:%02d".format(m, s)
            }
        }
    }

    // In-memory store: hostname → entry
    private val entries = mutableMapOf<String, AllowEntry>()

    // Cleanup job
    private var cleanupJob: Job? = null

    // ── Public API ────────────────────────────────────────────────────────────

    fun allow(context: Context, hostname: String, duration: Duration) {
        val expiry = when (duration) {
            Duration.SESSION   -> -1L
            Duration.PERMANENT -> Long.MAX_VALUE
            else               -> System.currentTimeMillis() + duration.ms
        }
        val entry = AllowEntry(hostname, duration, expiry)
        entries[hostname] = entry
        Log.i(TAG, "Allowed: $hostname for ${duration.label}")

        if (duration == Duration.PERMANENT) {
            persistEntry(context, hostname)
        }
    }

    fun revoke(context: Context, hostname: String) {
        entries.remove(hostname)
        removePersistedEntry(context, hostname)
        Log.i(TAG, "Revoked: $hostname")
    }

    fun isAllowed(hostname: String): Boolean {
        val entry = entries[hostname] ?: return false
        if (entry.isExpired) {
            entries.remove(hostname)
            return false
        }
        return true
    }

    fun getEntry(hostname: String): AllowEntry? {
        val entry = entries[hostname] ?: return null
        if (entry.isExpired) { entries.remove(hostname); return null }
        return entry
    }

    fun getAllEntries(): List<AllowEntry> {
        // Prune expired
        entries.entries.removeIf { it.value.isExpired }
        return entries.values.toList().sortedBy { it.addedMs }
    }

    /** Call when VPN stops — removes SESSION entries */
    fun clearSession() {
        entries.entries.removeIf { it.value.duration == Duration.SESSION }
        Log.i(TAG, "Session allowlist cleared")
    }

    /** Load permanent entries from SharedPreferences on startup */
    fun loadPersisted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("permanent", emptySet()) ?: emptySet()
        saved.forEach { hostname ->
            entries[hostname] = AllowEntry(
                hostname  = hostname,
                duration  = Duration.PERMANENT,
                expiryMs  = Long.MAX_VALUE
            )
        }
        Log.i(TAG, "Loaded ${saved.size} permanent allowlist entries")
    }

    /** Start background cleanup of expired timed entries */
    fun startCleanup(scope: CoroutineScope) {
        cleanupJob = scope.launch {
            while (isActive) {
                delay(30_000)  // check every 30s
                val expired = entries.entries
                    .filter { it.value.isExpired }
                    .map { it.key }
                expired.forEach {
                    entries.remove(it)
                    Log.i(TAG, "Allowlist expired: $it")
                }
            }
        }
    }

    fun stopCleanup() { cleanupJob?.cancel(); cleanupJob = null }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun persistEntry(context: Context, hostname: String) {
        val prefs   = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getStringSet("permanent", mutableSetOf()) ?: mutableSetOf()
        val updated = current.toMutableSet().also { it.add(hostname) }
        prefs.edit().putStringSet("permanent", updated).apply()
    }

    private fun removePersistedEntry(context: Context, hostname: String) {
        val prefs   = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getStringSet("permanent", mutableSetOf()) ?: mutableSetOf()
        val updated = current.toMutableSet().also { it.remove(hostname) }
        prefs.edit().putStringSet("permanent", updated).apply()
    }
}
