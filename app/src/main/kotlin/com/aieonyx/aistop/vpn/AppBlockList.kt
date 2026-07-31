// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.vpn

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/**
 * AppBlockList — user-managed list of apps to block from network access
 * when Sovereign Shield VPN is active.
 *
 * Uses VpnService.Builder.addDisallowedApplication() under the hood.
 * When an app is blocked, the VPN cuts all its network traffic.
 * Zero extra RAM — handled by the kernel VPN framework.
 *
 * Persisted to SharedPreferences. Requires VPN restart to take effect.
 */
object AppBlockList {

    private const val PREFS = "aistop_appblocklist"
    private const val KEY   = "blocked_apps"

    data class BlockedApp(
        val packageName: String,
        val label:       String,
        val addedMs:     Long = System.currentTimeMillis()
    )

    // In-memory cache
    private var _blocked = mutableSetOf<String>()

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _blocked   = (prefs.getStringSet(KEY, emptySet()) ?: emptySet()).toMutableSet()
    }

    fun block(context: Context, packageName: String) {
        _blocked.add(packageName)
        save(context)
    }

    fun unblock(context: Context, packageName: String) {
        _blocked.remove(packageName)
        save(context)
    }

    fun isBlocked(packageName: String): Boolean = _blocked.contains(packageName)

    fun getBlockedPackages(): Set<String> = _blocked.toSet()

    fun getBlockedApps(context: Context): List<BlockedApp> {
        val pm = context.packageManager
        return _blocked.mapNotNull { pkg ->
            try {
                val info  = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(info).toString()
                BlockedApp(pkg, label)
            } catch (e: Exception) {
                BlockedApp(pkg, pkg)  // app uninstalled — show package name
            }
        }.sortedBy { it.label }
    }

    /**
     * Returns all installed non-system apps for the picker UI.
     * Excludes AI Stop itself.
     */
    fun getInstalledApps(context: Context): List<BlockedApp> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { info ->
                // User-installed apps only
                (info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                info.packageName != context.packageName
            }
            .mapNotNull { info ->
                try {
                    val label = pm.getApplicationLabel(info).toString()
                    BlockedApp(info.packageName, label)
                } catch (e: Exception) { null }
            }
            .sortedBy { it.label }
    }

    private fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY, _blocked)
            .apply()
    }
}
