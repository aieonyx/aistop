// Copyright (c) 2026 Edison Lepiten / AIEONYX
// AI Stop — Sovereign Shield: DNS Filter
// Package: com.aieonyx.aistop

package com.aieonyx.aistop.vpn

import android.content.Context
import android.util.Log

/**
 * DNS-level AI crawler blocking.
 *
 * Intercepts raw UDP DNS query payloads (RFC 1035), extracts the queried
 * hostname, matches against the sovereign blocklist, and either:
 *   • ALLOWS — returns DnsResult(ALLOW, null)
 *   • DROPS  — synthesizes an NXDOMAIN response packet and returns
 *               DnsResult(DROP, syntheticPacket) so SovereignVpnService
 *               can write it back to the TUN fd.
 *
 * DNS packet layout (RFC 1035 §4.1):
 *   Header  (12 bytes): ID(2) FLAGS(2) QDCOUNT(2) ANCOUNT(2) NSCOUNT(2) ARCOUNT(2)
 *   Question section:   QNAME(variable) QTYPE(2) QCLASS(2)
 *
 *   FLAGS for response:
 *     QR=1, OPCODE=0, AA=0, TC=0, RD=copy, RA=1, Z=0, RCODE=3 (NXDOMAIN)
 *     = 0x8183 when RD was set in query
 *     = 0x8083 when RD was not set
 */
class DnsFilter(private val context: Context) {

    companion object {
        private const val TAG = "DnsFilter"
    }

    data class DnsResult(val verdict: Verdict, val syntheticPacket: ByteArray?)

    // ── Blocklist ─────────────────────────────────────────────────────────────
    // Exact domains AND suffix matches (e.g. "openai.com" also blocks "api.openai.com")

    private val aiCrawlerDomains: Set<String> = setOf(
        // OpenAI / ChatGPT crawlers
        "openai.com", "oaistatic.com", "oaiusercontent.com",
        "chatgpt.com", "chat.openai.com",

        // Google AI / Gemini
        "generativelanguage.googleapis.com", "bard.google.com",
        "gemini.google.com", "ai.google.dev", "makersuite.google.com",
        "aistudio.google.com",

        // Anthropic (Claude)
        "claude.ai", "api.anthropic.com", "anthropic.com",

        // Meta AI
        "ai.meta.com", "llama.meta.com",

        // Microsoft / Copilot
        "copilot.microsoft.com", "bing.com", "sydney.bing.com",
        "edgeservices.bing.com",

        // Common AI/ML data collection & crawler infra
        "commoncrawl.org", "cc-index.commoncrawl.org",
        "openwebtext.org",
        "pile.eleuther.ai",
        "laion.ai",
        "huggingface.co",          // model telemetry endpoints
        "api.huggingface.co",
        "datasets-server.huggingface.co",

        // Advertising & tracking doubles as AI training data
        "doubleclick.net", "adservice.google.com",
        "googletagmanager.com", "googletagservices.com",
        "google-analytics.com", "analytics.google.com",
        "facebook.com", "connect.facebook.net",
        "graph.facebook.com",
        "pixel.facebook.com",

        // Perplexity AI
        "perplexity.ai", "www.perplexity.ai",

        // Cohere
        "cohere.ai", "api.cohere.ai",

        // Mistral
        "mistral.ai", "api.mistral.ai",

        // xAI / Grok
        "x.ai", "grok.x.ai",

        // AI telemetry & beacon domains (common pattern)
        "telemetry.ai", "beacon.ai",
        "ml-telemetry.amazonaws.com"
    )

    // Pattern-based suffix match list (cheaper than regex per packet)
    private val aiCrawlerSuffixes: List<String> = listOf(
        ".openai.com",
        ".anthropic.com",
        ".google-ai.com",
        ".gemini.google.com",
        ".openai.azure.com",    // Azure OpenAI
        ".cognitive.microsoft.com",
        ".perplexity.ai",
        ".cohere.ai",
        ".mistral.ai"
    )

    // Runtime block log: hostname → block count (for UI)
    private val blockLog = mutableMapOf<String, Int>()
    fun getBlockLog(): Map<String, Int> = blockLog.toMap()

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * @param dnsPayload   Raw DNS query bytes (UDP payload, after IP+UDP headers)
     * @param ipPacket     Full raw IP packet (for building synthetic response)
     * @param ipHeaderLen  Length of IP header in ipPacket
     * @param totalLen     Total length of ipPacket
     */
    fun inspect(
        dnsPayload: ByteArray,
        ipPacket: ByteArray,
        ipHeaderLen: Int,
        totalLen: Int
    ): DnsResult {
        val hostname = extractQueryHostname(dnsPayload) ?: return DnsResult(Verdict.ALLOW, null)

        if (isBlocked(hostname)) {
            Log.i(TAG, "DNS BLOCK: $hostname")
            blockLog[hostname] = (blockLog[hostname] ?: 0) + 1

            val synthetic = buildNxDomainPacket(dnsPayload, ipPacket, ipHeaderLen, totalLen)
            return DnsResult(Verdict.DROP, synthetic)
        }

        return DnsResult(Verdict.ALLOW, null)
    }

    // ── Blocklist matching ────────────────────────────────────────────────────

    private fun isBlocked(hostname: String): Boolean {
        val h = hostname.trimEnd('.')   // strip trailing dot (FQDN → plain)
        if (aiCrawlerDomains.contains(h)) return true
        for (suffix in aiCrawlerSuffixes) {
            if (h.endsWith(suffix)) return true
        }
        return false
    }

