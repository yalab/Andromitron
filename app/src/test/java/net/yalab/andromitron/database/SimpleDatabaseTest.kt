package net.yalab.andromitron.database

import net.yalab.andromitron.filter.FilterAction
import net.yalab.andromitron.filter.FilterRule
import org.junit.Assert.*
import org.junit.Test

class SimpleDatabaseTest {
    
    @Test
    fun `should create filter rule entity from filter rule`() {
        val filterRule = FilterRule(
            domain = "example.com",
            action = FilterAction.BLOCK,
            isWildcard = false
        )
        
        val entity = FilterRuleEntity.fromFilterRule(filterRule)
        
        assertEquals("example.com", entity.domain)
        assertEquals(FilterAction.BLOCK.name, entity.action)
        assertFalse(entity.isWildcard)
    }
    
    @Test
    fun `should convert filter rule entity to filter rule`() {
        val entity = FilterRuleEntity(
            domain = "example.com",
            action = FilterAction.ALLOW.name,
            isWildcard = true
        )
        
        val filterRule = entity.toFilterRule()
        
        assertEquals("example.com", filterRule.domain)
        assertEquals(FilterAction.ALLOW, filterRule.action)
        assertTrue(filterRule.isWildcard)
    }
    
    @Test
    fun `should create connection log entity`() {
        val log = ConnectionLogEntity(
            timestamp = 1234567890L,
            domain = "example.com",
            sourceIp = "192.168.1.100",
            destinationIp = "8.8.8.8",
            sourcePort = 12345,
            destinationPort = 80,
            protocol = "TCP",
            action = "ALLOW",
            bytesSent = 1024,
            bytesReceived = 2048
        )
        
        assertEquals(1234567890L, log.timestamp)
        assertEquals("example.com", log.domain)
        assertEquals("192.168.1.100", log.sourceIp)
        assertEquals("8.8.8.8", log.destinationIp)
        assertEquals(12345, log.sourcePort)
        assertEquals(80, log.destinationPort)
        assertEquals("TCP", log.protocol)
        assertEquals("ALLOW", log.action)
        assertEquals(1024L, log.bytesSent)
        assertEquals(2048L, log.bytesReceived)
    }
    
    @Test
    fun `should handle nullable fields in connection log`() {
        val log = ConnectionLogEntity(
            timestamp = 1234567890L,
            domain = null,
            sourceIp = "192.168.1.100",
            destinationIp = "8.8.8.8",
            sourcePort = null,
            destinationPort = null,
            protocol = "UDP",
            action = "BLOCK"
        )
        
        assertNull(log.domain)
        assertNull(log.sourcePort)
        assertNull(log.destinationPort)
        assertEquals("UDP", log.protocol)
        assertEquals("BLOCK", log.action)
        assertEquals(0L, log.bytesSent)
        assertEquals(0L, log.bytesReceived)
    }
    
    @Test
    fun `should create database stats`() {
        val stats = DatabaseStats(
            filterRuleCount = 10,
            totalConnectionLogCount = 100,
            recentConnectionLogCount = 5
        )
        
        assertEquals(10, stats.filterRuleCount)
        assertEquals(100, stats.totalConnectionLogCount)
        assertEquals(5, stats.recentConnectionLogCount)
    }
    
    @Test
    fun `should create domain count result`() {
        val result = DomainCountResult(
            domain = "example.com",
            count = 42
        )
        
        assertEquals("example.com", result.domain)
        assertEquals(42, result.count)
    }
    
    @Test
    fun `should create action count result`() {
        val result = ActionCountResult(
            action = "BLOCK",
            count = 15
        )
        
        assertEquals("BLOCK", result.action)
        assertEquals(15, result.count)
    }
}