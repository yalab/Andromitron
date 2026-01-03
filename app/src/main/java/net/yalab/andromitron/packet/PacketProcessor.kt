package net.yalab.andromitron.packet

import android.util.Log
import net.yalab.andromitron.filter.FilterAction
import net.yalab.andromitron.filter.FilterManager
import net.yalab.andromitron.logging.ConnectionLogger
import java.nio.ByteBuffer

class PacketProcessor(private val connectionLogger: ConnectionLogger? = null) {
    
    private val filterManager = FilterManager.getInstance()
    
    // Simple cache for recent domain filtering results
    private val domainFilterCache = mutableMapOf<String, FilterAction>()
    private var cacheSize = 0
    private val maxCacheSize = 1000
    
    companion object {
        private const val TAG = "PacketProcessor"
        
        // Common DNS ports
        private const val DNS_PORT = 53
        
        // Common HTTP/HTTPS ports
        private const val HTTP_PORT = 80
        private const val HTTPS_PORT = 443
    }
    
    suspend fun processPacket(packetData: ByteBuffer): PacketAction {
        val packet = IpPacket.parse(packetData)
        
        if (packet == null) {
            Log.w(TAG, "Failed to parse packet")
            return PacketAction.ALLOW
        }
        
        // Log packet details for debugging only when logger is present
        connectionLogger?.logPacketDetails(
            sourceIp = packet.getSourceIp(),
            destinationIp = packet.getDestinationIp(),
            protocol = packet.getProtocol(),
            sourcePort = packet.getSourcePort(),
            destinationPort = packet.getDestinationPort(),
            payloadSize = packet.payload.size
        )
        
        return when {
            isHttpPacket(packet) -> processHttpPacket(packet)
            isHttpsPacket(packet) -> processHttpsPacket(packet)
            else -> processGenericPacket(packet)
        }
    }
    
    private fun isDnsPacket(packet: IpPacket): Boolean {
        return packet.isUdp() && 
               (packet.getDestinationPort() == DNS_PORT || packet.getSourcePort() == DNS_PORT)
    }
    
    private fun isHttpPacket(packet: IpPacket): Boolean {
        if (!packet.isTcp()) return false
        val destPort = packet.getDestinationPort()
        val srcPort = packet.getSourcePort()
        return destPort == HTTP_PORT || srcPort == HTTP_PORT
    }
    
    private fun isHttpsPacket(packet: IpPacket): Boolean {
        if (!packet.isTcp()) return false
        val destPort = packet.getDestinationPort()
        val srcPort = packet.getSourcePort()
        return destPort == HTTPS_PORT || srcPort == HTTPS_PORT
    }
    
    private suspend fun processDnsPacket(packet: IpPacket): PacketAction {
        Log.d(TAG, "Processing DNS packet")
        
        val domain = extractDomainFromDnsPacket(packet)
        if (domain != null) {
            Log.d(TAG, "DNS query for domain: $domain")
            
            val action = filterManager.filterDomain(domain)
            return when (action) {
                FilterAction.BLOCK -> {
                    Log.i(TAG, "Blocking DNS query for: $domain")
                    connectionLogger?.logDomainBlocked(domain, packet.getDestinationIp(), "DNS", packet.getDestinationPort())
                    PacketAction.BLOCK
                }
                FilterAction.ALLOW -> {
                    Log.v(TAG, "Allowing DNS query for: $domain")
                    connectionLogger?.logDomainAllowed(domain, packet.getDestinationIp(), "DNS", packet.getDestinationPort())
                    PacketAction.ALLOW
                }
                FilterAction.PROXY -> {
                    Log.d(TAG, "Proxying DNS query for: $domain")
                    connectionLogger?.logProxiedConnection(
                        packet.getSourceIp(),
                        packet.getDestinationIp(),
                        "DNS",
                        packet.getSourcePort(),
                        packet.getDestinationPort(),
                        domain
                    )
                    PacketAction.PROXY
                }
            }
        }
        
        return PacketAction.ALLOW
    }
    
