package net.yalab.andromitron.logging

import net.yalab.andromitron.logging.ConnectionLogger
import net.yalab.andromitron.packet.PacketProcessor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class PacketProcessorWithLoggingTest {

    @Mock
    private lateinit var connectionLogger: ConnectionLogger
    
    private lateinit var packetProcessor: PacketProcessor

    @Before
    fun setup() {
        packetProcessor = PacketProcessor(connectionLogger)
    }

    @Test
    fun testConnectionLoggerIntegration() {
        // Test that the packet processor can work with a null logger (defensive programming)
        val processorWithoutLogger = PacketProcessor(null)
        assertNotNull("PacketProcessor should be created successfully", processorWithoutLogger)
    }

    @Test
    fun testGetFilterStats() {
        val stats = packetProcessor.getFilterStats()
        assertNotNull("Filter stats should not be null", stats)
    }

    @Test
    fun testPacketProcessorWithLogger() {
        assertNotNull("PacketProcessor should be created with logger", packetProcessor)
    }
}