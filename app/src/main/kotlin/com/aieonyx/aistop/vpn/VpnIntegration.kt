// Copyright (c) 2026 Edison Lepiten / AIEONYX
// AI Stop — Sovereign Shield: VPN permission + launch helpers
// Package: com.aieonyx.aistop

package com.aieonyx.aistop.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.result.ActivityResultLauncher

/**
 * VPN permission + lifecycle helpers for MainActivity / SettingsFragment.
 *
 * Usage:
 *   1. In Activity.onCreate(), call VpnIntegration.registerPermissionLauncher(this, registry)
 *   2. To start: VpnIntegration.requestAndStart(this, launcher)
 *   3. To stop:  VpnIntegration.stop(this)
 */
object VpnIntegration {

    /**
     * Request VPN permission if not yet granted, then start SovereignVpnService.
     *
     * @param launcher  ActivityResultLauncher<Intent> registered with
     *                  registerForActivityResult(StartActivityForResult()) { result ->
     *                      if (result.resultCode == Activity.RESULT_OK) start(context)
     *                  }
     */
    fun requestAndStart(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        val intent = VpnService.prepare(activity)
        if (intent == null) {
            // Permission already granted
            start(activity)
        } else {
            // Show Android's "AI Stop wants to set up a VPN connection" dialog
            launcher.launch(intent)
        }
    }

    fun start(context: Context) {
        val intent = Intent(context, SovereignVpnService::class.java)
            .setAction(SovereignVpnService.ACTION_START)
        context.startForegroundService(intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, SovereignVpnService::class.java)
            .setAction(SovereignVpnService.ACTION_STOP)
        context.startService(intent)
    }

    fun isRunning(context: Context): Boolean {
        // Simple check via ActivityManager; can be replaced with a bound-service ping
        val am = context.getSystemService(Context.ACTIVITY_SERVICE)
            as android.app.ActivityManager
        @Suppress("DEPRECATION")
        return am.getRunningServices(Int.MAX_VALUE)
            ?.any { it.service.className == SovereignVpnService::class.java.name }
            ?: false
    }
}

/*
 * ════════════════════════════════════════════════════════════
 *  AndroidManifest.xml additions (inside <application>):
 * ════════════════════════════════════════════════════════════
 *
 *  <!-- VPN permission (declared at top-level, not inside <application>) -->
 *  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
 *  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
 *
 *  <!-- Inside <application> -->
 *  <service
 *      android:name=".vpn.SovereignVpnService"
 *      android:exported="false"
 *      android:permission="android.permission.BIND_VPN_SERVICE"
 *      android:foregroundServiceType="specialUse">
 *      <intent-filter>
 *          <action android:name="android.net.VpnService" />
 *      </intent-filter>
 *      <property
 *          android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
 *          android:value="Blocks AI crawler traffic from leaving the device" />
 *  </service>
 *
 * ════════════════════════════════════════════════════════════
 *  build.gradle (app) — no extra deps needed. VpnService is
 *  in android.net — no Gradle dependency required.
 * ════════════════════════════════════════════════════════════
 */
