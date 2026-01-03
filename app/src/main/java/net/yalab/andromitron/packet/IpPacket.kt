package net.yalab.andromitron.packet

import java.nio.ByteBuffer

data class IpHeader(
    val version: Int,
    val headerLength: Int,
    val typeOfService: Int,
    val totalLength: Int,
    val identification: Int,
    val flags: Int,
    val fragmentOffset: Int,
    val timeToLive: Int,
    val protocol: Int,
    val headerChecksum: Int,
    val sourceAddress: ByteArray,
    val destinationAddress: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IpHeader

        if (version != other.version) return false
        if (headerLength != other.headerLength) return false
        if (typeOfService != other.typeOfService) return false
        if (totalLength != other.totalLength) return false
        if (identification != other.identification) return false
        if (flags != other.flags) return false
        if (fragmentOffset != other.fragmentOffset) return false
        if (timeToLive != other.timeToLive) return false
        if (protocol != other.protocol) return false
        if (headerChecksum != other.headerChecksum) return false
        if (!sourceAddress.contentEquals(other.sourceAddress)) return false
        if (!destinationAddress.contentEquals(other.destinationAddress)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + headerLength
        result = 31 * result + typeOfService
        result = 31 * result + totalLength
        result = 31 * result + identification
        result = 31 * result + flags
        result = 31 * result + fragmentOffset
        result = 31 * result + timeToLive
        result = 31 * result + protocol
        result = 31 * result + headerChecksum
        result = 31 * result + sourceAddress.contentHashCode()
        result = 31 * result + destinationAddress.contentHashCode()
        return result
    }
}

data class TcpHeader(
    val sourcePort: Int,
    val destinationPort: Int,
    val sequenceNumber: Long,
    val acknowledgmentNumber: Long,
    val dataOffset: Int,
    val flags: Int,
    val windowSize: Int,
    val checksum: Int,
    val urgentPointer: Int
)

data class UdpHeader(
    val sourcePort: Int,
    val destinationPort: Int,
    val length: Int,
    val checksum: Int
)

