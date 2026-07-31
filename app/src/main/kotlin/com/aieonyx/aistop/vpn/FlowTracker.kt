// Copyright (c) 2026 Edison Lepiten / AIEONYX
// AI Stop — Sovereign Shield: Flow Tracker
// Package: com.aieonyx.aistop

package com.aieonyx.aistop.vpn

import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks network flows (per proto/IP/port tuple) for the network data flow map UI.
 *
 * Thread-safe singleton — written from the IO packet loop,
 * read from the UI coroutine.
 *
 * A "flow" = (proto, srcIp, srcPort, dstIp, dstPort) identified by a 5-tuple key.
 * We accumulate byte counts and packet counts per flow.
 *
 * The UI can call snapshot() to get a point-in-time copy for rendering.
 */
class FlowTracker private constructor() {

    companion object {
        @Volatile private var instance: FlowTracker? = null
        fun getInstance(): FlowTracker =
            instance ?: synchronized(this) {
                instance ?: FlowTracker().also { instance = it }
            }

        private const val MAX_FLOWS = 1000   // cap memory usage
    }

    data class FlowKey(
        val proto: String,
        val srcIp: String, val srcPort: Int,
        val dstIp: String, val dstPort: Int
    )

    data class FlowStats(
        val key: FlowKey,
        var bytes: Long = 0L,
        var packets: Long = 0L,
        var firstSeenMs: Long = System.currentTimeMillis(),
        var lastSeenMs:  Long = System.currentTimeMillis()
    )

    private val flows = ConcurrentHashMap<FlowKey, FlowStats>()

    fun record(
        proto: String,
        srcIp: String, srcPort: Int,
        dstIp: String, dstPort: Int,
        bytes: Long
    ) {
        val key = FlowKey(proto, srcIp, srcPort, dstIp, dstPort)
        val now = System.currentTimeMillis()
        flows.getOrPut(key) {
            if (flows.size >= MAX_FLOWS) evictOldest()
            FlowStats(key)
        }.apply {
            this.bytes   += bytes
            this.packets += 1
            this.lastSeenMs = now
        }
    }

    /** Returns a sorted-by-bytes snapshot for the UI. */
    fun snapshot(): List<FlowStats> =
        flows.values.toList().sortedByDescending { it.bytes }

    /** Returns total bytes seen across all flows. */
    fun totalBytes(): Long = flows.values.sumOf { it.bytes }

    fun clear() = flows.clear()

    private fun evictOldest() {
        val oldest = flows.values.minByOrNull { it.lastSeenMs } ?: return
        flows.remove(oldest.key)
    }
}
