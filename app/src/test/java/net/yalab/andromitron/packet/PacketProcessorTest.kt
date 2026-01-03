package net.yalab.andromitron.packet

import kotlinx.coroutines.test.runTest
import net.yalab.andromitron.filter.FilterAction
import net.yalab.andromitron.filter.FilterManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class PacketProcessorTest {
    
    private lateinit var packetProcessor: PacketProcessor
    private lateinit var filterManager: FilterManager
    
    @Before
    fun setUp() {
        FilterManager.resetInstance()
        filterManager = FilterManager.getInstance()
        packetProcessor = PacketProcessor()
        runTest {
            filterManager.clearAllRules()
        }
        filterManager.setEnabled(true)
    }
    
    @Test
    fun `should allow generic packets by default`() = runTest {
        val genericPacket = createGenericPacket()
        val action = packetProcessor.processPacket(genericPacket)
        
        assertEquals(PacketAction.ALLOW, action)
    }
    
    @Test
    fun `should handle invalid packet gracefully`() = runTest {
        val invalidPacket = ByteBuffer.allocate(10) // Too small
        val action = packetProcessor.processPacket(invalidPacket)
        
        assertEquals(PacketAction.ALLOW, action)
    }
    
    @Test
    fun `should provide filter statistics`() {
        val stats = packetProcessor.getFilterStats()
        
        assertNotNull(stats)
        assertTrue(stats.isEnabled)
        assertEquals(0, stats.totalRules)
        assertEquals(0, stats.cacheSize)
    }
    
    @Test
    fun `should process TCP packets`() = runTest {
        val tcpPacket = createTcpPacket()
        val action = packetProcessor.processPacket(tcpPacket)
        
        assertEquals(PacketAction.ALLOW, action)
    }
    
    @Test
    fun `should process UDP packets`() = runTest {
        val udpPacket = createUdpPacket()
        val action = packetProcessor.processPacket(udpPacket)
        
        assertEquals(PacketAction.ALLOW, action)
    }
    
    @Test
    fun `should apply filtering rules`() = runTest {
        filterManager.addRule("example.com", FilterAction.BLOCK)
        
        val packet = createGenericPacket()
        val action = packetProcessor.processPacket(packet)
        
        // Should allow because no domain was extracted
        assertEquals(PacketAction.ALLOW, action)
    }
    
    private fun createGenericPacket(): ByteBuffer {
        val buffer = ByteBuffer.allocate(20)
        
        // IP Header
        buffer.put(0x45.toByte())
        buffer.put(0x00.toByte())
        buffer.putShort(20)
        buffer.putShort(0x1234.toShort())
        buffer.putShort(0x0000.toShort())
        buffer.put(0x40.toByte())
        buffer.put(99.toByte()) // Unknown protocol
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
    
    private fun createTcpPacket(): ByteBuffer {
        val buffer = ByteBuffer.allocate(20)
        
        // IP Header with TCP protocol
        buffer.put(0x45.toByte())
        buffer.put(0x00.toByte())
        buffer.putShort(20)
        buffer.putShort(0x1234.toShort())
        buffer.putShort(0x0000.toShort())
        buffer.put(0x40.toByte())
        buffer.put(IpPacket.PROTOCOL_TCP.toByte())
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
    
    private fun createUdpPacket(): ByteBuffer {
        val buffer = ByteBuffer.allocate(20)
        
        // IP Header with UDP protocol
        buffer.put(0x45.toByte())
        buffer.put(0x00.toByte())
        buffer.putShort(20)
        buffer.putShort(0x1234.toShort())
        buffer.putShort(0x0000.toShort())
        buffer.put(0x40.toByte())
        buffer.put(IpPacket.PROTOCOL_UDP.toByte())
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