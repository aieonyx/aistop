// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.accessibility

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aieonyx.aistop.vpn.AllowlistManager
import com.aieonyx.aistop.vpn.AppBlockList

/**
 * Handles notification action button taps:
 *   ACTION_EXEMPT   — permanently suppress warnings for this app
 *   ACTION_BLOCK    — add app to App Block List (cuts network when Shield ON)
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_EXEMPT    = "com.aieonyx.aistop.NOTIF_EXEMPT"
        const val ACTION_BLOCK_APP = "com.aieonyx.aistop.NOTIF_BLOCK_APP"
        const val EXTRA_PACKAGE    = "pkg"
        const val EXTRA_LABEL      = "label"
        const val EXTRA_DOMAIN     = "domain"
        const val NOTIF_ID         = 2001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pkg    = intent.getStringExtra(EXTRA_PACKAGE)    ?: return
        val label  = intent.getStringExtra(EXTRA_LABEL)      ?: pkg
        val domain = intent.getStringExtra(EXTRA_DOMAIN)     ?: ""

        when (intent.action) {
            ACTION_EXEMPT -> {
                Log.i("NotifActionReceiver", "EXEMPT: $pkg ($label)")
                // Permanently allow the primary domain so AiAppWarning suppresses
                if (domain.isNotEmpty()) {
                    AllowlistManager.allow(context, domain, AllowlistManager.Duration.PERMANENT)
                }
                // Also mark package as exempted so isSuppressed() returns true
                AiAppWarning.exemptPackage(context, pkg)
                // Dismiss the notification
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(NOTIF_ID)
                // Show brief confirmation toast
                android.widget.Toast.makeText(
                    context,
                    "✓ $label exempted — AI Stop won't warn about this app again",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }

            ACTION_BLOCK_APP -> {
                Log.i("NotifActionReceiver", "BLOCK APP: $pkg ($label)")
                AppBlockList.load(context)
                AppBlockList.block(context, pkg)
                // Dismiss the notification
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(NOTIF_ID)
                android.widget.Toast.makeText(
                    context,
                    "🚫 $label blocked — toggle Shield OFF/ON to apply",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
