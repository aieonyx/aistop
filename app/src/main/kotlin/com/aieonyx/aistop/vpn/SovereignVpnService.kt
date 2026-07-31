// Copyright (c) 2026 Edison Lepiten / AIEONYX
// AI Stop — Sovereign Shield VPN Engine
// Package: com.aieonyx.aistop

package com.aieonyx.aistop.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aieonyx.aistop.ui.MainActivity
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class SovereignVpnService : VpnService() {

    companion object {
        private const val TAG = "SovereignVpnService"
        private const val NOTIF_CHANNEL_ID = "sovereign_shield"
        private const val NOTIF_ID = 1001
        const val ACTION_START = "com.aieonyx.aistop.vpn.START"
        const val ACTION_STOP  = "com.aieonyx.aistop.vpn.STOP"

        private const val TUN_ADDRESS = "10.99.0.1"
        private const val TUN_PREFIX  = 24
        private const val DNS_OVERRIDE = "10.99.0.2"
        private const val MTU = 1500
    }

    private var tunFd: ParcelFileDescriptor? = null
    private var interceptorJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var packetInterceptor: PacketInterceptor

    override fun onCreate() {
        super.onCreate()
        packetInterceptor = PacketInterceptor(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIF_ID, buildNotification("Sovereign Shield active"))
                startVpn()
                START_STICKY
            }
        }
    }

    override fun onRevoke() { stopVpn() }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startVpn() {
        if (tunFd != null) return
        Log.i(TAG, "Starting Sovereign Shield VPN tunnel")

        tunFd = Builder()
            .setSession("AI Stop Sovereign Shield")
            .addAddress(TUN_ADDRESS, TUN_PREFIX)
            .addDnsServer(DNS_OVERRIDE)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .setMtu(MTU)
            .addDisallowedApplication(packageName)
            .establish()

        if (tunFd == null) {
            Log.e(TAG, "Failed to establish VPN tunnel")
            stopSelf()
            return
        }

        interceptorJob = serviceScope.launch { runInterceptLoop() }
    }

    private fun stopVpn() {
        Log.i(TAG, "Stopping Sovereign Shield VPN tunnel")
        interceptorJob?.cancel()
        interceptorJob = null
        tunFd?.close()
        tunFd = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun runInterceptLoop() {
        val tun = tunFd ?: return
        val inputStream  = java.io.FileInputStream(tun.fileDescriptor)
        val outputStream = java.io.FileOutputStream(tun.fileDescriptor)
        val forwardChannel = DatagramChannel.open().also { protect(it.socket()) }

        val readBuf = ByteBuffer.allocate(MTU)

        Log.i(TAG, "Packet intercept loop running")

        // Use the coroutine scope's isActive instead of the suspend context
        while (serviceScope.isActive) {
            readBuf.clear()
            val bytesRead = inputStream.read(readBuf.array())
            if (bytesRead <= 0) {
                delay(1)
                continue
            }
            readBuf.limit(bytesRead)

            val verdict = packetInterceptor.inspect(readBuf, bytesRead)

            when (verdict) {
                Verdict.DROP -> {
                    val syntheticResponse = packetInterceptor.pendingSyntheticResponse()
                    if (syntheticResponse != null) {
                        outputStream.write(syntheticResponse)
                    }
                }
                Verdict.ALLOW -> {
                    outputStream.write(readBuf.array(), 0, bytesRead)
                }
            }
        }

        forwardChannel.close()
        Log.i(TAG, "Packet intercept loop exited")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Sovereign Shield",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "AI Stop network protection" }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = Intent(this, SovereignVpnService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("AI Stop — Sovereign Shield")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)  // system icon until ic_shield added
            .setContentIntent(openPending)
            .addAction(0, "Stop", stopPending)
            .setOngoing(true)
            .build()
    }
}

enum class Verdict { ALLOW, DROP }
