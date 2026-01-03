package net.yalab.andromitron.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionLogDao {
    
    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<ConnectionLogEntity>>
    
    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getLogsPaged(limit: Int, offset: Int): List<ConnectionLogEntity>
    
    @Query("SELECT * FROM connection_logs WHERE domain = :domain ORDER BY timestamp DESC")
    suspend fun getLogsByDomain(domain: String): List<ConnectionLogEntity>
    
    @Query("SELECT * FROM connection_logs WHERE action = :action ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLogsByAction(action: String, limit: Int = 100): List<ConnectionLogEntity>
    
    @Query("SELECT * FROM connection_logs WHERE protocol = :protocol ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLogsByProtocol(protocol: String, limit: Int = 100): List<ConnectionLogEntity>
    
    @Query("SELECT * FROM connection_logs WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getLogsByTimeRange(startTime: Long, endTime: Long): List<ConnectionLogEntity>
    
    @Insert
    suspend fun insertConnectionLog(log: ConnectionLogEntity): Long
    
    @Insert
    suspend fun insertLog(log: ConnectionLogEntity): Long
    
    @Insert
    suspend fun insertLogs(logs: List<ConnectionLogEntity>): List<Long>
    
    @Query("DELETE FROM connection_logs WHERE timestamp < :olderThan")
    suspend fun deleteOldLogs(olderThan: Long)
    
    @Query("DELETE FROM connection_logs")
    suspend fun deleteAllLogs()
    
    @Query("DELETE FROM connection_logs WHERE id IN (SELECT id FROM connection_logs ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldestLogs(count: Int)
    
    @Query("SELECT COUNT(*) FROM connection_logs")
    suspend fun getLogCount(): Int
    
    @Query("SELECT COUNT(*) FROM connection_logs WHERE action = :action")
    suspend fun getLogCountByAction(action: String): Int
    
    @Query("SELECT COUNT(*) FROM connection_logs WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getLogCountByTimeRange(startTime: Long, endTime: Long): Int
    
    @Query("SELECT SUM(bytes_sent + bytes_received) FROM connection_logs WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getTotalBytesInTimeRange(startTime: Long, endTime: Long): Long?
    
    @Query("SELECT domain, COUNT(*) as count FROM connection_logs WHERE domain IS NOT NULL AND timestamp >= :startTime GROUP BY domain ORDER BY count DESC LIMIT :limit")
    suspend fun getTopDomainsByCount(startTime: Long, limit: Int = 10): List<DomainCountResult>
    
    @Query("SELECT action, COUNT(*) as count FROM connection_logs WHERE timestamp >= :startTime GROUP BY action ORDER BY count DESC")
    suspend fun getActionStatistics(startTime: Long): List<ActionCountResult>
    
    @Query("""
        SELECT 
            (SELECT COUNT(*) FROM connection_logs WHERE timestamp >= :startTime) as totalConnections,
            (SELECT COUNT(*) FROM connection_logs WHERE action = 'BLOCK' AND timestamp >= :startTime) as blockedCount,
            (SELECT COUNT(*) FROM connection_logs WHERE action = 'ALLOW' AND timestamp >= :startTime) as allowedCount,
            (SELECT COUNT(*) FROM connection_logs WHERE action = 'PROXY' AND timestamp >= :startTime) as proxiedCount,
            (SELECT COALESCE(SUM(bytes_sent), 0) FROM connection_logs WHERE timestamp >= :startTime) as totalBytesSent,
            (SELECT COALESCE(SUM(bytes_received), 0) FROM connection_logs WHERE timestamp >= :startTime) as totalBytesReceived
    """)
    suspend fun getStatsSince(startTime: Long): ConnectionStats
}

data class DomainCountResult(
    val domain: String,
    val count: Int
)

data class ActionCountResult(
    val action: String,
    val count: Int
)

data class ConnectionStats(
    val totalConnections: Int,
    val blockedCount: Int,
    val allowedCount: Int,
    val proxiedCount: Int,
    val totalBytesSent: Long,
    val totalBytesReceived: Long
)