    // ── DNS hostname extraction ───────────────────────────────────────────────

    /**
     * Parses the QNAME field from the DNS question section.
     * QNAME is a sequence of labels: each label is length(1) + chars, terminated by 0x00.
     */
    private fun extractQueryHostname(dns: ByteArray): String? {
        if (dns.size < 12) return null  // need at least the header
        try {
            val parts = mutableListOf<String>()
            var i = 12  // skip 12-byte header
            while (i < dns.size) {
                val labelLen = dns[i].toInt() and 0xFF
                if (labelLen == 0) break
                i++
                if (i + labelLen > dns.size) return null
                parts.add(String(dns, i, labelLen, Charsets.US_ASCII))
                i += labelLen
            }
            if (parts.isEmpty()) return null
            return parts.joinToString(".")
        } catch (e: Exception) {
            return null
        }
    }

    // ── Synthetic NXDOMAIN packet builder ─────────────────────────────────────

    /**
     * Builds a complete IPv4+UDP+DNS NXDOMAIN response packet to write back to TUN.
     *
     * We swap src/dst IP and src/dst port (the response appears to come FROM the
     * DNS server TO the app), set DNS RCODE=3 (NXDOMAIN), QR=1, RA=1, copy the
     * original query's question section verbatim so the resolver matches the
     * transaction ID and question.
     */
    private fun buildNxDomainPacket(
        queryDns: ByteArray,
        queryIp: ByteArray,
        ipHeaderLen: Int,
        totalLen: Int
    ): ByteArray? {
        try {
            if (queryDns.size < 12) return null

            // ── Build DNS response payload ──
            val dnsResponse = queryDns.copyOf()
            val queryId = ((queryDns[0].toInt() and 0xFF) shl 8) or (queryDns[1].toInt() and 0xFF)
            val rdBit = (queryDns[2].toInt() and 0x01)  // copy RD from query

            // FLAGS: QR=1 | OPCODE=0 | AA=0 | TC=0 | RD=copy | RA=1 | RCODE=3
            val flags = 0x8080 or (rdBit shl 8) or 0x0003
            dnsResponse[2] = (flags ushr 8).toByte()
            dnsResponse[3] = (flags and 0xFF).toByte()
            // ANCOUNT=0, NSCOUNT=0, ARCOUNT=0 (no records — just NXDOMAIN)
            dnsResponse[4] = 0; dnsResponse[5] = 1   // QDCOUNT = 1 (keep question)
            dnsResponse[6] = 0; dnsResponse[7] = 0
            dnsResponse[8] = 0; dnsResponse[9] = 0
            dnsResponse[10] = 0; dnsResponse[11] = 0

            // ── Build UDP response ──
            val udpOffset = ipHeaderLen
            val srcPort = readUInt16(queryIp, udpOffset)       // original src port (app side)
            val dstPort = readUInt16(queryIp, udpOffset + 2)   // 53 (DNS server side)

            val udpLen = 8 + dnsResponse.size
            val udpBuf = ByteArray(udpLen)
            // Swap ports: response goes FROM dns-port TO app-port
            writeUInt16(udpBuf, 0, dstPort)   // new src = 53
            writeUInt16(udpBuf, 2, srcPort)   // new dst = original src
            writeUInt16(udpBuf, 4, udpLen)
            writeUInt16(udpBuf, 6, 0)         // checksum = 0 (optional for UDP/IPv4)
            System.arraycopy(dnsResponse, 0, udpBuf, 8, dnsResponse.size)

            // ── Build IPv4 response ──
            val ipLen = ipHeaderLen + udpLen
            val ipBuf = ByteArray(ipLen)
            // Copy original IP header, then swap src/dst IPs
            System.arraycopy(queryIp, 0, ipBuf, 0, ipHeaderLen)
            writeUInt16(ipBuf, 2, ipLen)     // update total length
            // Swap src/dst IP
            System.arraycopy(queryIp, 16, ipBuf, 12, 4)  // orig dst → new src
            System.arraycopy(queryIp, 12, ipBuf, 16, 4)  // orig src → new dst
            ipBuf[8] = 64  // TTL
            // Recalculate IP header checksum
            writeUInt16(ipBuf, 10, 0)
            writeUInt16(ipBuf, 10, ipChecksum(ipBuf, ipHeaderLen))

            System.arraycopy(udpBuf, 0, ipBuf, ipHeaderLen, udpLen)

            return ipBuf

        } catch (e: Exception) {
            Log.w(TAG, "Failed to build NXDOMAIN packet: ${e.message}")
            return null
        }
    }

    // ── Checksum / helpers ────────────────────────────────────────────────────

    private fun ipChecksum(buf: ByteArray, headerLen: Int): Int {
        var sum = 0
        var i = 0
        while (i < headerLen - 1) {
            sum += ((buf[i].toInt() and 0xFF) shl 8) or (buf[i + 1].toInt() and 0xFF)
            i += 2
        }
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum ushr 16)
        return sum.inv() and 0xFFFF
    }

    private fun readUInt16(buf: ByteArray, offset: Int): Int =
        ((buf[offset].toInt() and 0xFF) shl 8) or (buf[offset + 1].toInt() and 0xFF)

    private fun writeUInt16(buf: ByteArray, offset: Int, value: Int) {
        buf[offset]     = (value ushr 8).toByte()
        buf[offset + 1] = (value and 0xFF).toByte()
    }
}
