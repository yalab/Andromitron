package net.yalab.andromitron.logging

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.yalab.andromitron.database.AppDatabase
import net.yalab.andromitron.database.ConnectionLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConnectionLogger(context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val connectionLogDao = database.connectionLogDao()
    private val logScope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "ConnectionLogger"
        private const val MAX_LOGS = 10000 // Maximum number of logs to keep
        
        @Volatile
        private var INSTANCE: ConnectionLogger? = null
        
        fun getInstance(context: Context): ConnectionLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConnectionLogger(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    fun logConnection(
        domain: String? = null,
        sourceIp: String,
        destinationIp: String,
        sourcePort: Int? = null,
        destinationPort: Int? = null,
        protocol: String,
        action: String,
        bytesSent: Long = 0,
        bytesReceived: Long = 0
    ) {
        val timestamp = System.currentTimeMillis()
        val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        
        // Console logging
        val logMessage = buildString {
            append("[$formattedTime] ")
            append("${action.uppercase()} ")
            append("$protocol ")
            append("$sourceIp")
            sourcePort?.let { append(":$it") }
            append(" -> ")
            append("$destinationIp")
            destinationPort?.let { append(":$it") }
            domain?.let { append(" ($it)") }
            if (bytesSent > 0 || bytesReceived > 0) {
                append(" [↑${formatBytes(bytesSent)} ↓${formatBytes(bytesReceived)}]")
            }
        }
        
        when (action.lowercase()) {
            "allow" -> Log.i(TAG, logMessage)
            "block" -> Log.w(TAG, logMessage)
            "proxy" -> Log.d(TAG, logMessage)
            else -> Log.v(TAG, logMessage)
        }
        
        // Database logging
        logScope.launch {
            try {
                val logEntity = ConnectionLogEntity(
                    timestamp = timestamp,
                    domain = domain,
                    sourceIp = sourceIp,
                    destinationIp = destinationIp,
                    sourcePort = sourcePort,
                    destinationPort = destinationPort,
                    protocol = protocol,
                    action = action,
                    bytesSent = bytesSent,
                    bytesReceived = bytesReceived
                )
                
                connectionLogDao.insertConnectionLog(logEntity)
                
                // Clean up old logs to prevent database bloat
                cleanupOldLogs()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save connection log to database", e)
            }
        }
    }
    
    fun logVpnSessionStart() {
        val timestamp = System.currentTimeMillis()
        val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        
        Log.i(TAG, "====== VPN SESSION STARTED at $formattedTime ======")
        Log.i(TAG, "AndromitronVPN: Traffic filtering and proxy service activated")
        Log.i(TAG, "Monitoring network connections for domain filtering...")
        
        logScope.launch {
            try {
                val sessionStartLog = ConnectionLogEntity(
                    timestamp = timestamp,
                    domain = null,
                    sourceIp = "system",
                    destinationIp = "system",
                    sourcePort = null,
                    destinationPort = null,
                    protocol = "VPN",
                    action = "SESSION_START",
                    bytesSent = 0,
                    bytesReceived = 0
                )
                connectionLogDao.insertConnectionLog(sessionStartLog)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log VPN session start", e)
            }
        }
    }
    
    fun logVpnSessionStop() {
        val timestamp = System.currentTimeMillis()
        val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        
        Log.i(TAG, "====== VPN SESSION STOPPED at $formattedTime ======")
        Log.i(TAG, "AndromitronVPN: Traffic filtering and proxy service deactivated")
        
        logScope.launch {
            try {
                val sessionStopLog = ConnectionLogEntity(
                    timestamp = timestamp,
                    domain = null,
                    sourceIp = "system",
                    destinationIp = "system",
                    sourcePort = null,
                    destinationPort = null,
                    protocol = "VPN",
                    action = "SESSION_STOP",
                    bytesSent = 0,
                    bytesReceived = 0
                )
                connectionLogDao.insertConnectionLog(sessionStopLog)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log VPN session stop", e)
            }
        }
    }
    
    fun logDomainBlocked(domain: String, destinationIp: String, protocol: String, port: Int?) {
        Log.w(TAG, "🚫 BLOCKED: $domain ($destinationIp) - $protocol${port?.let { ":$it" } ?: ""}")
        
        logConnection(
            domain = domain,
            sourceIp = "local",
            destinationIp = destinationIp,
            sourcePort = null,
            destinationPort = port,
            protocol = protocol,
            action = "BLOCK"
        )
    }
    
    fun logDomainAllowed(domain: String, destinationIp: String, protocol: String, port: Int?) {
        Log.i(TAG, "✅ ALLOWED: $domain ($destinationIp) - $protocol${port?.let { ":$it" } ?: ""}")
        
        logConnection(
            domain = domain,
            sourceIp = "local", 
            destinationIp = destinationIp,
            sourcePort = null,
            destinationPort = port,
            protocol = protocol,
            action = "ALLOW"
        )
    }
    
    fun logProxiedConnection(
        sourceIp: String,
        destinationIp: String,
        protocol: String,
        sourcePort: Int?,
        destinationPort: Int?,
        domain: String? = null
    ) {
        Log.d(TAG, "🔄 PROXIED: $sourceIp${sourcePort?.let { ":$it" } ?: ""} -> $destinationIp${destinationPort?.let { ":$it" } ?: ""}${domain?.let { " ($it)" } ?: ""}")
        
        logConnection(
            domain = domain,
            sourceIp = sourceIp,
            destinationIp = destinationIp,
            sourcePort = sourcePort,
            destinationPort = destinationPort,
            protocol = protocol,
            action = "PROXY"
        )
    }
    
    fun logPacketDetails(
        sourceIp: String,
        destinationIp: String,
        protocol: String,
        sourcePort: Int?,
        destinationPort: Int?,
        payloadSize: Int,
        domain: String? = null
    ) {
        Log.v(TAG, "📦 PACKET: $protocol $sourceIp${sourcePort?.let { ":$it" } ?: ""} -> $destinationIp${destinationPort?.let { ":$it" } ?: ""} (${formatBytes(payloadSize.toLong())})${domain?.let { " - $it" } ?: ""}")
    }
    
    fun logStatistics() {
        logScope.launch {
            try {
                val today = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                val stats = connectionLogDao.getStatsSince(today)
                
                Log.i(TAG, "📊 Daily Statistics:")
                Log.i(TAG, "   Total connections: ${stats.totalConnections}")
                Log.i(TAG, "   Blocked: ${stats.blockedCount}")
                Log.i(TAG, "   Allowed: ${stats.allowedCount}")
                Log.i(TAG, "   Proxied: ${stats.proxiedCount}")
                Log.i(TAG, "   Data transferred: ↑${formatBytes(stats.totalBytesSent)} ↓${formatBytes(stats.totalBytesReceived)}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get statistics", e)
            }
        }
    }
    
    private suspend fun cleanupOldLogs() {
        try {
            val logCount = connectionLogDao.getLogCount()
            if (logCount > MAX_LOGS) {
                val deleteCount = logCount - MAX_LOGS + (MAX_LOGS / 10) // Delete 10% extra
                connectionLogDao.deleteOldestLogs(deleteCount.toInt())
                Log.d(TAG, "Cleaned up $deleteCount old log entries")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old logs", e)
        }
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            else -> "${bytes / (1024 * 1024 * 1024)}GB"
        }
    }
}