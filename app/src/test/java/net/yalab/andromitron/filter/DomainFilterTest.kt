package net.yalab.andromitron.filter

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DomainFilterTest {
    
    private lateinit var domainFilter: DomainFilter
    
    @Before
    fun setUp() {
        domainFilter = DomainFilter()
    }
    
    @Test
    fun `should add exact domain rule correctly`() {
        domainFilter.addRule("example.com", FilterAction.BLOCK)
        
        assertEquals(FilterAction.BLOCK, domainFilter.filterDomain("example.com"))
        assertEquals(FilterAction.ALLOW, domainFilter.filterDomain("test.com"))
    }
    
    @Test
    fun `should add wildcard domain rule correctly`() {
        domainFilter.addRule("*.example.com", FilterAction.BLOCK, isWildcard = true)
        
        assertEquals(FilterAction.BLOCK, domainFilter.filterDomain("sub.example.com"))
        assertEquals(FilterAction.BLOCK, domainFilter.filterDomain("test.sub.example.com"))
        assertEquals(FilterAction.ALLOW, domainFilter.filterDomain("example.org"))
    }
    
    @Test
    fun `should handle case normalization`() {
        domainFilter.addRule("Example.COM", FilterAction.BLOCK)
        
        assertEquals(FilterAction.BLOCK, domainFilter.filterDomain("example.com"))
        assertEquals(FilterAction.BLOCK, domainFilter.filterDomain("EXAMPLE.COM"))
        assertEquals(FilterAction.BLOCK, domainFilter.filterDomain("Example.Com"))
    }
    
    @Test
    fun `should remove domain rules correctly`() {
        domainFilter.addRule("example.com", FilterAction.BLOCK)
        domainFilter.addRule("test.com", FilterAction.ALLOW)
        
        assertEquals(2, domainFilter.getRuleCount())
        
        domainFilter.removeRule("example.com")
        
        assertEquals(1, domainFilter.getRuleCount())
        assertEquals(FilterAction.ALLOW, domainFilter.filterDomain("example.com"))
    }
    
    @Test
    fun `should clear all rules correctly`() {
        domainFilter.addRule("example.com", FilterAction.BLOCK)
        domainFilter.addRule("test.com", FilterAction.ALLOW)
        
        assertEquals(2, domainFilter.getRuleCount())
        
        domainFilter.clearRules()
        
        assertEquals(0, domainFilter.getRuleCount())
        assertEquals(FilterAction.ALLOW, domainFilter.filterDomain("example.com"))
    }
    
    @Test
    fun `should handle exact rules priority over wildcard rules`() {
        domainFilter.addRule("*.example.com", FilterAction.BLOCK, isWildcard = true)
        domainFilter.addRule("safe.example.com", FilterAction.ALLOW)
        
        assertEquals(FilterAction.ALLOW, domainFilter.filterDomain("safe.example.com"))
        assertEquals(FilterAction.BLOCK, domainFilter.filterDomain("unsafe.example.com"))
    }
    
    @Test
    fun `should return correct rule count`() {
        assertEquals(0, domainFilter.getRuleCount())
        
        domainFilter.addRule("example.com", FilterAction.BLOCK)
        assertEquals(1, domainFilter.getRuleCount())
        
        domainFilter.addRule("*.test.com", FilterAction.ALLOW, isWildcard = true)
        assertEquals(2, domainFilter.getRuleCount())
    }
    
    @Test
    fun `should check if domain has rule correctly`() {
        assertFalse(domainFilter.hasRule("example.com"))
        
        domainFilter.addRule("example.com", FilterAction.BLOCK)
        assertTrue(domainFilter.hasRule("example.com"))
        assertFalse(domainFilter.hasRule("test.com"))
    }
    
    @Test
    fun `should get rules list correctly`() {
        domainFilter.addRule("example.com", FilterAction.BLOCK)
        domainFilter.addRule("*.test.com", FilterAction.ALLOW, isWildcard = true)
        
        val rules = domainFilter.getRules()
        assertEquals(2, rules.size)
        
        val domains = rules.map { it.domain }
        assertTrue(domains.contains("example.com"))
        assertTrue(domains.contains("*.test.com"))
    }
    
    @Test
    fun `should handle whitespace normalization`() {
        domainFilter.addRule("  example.com  ", FilterAction.BLOCK)
        
        assertEquals(FilterAction.BLOCK, domainFilter.filterDomain("example.com"))
        assertEquals(FilterAction.BLOCK, domainFilter.filterDomain("  example.com  "))
    }
    
    @Test
    fun `should return default action for unknown domains`() {
        assertEquals(FilterAction.ALLOW, domainFilter.filterDomain("unknown.com"))
        assertEquals(FilterAction.ALLOW, domainFilter.filterDomain(""))
    }
}