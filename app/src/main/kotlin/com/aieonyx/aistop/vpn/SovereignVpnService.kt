// Copyright (c) 2026 Edison Lepiten / AIEONYX
// AI Stop — Sovereign Shield VPN Engine v2.8
// Uses BlockedDomains + AllowlistManager
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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.LinkedBlockingQueue

class SovereignVpnService : VpnService() {

    companion object {
        private const val TAG              = "SovereignVpnService"
        private const val NOTIF_CHANNEL_ID = "sovereign_shield"
        private const val NOTIF_ID         = 1001
        const val ACTION_START = "com.aieonyx.aistop.vpn.START"
        const val ACTION_STOP  = "com.aieonyx.aistop.vpn.STOP"

        private const val TUN_ADDRESS  = "10.99.0.1"
        private const val TUN_PREFIX   = 24
        private const val UPSTREAM_DNS = "8.8.8.8"
        private const val DNS_PORT     = 53
        private const val MTU          = 1500
        private const val DNS_TIMEOUT  = 4000
    }

    private var tunFd: ParcelFileDescriptor? = null
    private var readJob: Job? = null
    private var writeJob: Job? = null
    private var bridgeJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val writeQueue   = LinkedBlockingQueue<ByteArray>(512)

    private lateinit var dnsFilter: DnsFilter
    private lateinit var aiCreepDetector: AiCreepDetector

    override fun onCreate() {
        super.onCreate()
        dnsFilter       = DnsFilter(applicationContext)
        aiCreepDetector = AiCreepDetector(applicationContext)
        AllowlistManager.loadPersisted(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> { stopVpn(); START_NOT_STICKY }
            else -> {
                startForeground(NOTIF_ID, buildNotification("Sovereign Shield active"))
                startVpn()
                START_STICKY
            }
        }
    }

    override fun onRevoke() { stopVpn() }
    override fun onDestroy() { stopVpn(); serviceScope.cancel(); super.onDestroy() }

    private fun startVpn() {
        if (tunFd != null) return
        Log.i(TAG, "Starting Sovereign Shield v2.8")

        tunFd = Builder()
            .setSession("AI Stop Sovereign Shield")
            .addAddress(TUN_ADDRESS, TUN_PREFIX)
            .addRoute(UPSTREAM_DNS, 32)
            .addDnsServer(UPSTREAM_DNS)
            .setMtu(MTU)
            .addDisallowedApplication(packageName)
            .establish()

        if (tunFd == null) { Log.e(TAG, "Failed to establish tunnel"); stopSelf(); return }

        writeQueue.clear()
        AllowlistManager.startCleanup(serviceScope)

        writeJob = serviceScope.launch(Dispatchers.IO) {
            val out = FileOutputStream(tunFd!!.fileDescriptor)
            while (isActive) {
                try {
                    val pkt = writeQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                    if (pkt != null) out.write(pkt)
                } catch (_: InterruptedException) {
                } catch (e: Exception) { if (isActive) Log.w(TAG, "Write: ${e.message}") }
            }
        }

        readJob = serviceScope.launch(Dispatchers.IO) {
            val inp = FileInputStream(tunFd!!.fileDescriptor)
            val buf = ByteArray(MTU)
            Log.i(TAG, "DNS intercept loop running")

            while (isActive) {
                val n = try { inp.read(buf) }
                        catch (e: Exception) { if (isActive) Log.w(TAG, "Read: ${e.message}"); break }
                if (n < 28) { delay(1); continue }

                val version = (buf[0].toInt() ushr 4) and 0xF
                if (version != 4) continue

                val ihl   = (buf[0].toInt() and 0xF) * 4
                val proto = buf[9].toInt() and 0xFF
                if (proto != 17) continue

                val dstPort = ((buf[ihl+2].toInt() and 0xFF) shl 8) or
                               (buf[ihl+3].toInt() and 0xFF)
                if (dstPort != DNS_PORT) continue

                val dnsOffset = ihl + 8
                val dnsLen    = n - dnsOffset
                if (dnsLen < 12) continue

                val dns     = buf.copyOfRange(dnsOffset, dnsOffset + dnsLen)
                val srcIp   = buf.copyOfRange(12, 16)
                val srcPort = ((buf[ihl].toInt() and 0xFF) shl 8) or
                               (buf[ihl+1].toInt() and 0xFF)

                launch { handleDns(dns, srcIp, srcPort) }
            }
            Log.i(TAG, "DNS intercept loop exited")
        }

        bridgeJob = serviceScope.launch {
            while (isActive) {
                delay(2000)
                VpnDataBridge.update(
                    blockLog   = dnsFilter.getBlockLog(),
                    detections = aiCreepDetector.getDetections(),
                    flows      = FlowTracker.getInstance().snapshot(),
                    totalBytes = FlowTracker.getInstance().totalBytes()
                )
            }
        }
    }

    private suspend fun handleDns(dns: ByteArray, srcIp: ByteArray, srcPort: Int) {
        val hostname = extractHostname(dns)

        // Check allowlist first
        if (hostname != null && AllowlistManager.isAllowed(hostname)) {
            Log.d(TAG, "DNS ALLOWED (user): $hostname")
            forwardDns(dns, srcIp, srcPort, hostname)
            return
        }

        // Check blocklist
        if (hostname != null && isBlocked(hostname)) {
            Log.i(TAG, "DNS BLOCK: $hostname")
            dnsFilter.recordBlock(hostname)
            writeQueue.offer(buildIpUdpPacket(buildNxDomain(dns), srcIp, srcPort))
            return
        }

        forwardDns(dns, srcIp, srcPort, hostname)
    }

