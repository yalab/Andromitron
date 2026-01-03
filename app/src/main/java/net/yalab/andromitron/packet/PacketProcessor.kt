package net.yalab.andromitron.packet

import android.util.Log
import net.yalab.andromitron.filter.FilterAction
import net.yalab.andromitron.filter.FilterManager
import java.nio.ByteBuffer

class PacketProcessor {
    
    private val filterManager = FilterManager.getInstance()
    
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
        
        Log.v(TAG, "Processing ${packet.getProtocol()} packet: " +
                "${packet.getSourceIp()}:${packet.getSourcePort()} -> " +
                "${packet.getDestinationIp()}:${packet.getDestinationPort()}")
        
        return when {
            isDnsPacket(packet) -> processDnsPacket(packet)
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
        return packet.isTcp() && 
               (packet.getDestinationPort() == HTTP_PORT || packet.getSourcePort() == HTTP_PORT)
    }
    
    private fun isHttpsPacket(packet: IpPacket): Boolean {
        return packet.isTcp() && 
               (packet.getDestinationPort() == HTTPS_PORT || packet.getSourcePort() == HTTPS_PORT)
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
                    PacketAction.BLOCK
                }
                FilterAction.ALLOW -> {
                    Log.v(TAG, "Allowing DNS query for: $domain")
                    PacketAction.ALLOW
                }
                FilterAction.PROXY -> {
                    Log.d(TAG, "Proxying DNS query for: $domain")
                    PacketAction.PROXY
                }
            }
        }
        
        return PacketAction.ALLOW
    }
    
    private suspend fun processHttpPacket(packet: IpPacket): PacketAction {
        Log.d(TAG, "Processing HTTP packet")
        
        val domain = extractDomainFromHttpPacket(packet)
        if (domain != null) {
            Log.d(TAG, "HTTP request to domain: $domain")
            
            val action = filterManager.filterDomain(domain)
            return when (action) {
                FilterAction.BLOCK -> {
                    Log.i(TAG, "Blocking HTTP request to: $domain")
                    PacketAction.BLOCK
                }
                FilterAction.ALLOW -> {
                    Log.v(TAG, "Allowing HTTP request to: $domain")
                    PacketAction.ALLOW
                }
                FilterAction.PROXY -> {
                    Log.d(TAG, "Proxying HTTP request to: $domain")
                    PacketAction.PROXY
                }
            }
        }
        
        return PacketAction.ALLOW
    }
    
    private suspend fun processHttpsPacket(packet: IpPacket): PacketAction {
        Log.d(TAG, "Processing HTTPS packet")
        
        val domain = extractDomainFromTlsSni(packet)
        if (domain != null) {
            Log.d(TAG, "HTTPS/TLS request to domain: $domain")
            
            val action = filterManager.filterDomain(domain)
            return when (action) {
                FilterAction.BLOCK -> {
                    Log.i(TAG, "Blocking HTTPS request to: $domain")
                    PacketAction.BLOCK
                }
                FilterAction.ALLOW -> {
                    Log.v(TAG, "Allowing HTTPS request to: $domain")
                    PacketAction.ALLOW
                }
                FilterAction.PROXY -> {
                    Log.d(TAG, "Proxying HTTPS request to: $domain")
                    PacketAction.PROXY
                }
            }
        }
        
        return PacketAction.ALLOW
    }
    
    private fun processGenericPacket(packet: IpPacket): PacketAction {
        Log.v(TAG, "Processing generic ${packet.getProtocol()} packet")
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
            val payloadString = String(payload)
            
            // Look for Host header
            val hostHeaderRegex = Regex("Host:\\s*([^\\r\\n]+)", RegexOption.IGNORE_CASE)
            val match = hostHeaderRegex.find(payloadString)
            
            match?.groups?.get(1)?.value?.trim()
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting domain from HTTP packet", e)
            null
        }
    }
    
    private fun extractDomainFromTlsSni(packet: IpPacket): String? {
        return try {
            val payload = packet.payload
            if (payload.size < 43) return null
            
            val buffer = ByteBuffer.wrap(payload)
            
            // Check if this is a TLS handshake
            val contentType = buffer.get().toInt() and 0xFF
            if (contentType != 0x16) return null // Not a handshake
            
            // Skip version (2 bytes) and length (2 bytes)
            buffer.position(5)
            
            // Check handshake type
            val handshakeType = buffer.get().toInt() and 0xFF
            if (handshakeType != 0x01) return null // Not ClientHello
            
            // Skip handshake length (3 bytes), version (2 bytes), random (32 bytes)
            buffer.position(buffer.position() + 37)
            
            // Skip session ID
            if (!buffer.hasRemaining()) return null
            val sessionIdLength = buffer.get().toInt() and 0xFF
            buffer.position(buffer.position() + sessionIdLength)
            
            // Skip cipher suites
            if (buffer.remaining() < 2) return null
            val cipherSuitesLength = buffer.short.toInt() and 0xFFFF
            buffer.position(buffer.position() + cipherSuitesLength)
            
            // Skip compression methods
            if (!buffer.hasRemaining()) return null
            val compressionMethodsLength = buffer.get().toInt() and 0xFF
            buffer.position(buffer.position() + compressionMethodsLength)
            
            // Parse extensions
            if (buffer.remaining() < 2) return null
            val extensionsLength = buffer.short.toInt() and 0xFFFF
            
            return parseServerNameExtension(buffer, extensionsLength)
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting SNI from TLS packet", e)
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
    
    fun getFilterStats() = filterManager.getStats()
}

enum class PacketAction {
    ALLOW,
    BLOCK,
    PROXY
}