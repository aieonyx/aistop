// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.vpn

import android.util.Log
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class SovereignDnsResolver(
    private val dnsFilter: DnsFilter,
    private val upstreamIp: String = "1.1.1.1",
    private val upstreamPort: Int  = 53,
    private val listenPort: Int    = 5353
) {
    companion object {
        private const val TAG      = "SovereignDnsResolver"
        private const val BUF_SIZE = 512
        private const val TIMEOUT_MS = 3000
    }

    private var job: Job? = null
    private var socket: DatagramSocket? = null

    fun start(scope: CoroutineScope) {
        Log.i(TAG, "Starting sovereign DNS resolver on port $listenPort")
        socket = DatagramSocket(listenPort).apply { soTimeout = TIMEOUT_MS }
        job = scope.launch(Dispatchers.IO) {
            runResolverLoop()
        }
    }

    fun stop() {
        Log.i(TAG, "Stopping sovereign DNS resolver")
        job?.cancel()
        socket?.close()
        socket = null
    }

    private suspend fun CoroutineScope.runResolverLoop() {
        val sock = socket ?: return
        val buf  = ByteArray(BUF_SIZE)

        while (isActive) {
            try {
                val packet = DatagramPacket(buf, buf.size)
                sock.receive(packet)

                val queryBytes    = packet.data.copyOf(packet.length)
                val clientAddress = packet.address
                val clientPort    = packet.port

                launch {
                    handleQuery(sock, queryBytes, clientAddress, clientPort)
                }

            } catch (e: java.net.SocketTimeoutException) {
                // normal idle timeout — loop again
            } catch (e: Exception) {
                if (isActive) Log.w(TAG, "Resolver loop error: ${e.message}")
            }
        }
    }

    private suspend fun handleQuery(
        sock: DatagramSocket,
        query: ByteArray,
        clientAddress: InetAddress,
        clientPort: Int
    ) {
        val hostname = extractHostname(query)

        if (hostname != null) {
            val result = dnsFilter.inspect(
                dnsPayload  = query,
                ipPacket    = ByteArray(0),
                ipHeaderLen = 0,
                totalLen    = 0
            )
            if (result.verdict == Verdict.DROP) {
                val nxdomain = buildNxDomain(query)
                sock.send(DatagramPacket(nxdomain, nxdomain.size, clientAddress, clientPort))
                Log.i(TAG, "DNS SOVEREIGN BLOCK: $hostname")
                return
            }
        }

        val response = forwardToUpstream(query) ?: return
        sock.send(DatagramPacket(response, response.size, clientAddress, clientPort))
    }

    private fun forwardToUpstream(query: ByteArray): ByteArray? {
        return try {
            DatagramSocket().use { upstream ->
                upstream.soTimeout = TIMEOUT_MS
                val addr = InetAddress.getByName(upstreamIp)
                upstream.send(DatagramPacket(query, query.size, addr, upstreamPort))
                val respBuf = ByteArray(BUF_SIZE)
                val respPkt = DatagramPacket(respBuf, respBuf.size)
                upstream.receive(respPkt)
                respBuf.copyOf(respPkt.length)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Upstream DNS failed: ${e.message}")
            null
        }
    }

    private fun buildNxDomain(query: ByteArray): ByteArray {
        if (query.size < 12) return query
        val response = query.copyOf()
        val rdBit = (query[2].toInt() and 0x01)
        val flags = 0x8080 or (rdBit shl 8) or 0x0003
        response[2] = (flags ushr 8).toByte()
        response[3] = (flags and 0xFF).toByte()
        response[6] = 0; response[7] = 0
        response[8] = 0; response[9] = 0
        response[10] = 0; response[11] = 0
        return response
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
}