    private suspend fun forwardDns(
        dns: ByteArray, srcIp: ByteArray, srcPort: Int, hostname: String?
    ) {
        withContext(Dispatchers.IO) {
            try {
                val sock = DatagramSocket()
                protect(sock)
                sock.soTimeout = DNS_TIMEOUT
                sock.send(DatagramPacket(dns, dns.size,
                    InetAddress.getByName(UPSTREAM_DNS), DNS_PORT))

                val rb = ByteArray(MTU)
                val rp = DatagramPacket(rb, rb.size)
                sock.receive(rp)
                sock.close()

                val resp = rb.copyOf(rp.length)
                writeQueue.offer(buildIpUdpPacket(resp, srcIp, srcPort))

                FlowTracker.getInstance().record(
                    proto   = "UDP",
                    srcIp   = ipStr(srcIp),
                    srcPort = srcPort,
                    dstIp   = UPSTREAM_DNS,
                    dstPort = DNS_PORT,
                    bytes   = resp.size.toLong()
                )
            } catch (e: Exception) {
                Log.w(TAG, "DNS forward failed for $hostname: ${e.message}")
            }
        }
    }

    private fun isBlocked(h: String): Boolean {
        val host = h.trimEnd('.')
        return BlockedDomains.exact.contains(host) ||
               BlockedDomains.suffixes.any { host.endsWith(it) }
    }

    private fun buildIpUdpPacket(payload: ByteArray, dstIp: ByteArray, dstPort: Int): ByteArray {
        val udpLen = 8 + payload.size
        val ipLen  = 20 + udpLen
        val pkt    = ByteArray(ipLen)
        pkt[0] = 0x45.toByte(); pkt[1] = 0
        pkt[2] = (ipLen ushr 8).toByte(); pkt[3] = (ipLen and 0xFF).toByte()
        pkt[4] = 0; pkt[5] = 0; pkt[6] = 0x40.toByte(); pkt[7] = 0
        pkt[8] = 64; pkt[9] = 17; pkt[10] = 0; pkt[11] = 0
        pkt[12] = 8; pkt[13] = 8; pkt[14] = 8; pkt[15] = 8
        System.arraycopy(dstIp, 0, pkt, 16, 4)
        var sum = 0
        for (i in 0 until 20 step 2)
            sum += ((pkt[i].toInt() and 0xFF) shl 8) or (pkt[i+1].toInt() and 0xFF)
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum ushr 16)
        val ck = sum.inv() and 0xFFFF
        pkt[10] = (ck ushr 8).toByte(); pkt[11] = (ck and 0xFF).toByte()
        pkt[20] = (DNS_PORT ushr 8).toByte(); pkt[21] = (DNS_PORT and 0xFF).toByte()
        pkt[22] = (dstPort ushr 8).toByte();  pkt[23] = (dstPort and 0xFF).toByte()
        pkt[24] = (udpLen ushr 8).toByte();   pkt[25] = (udpLen and 0xFF).toByte()
        pkt[26] = 0; pkt[27] = 0
        System.arraycopy(payload, 0, pkt, 28, payload.size)
        return pkt
    }

    private fun extractHostname(dns: ByteArray): String? {
        if (dns.size < 12) return null
        return try {
            val parts = mutableListOf<String>()
            var i = 12
            while (i < dns.size) {
                val len = dns[i].toInt() and 0xFF
                if (len == 0) break
                i++
                if (i + len > dns.size) return null
                parts.add(String(dns, i, len, Charsets.US_ASCII))
                i += len
            }
            if (parts.isEmpty()) null else parts.joinToString(".")
        } catch (e: Exception) { null }
    }

    private fun buildNxDomain(dns: ByteArray): ByteArray {
        val r = dns.copyOf()
        val rd = dns[2].toInt() and 0x01
        val fl = 0x8080 or (rd shl 8) or 0x0003
        r[2] = (fl ushr 8).toByte(); r[3] = (fl and 0xFF).toByte()
        r[6] = 0; r[7] = 1; r[8] = 0; r[9] = 0; r[10] = 0; r[11] = 0
        return r
    }

    private fun ipStr(ip: ByteArray) =
        "${ip[0].toInt() and 0xFF}.${ip[1].toInt() and 0xFF}" +
        ".${ip[2].toInt() and 0xFF}.${ip[3].toInt() and 0xFF}"

    private fun stopVpn() {
        Log.i(TAG, "Stopping Sovereign Shield")
        AllowlistManager.clearSession()
        AllowlistManager.stopCleanup()
        bridgeJob?.cancel(); readJob?.cancel(); writeJob?.cancel()
        bridgeJob = null; readJob = null; writeJob = null
        writeQueue.clear()
        tunFd?.close(); tunFd = null
        VpnDataBridge.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(NOTIF_CHANNEL_ID, "Sovereign Shield",
                NotificationManager.IMPORTANCE_LOW)
                .apply { description = "AI Stop network protection" }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification {
        val stopPi = PendingIntent.getService(this, 0,
            Intent(this, SovereignVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val openPi = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("AI Stop — Sovereign Shield")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openPi)
            .addAction(0, "Stop", stopPi)
            .setOngoing(true)
            .build()
    }
}

enum class Verdict { ALLOW, DROP }
