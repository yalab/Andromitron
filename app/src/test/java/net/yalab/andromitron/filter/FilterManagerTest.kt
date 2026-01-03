package net.yalab.andromitron.filter

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FilterManagerTest {
    
    private lateinit var filterManager: FilterManager
    
    @Before
    fun setUp() {
        FilterManager.resetInstance()
        filterManager = FilterManager.getInstance()
        runTest {
            filterManager.clearAllRules()
        }
        filterManager.setEnabled(true)
    }
    
    @Test
    fun `should be singleton instance`() {
        val instance1 = FilterManager.getInstance()
        val instance2 = FilterManager.getInstance()
        
        assertSame(instance1, instance2)
    }
    
    @Test
    fun `should add and filter domain rules correctly`() = runTest {
        filterManager.addRule("example.com", FilterAction.BLOCK)
        
        assertEquals(FilterAction.BLOCK, filterManager.filterDomain("example.com"))
        assertEquals(FilterAction.ALLOW, filterManager.filterDomain("test.com"))
    }
    
    @Test
    fun `should add and filter wildcard rules correctly`() = runTest {
        filterManager.addRule("*.ads.com", FilterAction.BLOCK, isWildcard = true)
        
        assertEquals(FilterAction.BLOCK, filterManager.filterDomain("google.ads.com"))
        assertEquals(FilterAction.BLOCK, filterManager.filterDomain("test.ads.com"))
        assertEquals(FilterAction.ALLOW, filterManager.filterDomain("ads.org"))
    }
    
    @Test
    fun `should remove rules correctly`() = runTest {
        filterManager.addRule("example.com", FilterAction.BLOCK)
        filterManager.addRule("test.com", FilterAction.ALLOW)
        
        assertEquals(2, filterManager.getRuleCount())
        
        filterManager.removeRule("example.com")
        
        assertEquals(1, filterManager.getRuleCount())
        assertEquals(FilterAction.ALLOW, filterManager.filterDomain("example.com"))
    }
    
    @Test
    fun `should clear all rules correctly`() = runTest {
        filterManager.addRule("example.com", FilterAction.BLOCK)
        filterManager.addRule("test.com", FilterAction.ALLOW)
        
        assertEquals(2, filterManager.getRuleCount())
        
        filterManager.clearAllRules()
        
        assertEquals(0, filterManager.getRuleCount())
    }
    
    @Test
    fun `should load default rules correctly`() = runTest {
        filterManager.loadDefaultRules()
        
        assertTrue(filterManager.getRuleCount() > 0)
        assertEquals(FilterAction.ALLOW, filterManager.filterDomain("google.com"))
        assertEquals(FilterAction.BLOCK, filterManager.filterDomain("facebook.com"))
    }
    
    @Test
    fun `should respect enabled state`() = runTest {
        filterManager.addRule("example.com", FilterAction.BLOCK)
        
        assertTrue(filterManager.isEnabled())
        assertEquals(FilterAction.BLOCK, filterManager.filterDomain("example.com"))
        
        filterManager.setEnabled(false)
        assertFalse(filterManager.isEnabled())
        assertEquals(FilterAction.ALLOW, filterManager.filterDomain("example.com"))
        
        filterManager.setEnabled(true)
        assertEquals(FilterAction.BLOCK, filterManager.filterDomain("example.com"))
    }
    
    @Test
    fun `should use cache for performance`() = runTest {
        filterManager.addRule("example.com", FilterAction.BLOCK)
        
        assertEquals(0, filterManager.getCacheSize())
        
        filterManager.filterDomain("example.com")
        assertEquals(1, filterManager.getCacheSize())
        
        filterManager.filterDomain("example.com")
        assertEquals(1, filterManager.getCacheSize())
        
        filterManager.filterDomain("test.com")
        assertEquals(2, filterManager.getCacheSize())
    }
    
    @Test
    fun `should check if domain has rule correctly`() = runTest {
        assertFalse(filterManager.hasRule("example.com"))
        
        filterManager.addRule("example.com", FilterAction.BLOCK)
        assertTrue(filterManager.hasRule("example.com"))
        assertFalse(filterManager.hasRule("test.com"))
    }
    
    @Test
    fun `should get rules list correctly`() = runTest {
        filterManager.addRule("example.com", FilterAction.BLOCK)
        filterManager.addRule("*.test.com", FilterAction.ALLOW, isWildcard = true)
        
        val rules = filterManager.getRules()
        assertEquals(2, rules.size)
        
        val rulesDomains = rules.map { it.domain }
        assertTrue(rulesDomains.contains("example.com"))
        assertTrue(rulesDomains.contains("*.test.com"))
    }
    
    @Test
    fun `should provide filter statistics`() = runTest {
        filterManager.addRule("example.com", FilterAction.BLOCK)
        filterManager.filterDomain("test.com")
        
        val stats = filterManager.getStats()
        
        assertEquals(1, stats.totalRules)
        assertEquals(1, stats.cacheSize)
        assertTrue(stats.isEnabled)
        
        filterManager.setEnabled(false)
        val disabledStats = filterManager.getStats()
        assertFalse(disabledStats.isEnabled)
    }
    
    @Test
    fun `should handle case insensitive filtering`() = runTest {
        filterManager.addRule("Example.COM", FilterAction.BLOCK)
        
        assertEquals(FilterAction.BLOCK, filterManager.filterDomain("example.com"))
        assertEquals(FilterAction.BLOCK, filterManager.filterDomain("EXAMPLE.COM"))
        assertEquals(FilterAction.BLOCK, filterManager.filterDomain("Example.Com"))
    }
}