class IpPacket private constructor(
    val ipHeader: IpHeader,
    val tcpHeader: TcpHeader?,
    val udpHeader: UdpHeader?,
    val payload: ByteArray
) {
    
    companion object {
        const val PROTOCOL_TCP = 6
        const val PROTOCOL_UDP = 17
        const val MIN_IP_HEADER_SIZE = 20
        const val MIN_TCP_HEADER_SIZE = 20
        const val UDP_HEADER_SIZE = 8
        
        fun parse(buffer: ByteBuffer): IpPacket? {
            return try {
                parseInternal(buffer)
            } catch (e: Exception) {
                null
            }
        }
        
        private fun parseInternal(buffer: ByteBuffer): IpPacket {
            if (buffer.remaining() < MIN_IP_HEADER_SIZE) {
                throw IllegalArgumentException("Buffer too small for IP header")
            }
            
            val firstByte = buffer.get().toInt() and 0xFF
            val version = (firstByte shr 4) and 0x0F
            val headerLength = (firstByte and 0x0F) * 4
            
            if (version != 4) {
                throw IllegalArgumentException("Only IPv4 is supported")
            }
            
            if (buffer.remaining() < headerLength - 1) {
                throw IllegalArgumentException("Buffer too small for IP header length")
            }
            
            val typeOfService = buffer.get().toInt() and 0xFF
            val totalLength = buffer.short.toInt() and 0xFFFF
            val identification = buffer.short.toInt() and 0xFFFF
            val flagsAndFragOffset = buffer.short.toInt() and 0xFFFF
            val flags = (flagsAndFragOffset shr 13) and 0x07
            val fragmentOffset = flagsAndFragOffset and 0x1FFF
            val timeToLive = buffer.get().toInt() and 0xFF
            val protocol = buffer.get().toInt() and 0xFF
            val headerChecksum = buffer.short.toInt() and 0xFFFF
            
            val sourceAddress = ByteArray(4)
            buffer.get(sourceAddress)
            val destinationAddress = ByteArray(4)
            buffer.get(destinationAddress)
            
            // Skip options if present
            if (headerLength > MIN_IP_HEADER_SIZE) {
                val optionsSize = headerLength - MIN_IP_HEADER_SIZE
                buffer.position(buffer.position() + optionsSize)
            }
            
            val ipHeader = IpHeader(
                version, headerLength, typeOfService, totalLength,
                identification, flags, fragmentOffset, timeToLive,
                protocol, headerChecksum, sourceAddress, destinationAddress
            )
            
            val payloadSize = totalLength - headerLength
            if (buffer.remaining() < payloadSize) {
                throw IllegalArgumentException("Buffer too small for payload")
            }
            
            var tcpHeader: TcpHeader? = null
            var udpHeader: UdpHeader? = null
            val payloadStart = buffer.position()
            
            when (protocol) {
                PROTOCOL_TCP -> {
                    tcpHeader = parseTcpHeader(buffer)
                }
                PROTOCOL_UDP -> {
                    udpHeader = parseUdpHeader(buffer)
                }
            }
            
            val payloadDataSize = payloadSize - (buffer.position() - payloadStart)
            val payload = ByteArray(payloadDataSize)
            buffer.get(payload)
            
            return IpPacket(ipHeader, tcpHeader, udpHeader, payload)
        }
        
        private fun parseTcpHeader(buffer: ByteBuffer): TcpHeader? {
            if (buffer.remaining() < MIN_TCP_HEADER_SIZE) {
                return null
            }
            
            val sourcePort = buffer.short.toInt() and 0xFFFF
            val destinationPort = buffer.short.toInt() and 0xFFFF
            val sequenceNumber = buffer.int.toLong() and 0xFFFFFFFFL
            val acknowledgmentNumber = buffer.int.toLong() and 0xFFFFFFFFL
            val dataOffsetAndFlags = buffer.short.toInt() and 0xFFFF
            val dataOffset = ((dataOffsetAndFlags shr 12) and 0x0F) * 4
            val flags = dataOffsetAndFlags and 0x01FF
            val windowSize = buffer.short.toInt() and 0xFFFF
            val checksum = buffer.short.toInt() and 0xFFFF
            val urgentPointer = buffer.short.toInt() and 0xFFFF
            
            // Skip options if present
            if (dataOffset > MIN_TCP_HEADER_SIZE) {
                val optionsSize = dataOffset - MIN_TCP_HEADER_SIZE
                if (buffer.remaining() >= optionsSize) {
                    buffer.position(buffer.position() + optionsSize)
                }
            }
            
            return TcpHeader(
                sourcePort, destinationPort, sequenceNumber,
                acknowledgmentNumber, dataOffset, flags,
                windowSize, checksum, urgentPointer
            )
        }
        
        private fun parseUdpHeader(buffer: ByteBuffer): UdpHeader? {
            if (buffer.remaining() < UDP_HEADER_SIZE) {
                return null
            }
            
            val sourcePort = buffer.short.toInt() and 0xFFFF
            val destinationPort = buffer.short.toInt() and 0xFFFF
            val length = buffer.short.toInt() and 0xFFFF
            val checksum = buffer.short.toInt() and 0xFFFF
            
            return UdpHeader(sourcePort, destinationPort, length, checksum)
        }
    }
    
    fun getProtocol(): String {
        return when (ipHeader.protocol) {
            PROTOCOL_TCP -> "TCP"
            PROTOCOL_UDP -> "UDP"
            else -> "Unknown(${ipHeader.protocol})"
        }
    }
    
    fun getSourceIp(): String {
        return ipHeader.sourceAddress.joinToString(".") { (it.toInt() and 0xFF).toString() }
    }
    
    fun getDestinationIp(): String {
        return ipHeader.destinationAddress.joinToString(".") { (it.toInt() and 0xFF).toString() }
    }
    
    fun getSourcePort(): Int? {
        return tcpHeader?.sourcePort ?: udpHeader?.sourcePort
    }
    
    fun getDestinationPort(): Int? {
        return tcpHeader?.destinationPort ?: udpHeader?.destinationPort
    }
    
    fun isTcp(): Boolean = tcpHeader != null
    
    fun isUdp(): Boolean = udpHeader != null
    
    fun getPayloadSize(): Int = payload.size
}