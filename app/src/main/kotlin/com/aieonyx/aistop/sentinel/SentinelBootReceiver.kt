// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.sentinel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * SentinelBootReceiver — auto-restarts ClipboardSentinelService after device reboot.
 * Only starts if the user had previously enabled the sentinel.
 */
class SentinelBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("aistop_prefs", Context.MODE_PRIVATE)
        val sentinelEnabled = prefs.getBoolean("sentinel_enabled", false)
        if (sentinelEnabled) {
            ClipboardSentinelService.start(context)
        }
    }
}
