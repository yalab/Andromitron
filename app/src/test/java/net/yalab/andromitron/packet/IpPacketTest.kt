package net.yalab.andromitron.packet

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class IpPacketTest {
    
    @Test
    fun `should return null for invalid packet data`() {
        val invalidData = ByteBuffer.allocate(10) // Too small
        val packet = IpPacket.parse(invalidData)
        
        assertNull(packet)
    }
    
    @Test
    fun `should return null for non-IPv4 packet`() {
        val ipv6Data = ByteBuffer.allocate(40)
        ipv6Data.put(0x60.toByte()) // IPv6 version
        ipv6Data.flip()
        
        val packet = IpPacket.parse(ipv6Data)
        assertNull(packet)
    }
    
    @Test
    fun `should parse basic IPv4 packet`() {
        val packetData = createBasicIpPacket()
        val packet = IpPacket.parse(packetData)
        
        assertNotNull(packet)
        assertEquals(4, packet!!.ipHeader.version)
        assertEquals("192.168.1.1", packet.getSourceIp())
        assertEquals("192.168.1.2", packet.getDestinationIp())
        assertEquals("Unknown(99)", packet.getProtocol())
    }
    
    @Test
    fun `should identify TCP protocol`() {
        val packetData = createTcpIpPacket()
        val packet = IpPacket.parse(packetData)
        
        assertNotNull(packet)
        assertEquals(IpPacket.PROTOCOL_TCP, packet!!.ipHeader.protocol)
        assertEquals("TCP", packet.getProtocol())
    }
    
    @Test
    fun `should identify UDP protocol`() {
        val packetData = createUdpIpPacket()
        val packet = IpPacket.parse(packetData)
        
        assertNotNull(packet)
        assertEquals(IpPacket.PROTOCOL_UDP, packet!!.ipHeader.protocol)
        assertEquals("UDP", packet.getProtocol())
    }
    
    private fun createBasicIpPacket(): ByteBuffer {
        val buffer = ByteBuffer.allocate(20)
        
        // IP Header
        buffer.put(0x45.toByte()) // Version 4, Header length 20
        buffer.put(0x00.toByte()) // Type of service
        buffer.putShort(20) // Total length
        buffer.putShort(0x1234.toShort()) // Identification
        buffer.putShort(0x0000.toShort()) // Flags and fragment offset
        buffer.put(0x40.toByte()) // TTL
        buffer.put(99.toByte()) // Protocol (unknown)
        buffer.putShort(0x0000.toShort()) // Header checksum
        
        // Source IP: 192.168.1.1
        buffer.put(192.toByte())
        buffer.put(168.toByte())
        buffer.put(1.toByte())
        buffer.put(1.toByte())
        
        // Destination IP: 192.168.1.2
        buffer.put(192.toByte())
        buffer.put(168.toByte())
        buffer.put(1.toByte())
        buffer.put(2.toByte())
        
        buffer.flip()
        return buffer
    }
    
    private fun createTcpIpPacket(): ByteBuffer {
        val buffer = ByteBuffer.allocate(20)
        
        // IP Header
        buffer.put(0x45.toByte())
        buffer.put(0x00.toByte())
        buffer.putShort(20)
        buffer.putShort(0x1234.toShort())
        buffer.putShort(0x0000.toShort())
        buffer.put(0x40.toByte())
        buffer.put(IpPacket.PROTOCOL_TCP.toByte()) // TCP
        buffer.putShort(0x0000.toShort())
        
        // Source IP: 192.168.1.1
        buffer.put(192.toByte())
        buffer.put(168.toByte())
        buffer.put(1.toByte())
        buffer.put(1.toByte())
        
        // Destination IP: 192.168.1.2
        buffer.put(192.toByte())
        buffer.put(168.toByte())
        buffer.put(1.toByte())
        buffer.put(2.toByte())
        
        buffer.flip()
        return buffer
    }
    
    private fun createUdpIpPacket(): ByteBuffer {
        val buffer = ByteBuffer.allocate(20)
        
        // IP Header  
        buffer.put(0x45.toByte())
        buffer.put(0x00.toByte())
        buffer.putShort(20)
        buffer.putShort(0x1234.toShort())
        buffer.putShort(0x0000.toShort())
        buffer.put(0x40.toByte())
        buffer.put(IpPacket.PROTOCOL_UDP.toByte()) // UDP
        buffer.putShort(0x0000.toShort())
        
        // Source IP: 192.168.1.1
        buffer.put(192.toByte())
        buffer.put(168.toByte())
        buffer.put(1.toByte())
        buffer.put(1.toByte())
        
        // Destination IP: 192.168.1.2
        buffer.put(192.toByte())
        buffer.put(168.toByte())
        buffer.put(1.toByte())
        buffer.put(2.toByte())
        
        buffer.flip()
        return buffer
    }
}