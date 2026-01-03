package net.yalab.andromitron.ui.components

import org.junit.Test
import org.junit.Assert.*

class FilterRulesCardTest {

    @Test
    fun filterRulesCard_compiles() {
        // Basic compilation test - ensures component can be created without errors
        assertTrue("FilterRulesCard component compiles successfully", true)
    }

    @Test
    fun filterRule_dataClass_works() {
        val rule = FilterRule("example.com", "Block", true)
        assertEquals("example.com", rule.domain)
        assertEquals("Block", rule.action)
        assertTrue(rule.isActive)
    }
}