    private suspend fun processHttpPacket(packet: IpPacket): PacketAction {
        val domain = extractDomainFromHttpPacket(packet) ?: return PacketAction.ALLOW
        
        val action = getCachedFilterAction(domain) ?: run {
            val result = filterManager.filterDomain(domain)
            cacheFilterAction(domain, result)
            result
        }
        
        when (action) {
            FilterAction.BLOCK -> {
                connectionLogger?.logDomainBlocked(domain, packet.getDestinationIp(), "HTTP", packet.getDestinationPort())
                return PacketAction.BLOCK
            }
            FilterAction.ALLOW -> {
                connectionLogger?.logDomainAllowed(domain, packet.getDestinationIp(), "HTTP", packet.getDestinationPort())
                return PacketAction.ALLOW
            }
            FilterAction.PROXY -> {
                connectionLogger?.logProxiedConnection(
                    packet.getSourceIp(),
                    packet.getDestinationIp(),
                    "HTTP",
                    packet.getSourcePort(),
                    packet.getDestinationPort(),
                    domain
                )
                return PacketAction.PROXY
            }
        }
    }
    
    private suspend fun processHttpsPacket(packet: IpPacket): PacketAction {
        val domain = extractDomainFromTlsSni(packet) ?: return PacketAction.ALLOW
        
        val action = getCachedFilterAction(domain) ?: run {
            val result = filterManager.filterDomain(domain)
            cacheFilterAction(domain, result)
            result
        }
        
        when (action) {
            FilterAction.BLOCK -> {
                connectionLogger?.logDomainBlocked(domain, packet.getDestinationIp(), "HTTPS", packet.getDestinationPort())
                return PacketAction.BLOCK
            }
            FilterAction.ALLOW -> {
                connectionLogger?.logDomainAllowed(domain, packet.getDestinationIp(), "HTTPS", packet.getDestinationPort())
                return PacketAction.ALLOW
            }
            FilterAction.PROXY -> {
                connectionLogger?.logProxiedConnection(
                    packet.getSourceIp(),
                    packet.getDestinationIp(),
                    "HTTPS",
                    packet.getSourcePort(),
                    packet.getDestinationPort(),
                    domain
                )
                return PacketAction.PROXY
            }
        }
    }
    
    private fun processGenericPacket(packet: IpPacket): PacketAction {
        return PacketAction.ALLOW
    }
    
    private fun extractDomainFromDnsPacket(packet: IpPacket): String? {
        return try {
            val payload = packet.payload
            if (payload.size < 12) return null
            
            val buffer = ByteBuffer.wrap(payload)
            
            // Skip DNS header (12 bytes)
            buffer.position(12)
            
            // Extract domain name from question section
            val domain = StringBuilder()
            var labelLength = buffer.get().toInt() and 0xFF
            
            while (labelLength != 0 && buffer.hasRemaining()) {
                if (domain.isNotEmpty()) {
                    domain.append('.')
                }
                
                if (buffer.remaining() < labelLength) break
                
                val label = ByteArray(labelLength)
                buffer.get(label)
                domain.append(String(label))
                
                if (buffer.hasRemaining()) {
                    labelLength = buffer.get().toInt() and 0xFF
                } else {
                    break
                }
            }
            
            val domainStr = domain.toString()
            if (domainStr.isNotEmpty()) domainStr else null
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting domain from DNS packet", e)
            null
        }
    }
    
