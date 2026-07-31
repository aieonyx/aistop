// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.sentinel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.aieonyx.aistop.R
import com.aieonyx.aistop.ui.MainActivity

object BankingModeManager {

    private const val CHANNEL_BANKING = "aistop_banking_mode"
    private const val NOTIF_BANKING   = 3001

    val STRICT_PACKAGES = setOf(
        "com.globe.gcash.android",
        "com.maya.app",
    )

    fun isStrict(packageName: String): Boolean = packageName in STRICT_PACKAGES

    fun showBankingModeNotification(context: Context, appName: String) {
        createChannel(context)
        val settingsIntent = PendingIntent.getActivity(
            context, 4001,
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openIntent = PendingIntent.getActivity(
            context, 4002,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_BANKING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠ $appName requires AI Stop to pause")
            .setContentText("Tap 'Pause Guardian' to use $appName, then resume when done.")
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle()
                .bigText("$appName blocks all Accessibility Services. " +
                         "Tap 'Pause Guardian' → disable AI Stop in Accessibility Settings → " +
                         "use $appName → re-enable AI Stop Guardian when done."))
            .addAction(0, "⏸ Pause Guardian", settingsIntent)
            .addAction(0, "Open AI Stop", openIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
        nm.notify(NOTIF_BANKING, notif)
    }

    fun dismissBankingModeNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_BANKING)
    }

    private fun createChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_BANKING,
                "Banking Mode",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifies when a banking app requires AI Stop to pause" }
        )
    }

    fun appLabel(pkg: String): String = when (pkg) {
        "com.globe.gcash.android" -> "GCash"
        "com.maya.app"            -> "Maya"
        else                      -> pkg.substringAfterLast(".")
    }
}
