// Copyright (c) 2026 Edison Lepiten / AIEONYX
// AI Stop — Sovereign Shield: Packet Interceptor
// Package: com.aieonyx.aistop

package com.aieonyx.aistop.vpn

import android.content.Context
import android.util.Log
import java.nio.ByteBuffer

/**
 * Parses raw IP packets off the TUN fd.
 *
 * IPv4 layout (20-byte fixed header):
 *   [0]    Version (4) + IHL
 *   [1]    DSCP + ECN
 *   [2-3]  Total length
 *   [4-5]  Identification
 *   [6-7]  Flags + Fragment offset
 *   [8]    TTL
 *   [9]    Protocol  (17 = UDP, 6 = TCP)
 *   [10-11] Header checksum
 *   [12-15] Src IP
 *   [16-19] Dst IP
 *   [20+]  Payload
 *
 * UDP header (8 bytes after IP header):
 *   [0-1]  Src port
 *   [2-3]  Dst port
 *   [4-5]  Length
 *   [6-7]  Checksum
 *   [8+]   Data
 */
class PacketInterceptor(private val context: Context) {

    companion object {
        private const val TAG = "PacketInterceptor"

        // Protocol numbers
        private const val PROTO_TCP: Byte = 6
        private const val PROTO_UDP: Byte = 17

        // Well-known ports
        private const val PORT_DNS   = 53
        private const val PORT_HTTPS = 443
        private const val PORT_HTTP  = 80

        // IPv4 version nibble
        private const val IPV4_VERSION = 4
        private const val IPV6_VERSION = 6
    }

    private val dnsFilter      = DnsFilter(context)
    private val aiCreepDetector = AiCreepDetector(context)
    private val flowTracker    = FlowTracker.getInstance()

    // Holds a synthetic DNS response to be written back to TUN after a DROP verdict
    private var pendingSynthetic: ByteArray? = null

    fun pendingSyntheticResponse(): ByteArray? {
        val r = pendingSynthetic
        pendingSynthetic = null
        return r
    }

    /**
     * Main entry point. Returns ALLOW or DROP.
     * Side-effects: populates pendingSyntheticResponse for DNS blocks,
     * updates FlowTracker for all passing packets.
     */
    fun inspect(buf: ByteBuffer, len: Int): Verdict {
        if (len < 20) return Verdict.ALLOW  // too short to be a valid IP packet

        val raw = buf.array()
        val versionIhl = raw[0].toInt() and 0xFF
        val version = versionIhl ushr 4

        return when (version) {
            IPV4_VERSION -> inspectIpv4(raw, len)
            IPV6_VERSION -> Verdict.ALLOW   // IPv6: passthrough for now (V-2 extension)
            else -> Verdict.ALLOW
        }
    }

    // ── IPv4 ──────────────────────────────────────────────────────────────────

    private fun inspectIpv4(raw: ByteArray, len: Int): Verdict {
        val ihl = (raw[0].toInt() and 0x0F) * 4   // header length in bytes
        if (len < ihl + 8) return Verdict.ALLOW    // need at least UDP header

        val protocol = raw[9]
        val srcIp = readIp(raw, 12)
        val dstIp = readIp(raw, 16)

        return when (protocol) {
            PROTO_UDP -> inspectUdp(raw, ihl, len, srcIp, dstIp)
            PROTO_TCP -> inspectTcp(raw, ihl, len, srcIp, dstIp)
            else      -> Verdict.ALLOW
        }
    }

    // ── UDP ───────────────────────────────────────────────────────────────────

    private fun inspectUdp(
        raw: ByteArray, ipHeaderLen: Int, totalLen: Int,
        srcIp: String, dstIp: String
    ): Verdict {
        val udpOffset = ipHeaderLen
        val srcPort = readUInt16(raw, udpOffset)
        val dstPort = readUInt16(raw, udpOffset + 2)
        val dataOffset = udpOffset + 8
        val dataLen    = totalLen - dataOffset

        // Track flow regardless of verdict
        flowTracker.record(
            proto = "UDP",
            srcIp = srcIp, srcPort = srcPort,
            dstIp = dstIp, dstPort = dstPort,
            bytes = dataLen.toLong()
        )

        return if (dstPort == PORT_DNS) {
            // Hand off to DnsFilter
            val dnsPayload = raw.copyOfRange(dataOffset, dataOffset + dataLen)
            val result = dnsFilter.inspect(dnsPayload, raw, ipHeaderLen, totalLen)
            if (result.verdict == Verdict.DROP) {
                pendingSynthetic = result.syntheticPacket
            }
            result.verdict
        } else {
            Verdict.ALLOW
        }
    }

