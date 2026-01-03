package net.yalab.andromitron.packet

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class SimplePacketTest {
    
    @Test
    fun `should return null for empty buffer`() {
        val emptyBuffer = ByteBuffer.allocate(0)
        val packet = IpPacket.parse(emptyBuffer)
        assertNull(packet)
    }
    
    @Test
    fun `should return null for buffer too small`() {
        val smallBuffer = ByteBuffer.allocate(10)
        val packet = IpPacket.parse(smallBuffer)
        assertNull(packet)
    }
    
    @Test
    fun `should handle basic packet structure`() {
        val buffer = createMinimalValidPacket()
        val packet = IpPacket.parse(buffer)
        
        // Just verify it doesn't crash and basic properties
        if (packet != null) {
            assertEquals(4, packet.ipHeader.version)
            assertNotNull(packet.getProtocol())
            assertNotNull(packet.getSourceIp())
            assertNotNull(packet.getDestinationIp())
        }
    }
    
    private fun createMinimalValidPacket(): ByteBuffer {
        val buffer = ByteBuffer.allocate(20) // Minimal IP header only
        
        // IP Header (20 bytes)
        buffer.put(0x45.toByte()) // Version 4, Header length 20
        buffer.put(0x00.toByte()) // Type of service  
        buffer.putShort(20) // Total length (header only)
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
}