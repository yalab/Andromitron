package net.yalab.andromitron.filter

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SimpleDomainFilterTest {
    
    private lateinit var domainFilter: DomainFilter
    
    @Before
    fun setUp() {
        domainFilter = DomainFilter()
    }
    
    @Test
    fun `should filter exact domain correctly`() {
        domainFilter.addRule("example.com", FilterAction.BLOCK)
        
        assertEquals(FilterAction.BLOCK, domainFilter.filterDomain("example.com"))
        assertEquals(FilterAction.ALLOW, domainFilter.filterDomain("test.com"))
    }
    
    @Test
    fun `should filter wildcard domain correctly`() {
        domainFilter.addRule("*.example.com", FilterAction.BLOCK, isWildcard = true)
        
        assertEquals(FilterAction.BLOCK, domainFilter.filterDomain("sub.example.com"))
        assertEquals(FilterAction.ALLOW, domainFilter.filterDomain("example.org"))
    }
    
    @Test
    fun `should return rule count correctly`() {
        assertEquals(0, domainFilter.getRuleCount())
        
        domainFilter.addRule("example.com", FilterAction.BLOCK)
        assertEquals(1, domainFilter.getRuleCount())
    }
}