package net.yalab.andromitron.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterRuleDao {
    
    @Query("SELECT * FROM filter_rules ORDER BY domain ASC")
    fun getAllRules(): Flow<List<FilterRuleEntity>>
    
    @Query("SELECT * FROM filter_rules ORDER BY domain ASC")
    suspend fun getAllRulesList(): List<FilterRuleEntity>
    
    @Query("SELECT * FROM filter_rules WHERE domain = :domain LIMIT 1")
    suspend fun getRuleByDomain(domain: String): FilterRuleEntity?
    
    @Query("SELECT * FROM filter_rules WHERE action = :action ORDER BY domain ASC")
    suspend fun getRulesByAction(action: String): List<FilterRuleEntity>
    
    @Query("SELECT * FROM filter_rules WHERE is_wildcard = :isWildcard ORDER BY domain ASC")
    suspend fun getRulesByWildcardStatus(isWildcard: Boolean): List<FilterRuleEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: FilterRuleEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<FilterRuleEntity>): List<Long>
    
    @Update
    suspend fun updateRule(rule: FilterRuleEntity)
    
    @Query("UPDATE filter_rules SET action = :action, updated_at = :updatedAt WHERE domain = :domain")
    suspend fun updateRuleAction(domain: String, action: String, updatedAt: Long = System.currentTimeMillis())
    
    @Delete
    suspend fun deleteRule(rule: FilterRuleEntity)
    
    @Query("DELETE FROM filter_rules WHERE domain = :domain")
    suspend fun deleteRuleByDomain(domain: String)
    
    @Query("DELETE FROM filter_rules")
    suspend fun deleteAllRules()
    
    @Query("SELECT COUNT(*) FROM filter_rules")
    suspend fun getRuleCount(): Int
    
    @Query("SELECT COUNT(*) FROM filter_rules WHERE action = :action")
    suspend fun getRuleCountByAction(action: String): Int
    
    @Query("SELECT COUNT(*) FROM filter_rules WHERE is_wildcard = :isWildcard")
    suspend fun getRuleCountByWildcardStatus(isWildcard: Boolean): Int
    
    @Query("SELECT * FROM filter_rules WHERE domain LIKE :searchQuery OR domain LIKE :wildcardQuery ORDER BY domain ASC LIMIT :limit OFFSET :offset")
    suspend fun searchRules(searchQuery: String, wildcardQuery: String, limit: Int, offset: Int): List<FilterRuleEntity>
}