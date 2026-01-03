package net.yalab.andromitron.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class ProxyVpnService : VpnService() {
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    companion object {
        private const val TAG = "ProxyVpnService"
        private const val NOTIFICATION_CHANNEL_ID = "vpn_proxy_channel"
        private const val NOTIFICATION_ID = 1
        private const val VPN_MTU = 1500
        private const val VPN_ADDRESS = "10.0.0.1"
        private const val VPN_ROUTE = "0.0.0.0"
        private const val DNS_SERVER = "8.8.8.8"
        
        const val ACTION_START = "start_vpn"
        const val ACTION_STOP = "stop_vpn"
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand: action=$action")
        
        when (action) {
            ACTION_START -> startVpnService()
            ACTION_STOP -> stopVpnService()
            else -> {
                Log.w(TAG, "Unknown action: $action")
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun startVpnService() {
        if (vpnInterface != null) {
            Log.d(TAG, "VPN service is already running")
            return
        }
        
        try {
            createNotificationChannel()
            val notification = createNotification()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID, 
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            
            establish()
            
            serviceJob = serviceScope.launch {
                runVpnLoop()
            }
            
            Log.i(TAG, "VPN service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN service", e)
            stopVpnService()
        }
    }
    
    private fun establish(): Boolean {
        val builder = Builder()
        
        builder.setMtu(VPN_MTU)
            .addAddress(VPN_ADDRESS, 24)
            .addRoute(VPN_ROUTE, 0)
            .addDnsServer(DNS_SERVER)
            .setSession("AndromitronVPN")
        
        createConfigureIntent()?.let { builder.setConfigureIntent(it) }
        
        vpnInterface = builder.establish()
        
        return vpnInterface != null
    }
    
    private fun createConfigureIntent(): PendingIntent? {
        val intent = Intent()
        intent.setClassName(packageName, "net.yalab.andromitron.MainActivity")
        
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private suspend fun runVpnLoop() {
        val vpnInput = FileInputStream(vpnInterface?.fileDescriptor)
        val vpnOutput = FileOutputStream(vpnInterface?.fileDescriptor)
        
        val buffer = ByteArray(VPN_MTU)
        
        try {
            while (vpnInterface != null && serviceJob?.isActive == true) {
                val length = vpnInput.read(buffer)
                if (length > 0) {
                    processPacket(ByteBuffer.wrap(buffer, 0, length))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in VPN loop", e)
        } finally {
            vpnInput.close()
            vpnOutput.close()
        }
    }
    
    private fun processPacket(packet: ByteBuffer) {
        Log.v(TAG, "Processing packet: ${packet.remaining()} bytes")
    }
    
    private fun stopVpnService() {
        Log.i(TAG, "Stopping VPN service")
        
        serviceJob?.cancel()
        serviceJob = null
        
        vpnInterface?.close()
        vpnInterface = null
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "VPN Proxy Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for VPN proxy service"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Andromitron VPN")
            .setContentText("VPN proxy is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopVpnService()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }
    
    override fun onRevoke() {
        super.onRevoke()
        Log.i(TAG, "VPN permission revoked")
        stopVpnService()
    }
}