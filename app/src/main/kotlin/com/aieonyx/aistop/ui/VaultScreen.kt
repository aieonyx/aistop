// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import com.aieonyx.aistop.security.BiometricGate
import com.aieonyx.aistop.vault.VaultExportManager
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.aieonyx.aistop.ui.theme.AiStopTheme
import com.aieonyx.aistop.vault.SovereignVault
import java.text.SimpleDateFormat
import java.util.*

/**
 * VaultScreen — biometric-gated view of encrypted vault entries.
 *
 * Shown inside MoreScreen as a section.
 * Requires biometric/PIN to unlock and view entries.
 * Each item: tap to copy (decrypted) to clipboard for 30 seconds,
 * then auto-clears.
 */
@Composable
fun VaultScreen() {
    val context  = LocalContext.current
    val colors   = AiStopTheme.colors
    val typo     = AiStopTheme.typography
    val activity = context as? FragmentActivity

    val scope         = rememberCoroutineScope()
    var unlocked      by remember { mutableStateOf(false) }
    var exporting     by remember { mutableStateOf(false) }
    var showQr        by remember { mutableStateOf(false) }
    var qrBitmap      by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var authFailed    by remember { mutableStateOf(false) }
    var entries       by remember { mutableStateOf(SovereignVault.listEntries(context)) }
    var copiedId      by remember { mutableStateOf<String?>(null) }
    var copyCountdown by remember { mutableStateOf(0) }

    // Auto-clear clipboard countdown
    LaunchedEffect(copiedId) {
        if (copiedId != null) {
            copyCountdown = 30
            while (copyCountdown > 0) {
                kotlinx.coroutines.delay(1000)
                copyCountdown--
            }
            // Clear clipboard
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("", ""))
            copiedId = null
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Section header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .background(Color(0xFFA78BFA)) // purple = vault color
            )
            Spacer(Modifier.width(10.dp))
            Text("SOVEREIGN VAULT", style = typo.label, color = colors.textPrimary)
            Spacer(Modifier.width(10.dp))
            HorizontalDivider(color = colors.divider, thickness = 1.dp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(
                "${SovereignVault.entryCount(context)} items",
                style = typo.labelSmall,
                color = colors.textSecondary
            )
        }

        if (!unlocked) {
            // ── Locked state ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, Color(0xFFA78BFA).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🔒", fontSize = 36.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "VAULT LOCKED",
                    style = typo.label,
                    color = Color(0xFFA78BFA)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your encrypted sensitive data is protected.\nBiometric or PIN required to access.",
                    style     = typo.caption,
                    color     = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                if (authFailed) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Authentication failed. Try again.",
                        style = typo.caption,
                        color = colors.danger
                    )
                }
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFA78BFA))
                        .clickable {
                            if (activity != null) {
                                BiometricGate.authenticate(
                                    activity    = activity,
                                    actionTitle = "Unlock Sovereign Vault",
                                    subtitle    = "Verify your identity to access encrypted data"
                                ) { result ->
                                    when (result) {
                                        is BiometricGate.AuthResult.Success,
                                        is BiometricGate.AuthResult.NoHardware -> {
                                            unlocked   = true
                                            authFailed = false
                                            entries    = SovereignVault.listEntries(context)
                                        }
                                        else -> authFailed = true
                                    }
                                }
                            } else {
                                // Fallback if not FragmentActivity
                                unlocked = true
                                entries  = SovereignVault.listEntries(context)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔐 UNLOCK VAULT", style = typo.label, color = Color.White)
                }
            }
        } else {
            // ── Unlocked state ──
            if (entries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🛡", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("VAULT IS EMPTY", style = typo.label, color = colors.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "When the Clipboard Sentinel detects sensitive data,\nyou can save it here instead of clearing it.",
                        style     = typo.caption,
                        color     = colors.disabled,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
                ) {
                    entries.forEachIndexed { index, entry ->
                        VaultEntryRow(
                            entry       = entry,
                            isCopied    = copiedId == entry.id,
                            countdown   = if (copiedId == entry.id) copyCountdown else 0,
                            colors      = colors,
                            typo        = typo,
                            onCopy      = {
                                val value = SovereignVault.retrieve(context, entry.id)
                                if (value != null) {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                        as ClipboardManager
                                    cm.setPrimaryClip(
                                        ClipData.newPlainText("AI Stop Vault", value)
                                    )
                                    copiedId = entry.id
                                }
                            },
                            onDelete    = {
                                SovereignVault.delete(context, entry.id)
                                entries = SovereignVault.listEntries(context)
                            }
                        )
                        if (index < entries.size - 1) {
                            HorizontalDivider(color = colors.divider, thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 14.dp))
                        }
                    }
                }
            }

            // Export + QR buttons
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Export signed bundle
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accentPrimary.copy(alpha = if (exporting) 0.5f else 1f))
                        .clickable {
                            if (!exporting) scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                exporting = true
                                try {
                                    val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        VaultExportManager.exportSignedBundle(context)
                                    }
                                    VaultExportManager.shareBundle(context, result.file)
                                } catch (e: Exception) {
                                    android.util.Log.e("AiStop", "Vault export failed: ${e.message}")
                                } finally { exporting = false }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (exporting) "Signing…" else "📤 Export",
                        style = typo.label,
                        color = Color.White
                    )
                }

                // QR backup
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFA78BFA).copy(alpha = if (exporting) 0.5f else 1f))
                        .clickable {
                            if (!exporting) scope.launch {
                                exporting = true
                                try {
                                    val bmp = VaultExportManager.generateBackupQr(context)
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        qrBitmap = bmp
                                        showQr  = true
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("AiStop", "QR failed: ${e.message}")
                                } finally { exporting = false }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("📱 QR Backup", style = typo.label, color = Color.White)
                }
            }

            // QR display
            if (showQr && qrBitmap != null) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.foundation.Image(
                        bitmap             = qrBitmap!!.asImageBitmap(),
                        contentDescription = "Vault backup QR",
                        modifier           = Modifier.size(240.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Scan to view vault record list",
                        style = typo.caption,
                        color = Color.DarkGray
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1A1A2E))
                                .clickable {
                                    scope.launch {
                                        val file = VaultExportManager.saveQrToFile(context, qrBitmap!!)
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            VaultExportManager.shareQr(context, file)
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Save QR", style = typo.label, color = Color.White)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF333333))
                                .clickable { showQr = false }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Close", style = typo.label, color = Color.White)
                        }
                    }
                }
            }

            // Lock button
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                    .clickable { unlocked = false }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🔒 Lock vault", style = typo.label, color = colors.textSecondary)
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun VaultEntryRow(
    entry:     SovereignVault.VaultEntry,
    isCopied:  Boolean,
    countdown: Int,
    colors:    com.aieonyx.aistop.ui.theme.AiStopColors,
    typo:      com.aieonyx.aistop.ui.theme.AiStopTypography,
    onCopy:    () -> Unit,
    onDelete:  () -> Unit
) {
    val fmt  = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val date = fmt.format(Date(entry.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon by type
        val icon = when (entry.piiClass) {
            "ApiKey", "AwsKey", "GitHubToken" -> "🔑"
            "Password"                         -> "🔐"
            "Ssn"                              -> "🪪"
            "CreditCard", "Iban"               -> "💳"
            "CryptoWallet"                     -> "🪙"
            "Phone"                            -> "📱"
            else                               -> "🔒"
        }
        Text(icon, fontSize = 20.sp, modifier = Modifier.width(32.dp))

        Column(Modifier.weight(1f)) {
            Text(
                entry.piiClass.uppercase(),
                style = typo.labelSmall,
                color = Color(0xFFA78BFA)
            )
            Text(entry.preview, style = typo.bodySmall, color = colors.textPrimary)
            Text(date, style = typo.caption, color = colors.textSecondary)
        }

        Spacer(Modifier.width(8.dp))

        if (isCopied) {
            // Countdown
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.warning.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "Clears in ${countdown}s",
                    style = typo.caption,
                    color = colors.warning
                )
            }
        } else {
            // Copy button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFA78BFA).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFA78BFA).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .clickable { onCopy() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("COPY", style = typo.labelSmall, color = Color(0xFFA78BFA))
            }
        }

        Spacer(Modifier.width(6.dp))

        // Delete button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(colors.danger.copy(alpha = 0.1f))
                .clickable { onDelete() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("✕", style = typo.labelSmall, color = colors.danger)
        }
    }
}
