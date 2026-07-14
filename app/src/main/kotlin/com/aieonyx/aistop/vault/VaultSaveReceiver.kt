// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.vault

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.aieonyx.aistop.R
import com.aieonyx.aistop.accessibility.SovereignAccessibilityService

/**
 * VaultSaveReceiver — handles "Save to Vault" notification action.
 * Encrypts detected PII and stores in SovereignVault.
 */
class VaultSaveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SovereignAccessibilityService.ACTION_SAVE_VAULT) return

        val text     = intent.getStringExtra(SovereignAccessibilityService.EXTRA_TEXT) ?: return
        val piiClass = intent.getStringExtra(SovereignAccessibilityService.EXTRA_PII_CLASS) ?: "Unknown"

        // Create masked preview
        val preview = if (text.length > 8) {
            "${text.take(4)}••••${text.takeLast(4)}"
        } else "••••"

        // Save to vault
        val id = SovereignVault.save(
            context  = context,
            text     = text,
            type     = piiClass.lowercase(),
            piiClass = piiClass,
            preview  = preview
        )

        // Show confirmation notification
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (id.isNotEmpty()) {
            val notif = NotificationCompat.Builder(context, "aistop_autopilot")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("🔐 Saved to Sovereign Vault")
                .setContentText("$piiClass encrypted and stored securely")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            nm.notify(3003, notif)
        }
    }
}