    private fun extractDomainFromHttpPacket(packet: IpPacket): String? {
        return try {
            val payload = packet.payload
            if (payload.size < 20) return null // Skip very small packets
            
            val payloadString = String(payload, Charsets.UTF_8)
            
            // Quick check if it's likely HTTP
            if (!payloadString.startsWith("GET ") && !payloadString.startsWith("POST ") && 
                !payloadString.startsWith("PUT ") && !payloadString.startsWith("HEAD ")) {
                return null
            }
            
            // Look for Host header more efficiently
            val hostIndex = payloadString.indexOf("Host:", ignoreCase = true)
            if (hostIndex == -1) return null
            
            val lineStart = hostIndex + 5
            val lineEnd = payloadString.indexOf('\n', lineStart)
            if (lineEnd == -1) return null
            
            payloadString.substring(lineStart, lineEnd).trim().replace("\r", "")
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractDomainFromTlsSni(packet: IpPacket): String? {
        return try {
            val payload = packet.payload
            if (payload.size < 43) return null
            
            val buffer = ByteBuffer.wrap(payload)
            
            // Quick check for TLS handshake and ClientHello
            if (buffer.get().toInt() and 0xFF != 0x16) return null // Not a handshake
            buffer.position(5) // Skip version and length
            if (buffer.get().toInt() and 0xFF != 0x01) return null // Not ClientHello
            
            // Jump to extensions more efficiently
            buffer.position(buffer.position() + 37) // Skip handshake length, version, random
            
            // Skip session ID
            if (!buffer.hasRemaining()) return null
            val sessionIdLength = buffer.get().toInt() and 0xFF
            if (buffer.remaining() < sessionIdLength) return null
            buffer.position(buffer.position() + sessionIdLength)
            
            // Skip cipher suites
            if (buffer.remaining() < 2) return null
            val cipherSuitesLength = buffer.short.toInt() and 0xFFFF
            if (buffer.remaining() < cipherSuitesLength) return null
            buffer.position(buffer.position() + cipherSuitesLength)
            
            // Skip compression methods
            if (!buffer.hasRemaining()) return null
            val compressionMethodsLength = buffer.get().toInt() and 0xFF
            if (buffer.remaining() < compressionMethodsLength) return null
            buffer.position(buffer.position() + compressionMethodsLength)
            
            // Parse extensions
            if (buffer.remaining() < 2) return null
            val extensionsLength = buffer.short.toInt() and 0xFFFF
            if (buffer.remaining() < extensionsLength) return null
            
            return parseServerNameExtension(buffer, extensionsLength)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseServerNameExtension(buffer: ByteBuffer, extensionsLength: Int): String? {
        val endPosition = buffer.position() + extensionsLength
        
        while (buffer.position() < endPosition && buffer.remaining() >= 4) {
            val extensionType = buffer.short.toInt() and 0xFFFF
            val extensionLength = buffer.short.toInt() and 0xFFFF
            
            if (extensionType == 0x0000) { // Server Name Indication
                if (buffer.remaining() < extensionLength) break
                
                val serverNameListLength = buffer.short.toInt() and 0xFFFF
                if (buffer.remaining() < serverNameListLength) break
                
                val nameType = buffer.get().toInt() and 0xFF
                if (nameType == 0x00) { // hostname
                    val nameLength = buffer.short.toInt() and 0xFFFF
                    if (buffer.remaining() >= nameLength) {
                        val nameBytes = ByteArray(nameLength)
                        buffer.get(nameBytes)
                        return String(nameBytes)
                    }
                }
                break
            } else {
                // Skip this extension
                buffer.position(buffer.position() + extensionLength)
            }
        }
        
        return null
    }
    
    private fun getCachedFilterAction(domain: String): FilterAction? {
        return domainFilterCache[domain]
    }
    
    private fun cacheFilterAction(domain: String, action: FilterAction) {
        if (cacheSize >= maxCacheSize) {
            // Simple cache eviction - clear half the cache
            val keysToRemove = domainFilterCache.keys.take(maxCacheSize / 2)
            keysToRemove.forEach { domainFilterCache.remove(it) }
            cacheSize = domainFilterCache.size
        }
        
        domainFilterCache[domain] = action
        cacheSize++
    }
    
    fun clearCache() {
        domainFilterCache.clear()
        cacheSize = 0
    }
    
    fun getFilterStats() = filterManager.getStats()
}

enum class PacketAction {
    ALLOW,
    BLOCK,
    PROXY
}