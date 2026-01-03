package net.yalab.andromitron

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import net.yalab.andromitron.service.ProxyVpnService
import net.yalab.andromitron.ui.AndromitronApp
import net.yalab.andromitron.ui.theme.AndromitronTheme

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private var vpnConnectionCallback: ((Boolean) -> Unit)? = null
    
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val granted = result.resultCode == RESULT_OK
        Log.d(TAG, "VPN permission result: $granted")
        vpnConnectionCallback?.invoke(granted)
        vpnConnectionCallback = null
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndromitronTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AndromitronApp(
                        onVpnToggle = { isActive, callback ->
                            handleVpnToggle(isActive, callback)
                        }
                    )
                }
            }
        }
    }
    
    private fun handleVpnToggle(isActive: Boolean, callback: (Boolean) -> Unit) {
        if (isActive) {
            startVpnService(callback)
        } else {
            stopVpnService()
            callback(false)
        }
    }
    
    private fun startVpnService(callback: (Boolean) -> Unit) {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnConnectionCallback = { granted ->
                if (granted) {
                    val serviceIntent = Intent(this, ProxyVpnService::class.java).apply {
                        action = ProxyVpnService.ACTION_START
                    }
                    startService(serviceIntent)
                    callback(true)
                } else {
                    Log.w(TAG, "VPN permission denied")
                    callback(false)
                }
            }
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
            val serviceIntent = Intent(this, ProxyVpnService::class.java).apply {
                action = ProxyVpnService.ACTION_START
            }
            startService(serviceIntent)
            callback(true)
        }
    }
    
    private fun stopVpnService() {
        val serviceIntent = Intent(this, ProxyVpnService::class.java).apply {
            action = ProxyVpnService.ACTION_STOP
        }
        startService(serviceIntent)
    }
}