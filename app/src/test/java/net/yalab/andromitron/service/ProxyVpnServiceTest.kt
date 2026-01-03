package net.yalab.andromitron.service

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProxyVpnServiceTest {

    @Test
    fun `service constants should be defined correctly`() {
        assertEquals("start_vpn", ProxyVpnService.ACTION_START)
        assertEquals("stop_vpn", ProxyVpnService.ACTION_STOP)
    }

    @Test
    fun `service companion object values are correct`() {
        val startAction = ProxyVpnService.ACTION_START
        val stopAction = ProxyVpnService.ACTION_STOP
        
        assertNotNull("Start action should not be null", startAction)
        assertNotNull("Stop action should not be null", stopAction)
        assertTrue("Start action should not be empty", startAction.isNotEmpty())
        assertTrue("Stop action should not be empty", stopAction.isNotEmpty())
    }

    @Test
    fun `service actions should be different`() {
        assertNotEquals(
            "Start and stop actions should be different",
            ProxyVpnService.ACTION_START,
            ProxyVpnService.ACTION_STOP
        )
    }
}