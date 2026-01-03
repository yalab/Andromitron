package net.yalab.andromitron.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.yalab.andromitron.filter.FilterAction
import net.yalab.andromitron.filter.FilterRule

class DatabaseRepository private constructor(
    private val filterRuleDao: FilterRuleDao,
    private val connectionLogDao: ConnectionLogDao
) {
    
    companion object {
        @Volatile
        private var INSTANCE: DatabaseRepository? = null
        
        fun getInstance(context: Context): DatabaseRepository {
            return INSTANCE ?: synchronized(this) {
                val database = AppDatabase.getDatabase(context)
                val instance = DatabaseRepository(
                    database.filterRuleDao(),
                    database.connectionLogDao()
                )
                INSTANCE = instance
                instance
            }
        }
        
        fun getInMemoryInstance(context: Context): DatabaseRepository {
            val database = AppDatabase.getInMemoryDatabase(context)
            return DatabaseRepository(
                database.filterRuleDao(),
                database.connectionLogDao()
            )
        }
    }
    
    // Filter Rules Operations
    fun getAllFilterRules(): LiveData<List<FilterRule>> {
        return filterRuleDao.getAllRules().map { entities ->
            entities.map { it.toFilterRule() }
        }.asLiveData()
    }
    
    suspend fun getAllFilterRulesList(): List<FilterRule> {
        return filterRuleDao.getAllRulesList().map { it.toFilterRule() }
    }
    
    suspend fun getFilterRuleByDomain(domain: String): FilterRule? {
        return filterRuleDao.getRuleByDomain(domain)?.toFilterRule()
    }
    
    suspend fun insertFilterRule(rule: FilterRule): Long {
        return filterRuleDao.insertRule(FilterRuleEntity.fromFilterRule(rule))
    }
    
    suspend fun insertFilterRules(rules: List<FilterRule>): List<Long> {
        val entities = rules.map { FilterRuleEntity.fromFilterRule(it) }
        return filterRuleDao.insertRules(entities)
    }
    
    suspend fun updateFilterRuleAction(domain: String, action: FilterAction) {
        filterRuleDao.updateRuleAction(domain, action.name)
    }
    
    suspend fun deleteFilterRule(domain: String) {
        filterRuleDao.deleteRuleByDomain(domain)
    }
    
    suspend fun deleteAllFilterRules() {
        filterRuleDao.deleteAllRules()
    }
    
    suspend fun getFilterRuleCount(): Int {
        return filterRuleDao.getRuleCount()
    }
    
    suspend fun searchFilterRules(query: String, limit: Int = 50, offset: Int = 0): List<FilterRule> {
        val searchQuery = "%$query%"
        val wildcardQuery = "%*.$query%"
        return filterRuleDao.searchRules(searchQuery, wildcardQuery, limit, offset)
            .map { it.toFilterRule() }
    }
    
    // Connection Logs Operations
    fun getRecentConnectionLogs(limit: Int = 100): LiveData<List<ConnectionLogEntity>> {
        return connectionLogDao.getRecentLogs(limit).asLiveData()
    }
    
    suspend fun getConnectionLogsPaged(limit: Int, offset: Int): List<ConnectionLogEntity> {
        return connectionLogDao.getLogsPaged(limit, offset)
    }
    
    suspend fun getConnectionLogsByDomain(domain: String): List<ConnectionLogEntity> {
        return connectionLogDao.getLogsByDomain(domain)
    }
    
    suspend fun insertConnectionLog(log: ConnectionLogEntity): Long {
        return connectionLogDao.insertLog(log)
    }
    
    suspend fun insertConnectionLogs(logs: List<ConnectionLogEntity>): List<Long> {
        return connectionLogDao.insertLogs(logs)
    }
    
    suspend fun deleteOldConnectionLogs(olderThanMillis: Long) {
        connectionLogDao.deleteOldLogs(olderThanMillis)
    }
    
    suspend fun deleteAllConnectionLogs() {
        connectionLogDao.deleteAllLogs()
    }
    
    suspend fun getConnectionLogCount(): Int {
        return connectionLogDao.getLogCount()
    }
    
    suspend fun getConnectionLogsByAction(action: String, limit: Int = 100): List<ConnectionLogEntity> {
        return connectionLogDao.getLogsByAction(action, limit)
    }
    
    suspend fun getConnectionLogsByTimeRange(startTime: Long, endTime: Long): List<ConnectionLogEntity> {
        return connectionLogDao.getLogsByTimeRange(startTime, endTime)
    }
    
    suspend fun getTotalBytesInTimeRange(startTime: Long, endTime: Long): Long {
        return connectionLogDao.getTotalBytesInTimeRange(startTime, endTime) ?: 0L
    }
    
    suspend fun getTopDomainsByCount(startTime: Long, limit: Int = 10): List<DomainCountResult> {
        return connectionLogDao.getTopDomainsByCount(startTime, limit)
    }
    
    suspend fun getActionStatistics(startTime: Long): List<ActionCountResult> {
        return connectionLogDao.getActionStatistics(startTime)
    }
    
    // Maintenance Operations
    suspend fun cleanupOldData(maxAgeMillis: Long) {
        val cutoffTime = System.currentTimeMillis() - maxAgeMillis
        deleteOldConnectionLogs(cutoffTime)
    }
    
    suspend fun getDatabaseStats(): DatabaseStats {
        val filterRuleCount = getFilterRuleCount()
        val connectionLogCount = getConnectionLogCount()
        val now = System.currentTimeMillis()
        val last24Hours = now - (24 * 60 * 60 * 1000)
        val recentLogCount = connectionLogDao.getLogCountByTimeRange(last24Hours, now)
        
        return DatabaseStats(
            filterRuleCount = filterRuleCount,
            totalConnectionLogCount = connectionLogCount,
            recentConnectionLogCount = recentLogCount
        )
    }
}

data class DatabaseStats(
    val filterRuleCount: Int,
    val totalConnectionLogCount: Int,
    val recentConnectionLogCount: Int
)