    // ── TCP ───────────────────────────────────────────────────────────────────

    private fun inspectTcp(
        raw: ByteArray, ipHeaderLen: Int, totalLen: Int,
        srcIp: String, dstIp: String
    ): Verdict {
        val tcpOffset = ipHeaderLen
        if (totalLen < tcpOffset + 20) return Verdict.ALLOW

        val srcPort = readUInt16(raw, tcpOffset)
        val dstPort = readUInt16(raw, tcpOffset + 2)
        val dataOffsetNibble = (raw[tcpOffset + 12].toInt() ushr 4) and 0xF
        val tcpHeaderLen = dataOffsetNibble * 4
        val dataOffset   = tcpOffset + tcpHeaderLen
        val dataLen      = totalLen - dataOffset

        flowTracker.record(
            proto = "TCP",
            srcIp = srcIp, srcPort = srcPort,
            dstIp = dstIp, dstPort = dstPort,
            bytes = dataLen.toLong()
        )

        // TLS ClientHello SNI extraction (port 443, first byte 0x16 = TLS handshake)
        if (dstPort == PORT_HTTPS && dataLen > 5 && raw.getOrNull(dataOffset) == 0x16.toByte()) {
            val sni = extractSni(raw, dataOffset, dataLen)
            if (sni != null) {
                Log.d(TAG, "SNI: $sni  [$srcIp:$srcPort → $dstIp:$dstPort]")
                val creepVerdict = aiCreepDetector.inspect(sni, srcIp, dstIp)
                if (creepVerdict == Verdict.DROP) {
                    Log.i(TAG, "AI Creep blocked: $sni")
                    return Verdict.DROP
                }
            }
        }

        return Verdict.ALLOW
    }

    // ── TLS SNI extraction ────────────────────────────────────────────────────

    /**
     * Minimal TLS ClientHello parser to extract the SNI extension.
     * Layout: TLS Record (5) + Handshake header (4) + ClientHello fields + Extensions
     */
    private fun extractSni(raw: ByteArray, offset: Int, len: Int): String? {
        return try {
            var i = offset
            val end = offset + len

            // TLS Record header: content-type(1) + version(2) + length(2)
            if (i + 5 > end) return null
            i += 5

            // Handshake header: type(1) + length(3)
            if (i + 4 > end) return null
            if (raw[i] != 0x01.toByte()) return null  // must be ClientHello
            i += 4

            // ClientHello: version(2) + random(32) + session_id_len(1) + session_id
            if (i + 35 > end) return null
            i += 34  // version + random
            val sessionIdLen = raw[i++].toInt() and 0xFF
            i += sessionIdLen

            // Cipher suites
            if (i + 2 > end) return null
            val cipherLen = readUInt16(raw, i); i += 2 + cipherLen

            // Compression methods
            if (i + 1 > end) return null
            val comprLen = raw[i++].toInt() and 0xFF; i += comprLen

            // Extensions
            if (i + 2 > end) return null
            val extTotal = readUInt16(raw, i); i += 2
            val extEnd = i + extTotal

            while (i + 4 <= extEnd && i + 4 <= end) {
                val extType = readUInt16(raw, i); i += 2
                val extLen  = readUInt16(raw, i); i += 2
                if (extType == 0x0000) {  // server_name extension
                    // server_name_list length(2) + entry type(1) + name length(2) + name
                    if (i + 5 > end) return null
                    i += 3  // list length + type
                    val nameLen = readUInt16(raw, i); i += 2
                    if (i + nameLen > end) return null
                    return String(raw, i, nameLen, Charsets.US_ASCII)
                } else {
                    i += extLen
                }
            }
            null
        } catch (e: Exception) {
            null  // malformed packet — allow through
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun readUInt16(buf: ByteArray, offset: Int): Int =
        ((buf[offset].toInt() and 0xFF) shl 8) or (buf[offset + 1].toInt() and 0xFF)

    private fun readIp(buf: ByteArray, offset: Int): String =
        "${buf[offset].toInt() and 0xFF}.${buf[offset+1].toInt() and 0xFF}" +
        ".${buf[offset+2].toInt() and 0xFF}.${buf[offset+3].toInt() and 0xFF}"
}
