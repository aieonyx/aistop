// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.sentinel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * SentinelBootReceiver — auto-restarts ClipboardSentinelService after device reboot.
 *
 * Android 15+ (API 35): BOOT_COMPLETED cannot start restricted foreground services.
 * Fix: use ACTION_MY_PACKAGE_REPLACED for app updates, and for boot we rely on
 * the Accessibility Service which auto-restarts via system binding.
 * The Clipboard Sentinel is re-enabled when user opens the app post-boot.
 */
class SentinelBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("aistop_prefs", Context.MODE_PRIVATE)
        val sentinelEnabled = prefs.getBoolean("sentinel_enabled", false)
        if (!sentinelEnabled) return

        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // App updated — safe to restart on all Android versions
                if (Build.VERSION.SDK_INT < 35) {
                    ClipboardSentinelService.start(context)
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Android 14 and below — direct start allowed
                if (Build.VERSION.SDK_INT < 35) {
                    ClipboardSentinelService.start(context)
                }
                // Android 15+ — foreground service from BOOT_COMPLETED is restricted
                // Sentinel will restart when user opens AI Stop app
            }
        }
    }
}
