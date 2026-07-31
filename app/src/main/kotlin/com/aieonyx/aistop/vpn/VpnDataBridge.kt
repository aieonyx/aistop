// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.vpn

/**
 * VpnDataBridge — singleton that SovereignVpnService writes to
 * and ShieldScreen reads from.
 *
 * Decouples the UI from the service lifecycle: ShieldScreen never
 * needs to bind to the service. Data persists across screen navigations
 * and survives brief VPN restarts.
 */
object VpnDataBridge {

    // DNS block log: hostname → total block count
    @Volatile private var _blockLog = mapOf<String, Int>()
    val blockLog: Map<String, Int> get() = _blockLog

    // AI Creep detections (capped at 200)
    @Volatile private var _detections = listOf<AiCreepDetector.Detection>()
    val detections: List<AiCreepDetector.Detection> get() = _detections

    // Network flow snapshot
    @Volatile private var _flows = listOf<FlowTracker.FlowStats>()
    val flows: List<FlowTracker.FlowStats> get() = _flows

    // Traffic history for bar chart: list of (blockedBytes, allowedBytes) per 2-min window
    data class TrafficWindow(
        val label: String,
        val blocked: Long,
        val allowed: Long
    )
    @Volatile private var _trafficHistory = listOf<TrafficWindow>()
    val trafficHistory: List<TrafficWindow> get() = _trafficHistory

    // Total intercepted bytes
    @Volatile var totalIntercepted: Long = 0L
        private set

    // Called by SovereignVpnService every 2 seconds
    fun update(
        blockLog: Map<String, Int>,
        detections: List<AiCreepDetector.Detection>,
        flows: List<FlowTracker.FlowStats>,
        totalBytes: Long
    ) {
        _blockLog = blockLog
        _detections = detections.takeLast(200)
        _flows = flows.take(30)
        totalIntercepted = totalBytes
        appendTrafficWindow(blockLog, flows)
    }

    private fun appendTrafficWindow(
        blockLog: Map<String, Int>,
        flows: List<FlowTracker.FlowStats>
    ) {
        val now = java.text.SimpleDateFormat("H:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        val blockedBytes = blockLog.values.sum().toLong() * 512L  // estimate ~512B per DNS block
        val allowedBytes = flows.sumOf { it.bytes }

        val history = _trafficHistory.toMutableList()
        history.add(TrafficWindow(now, blockedBytes, allowedBytes))
        if (history.size > 12) history.removeAt(0)
        _trafficHistory = history
    }

    fun clear() {
        _blockLog = mapOf()
        _detections = listOf()
        _flows = listOf()
        _trafficHistory = listOf()
        totalIntercepted = 0L
    }
}
