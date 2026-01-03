package net.yalab.andromitron.filter

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class FilterManager {
    private val domainFilter = DomainFilter()
    private val cache = ConcurrentHashMap<String, FilterAction>()
    private val filterMutex = Mutex()
    private var isEnabled = true
    
    companion object {
        private const val TAG = "FilterManager"
        private const val CACHE_SIZE_LIMIT = 1000
        
        @Volatile
        private var INSTANCE: FilterManager? = null
        
        fun getInstance(): FilterManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FilterManager().also { INSTANCE = it }
            }
        }
        
        fun resetInstance() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }
    
    suspend fun addRule(domain: String, action: FilterAction, isWildcard: Boolean = false) {
        filterMutex.withLock {
            domainFilter.addRule(domain, action, isWildcard)
            clearCache()
        }
        Log.d(TAG, "Added filter rule: $domain -> $action (wildcard: $isWildcard)")
    }
    
    suspend fun removeRule(domain: String) {
        filterMutex.withLock {
            domainFilter.removeRule(domain)
            clearCache()
        }
        Log.d(TAG, "Removed filter rule: $domain")
    }
    
    suspend fun clearAllRules() {
        filterMutex.withLock {
            domainFilter.clearRules()
            clearCache()
        }
        Log.d(TAG, "Cleared all filter rules")
    }
    
    suspend fun filterDomain(domain: String): FilterAction {
        if (!isEnabled) {
            return FilterAction.ALLOW
        }
        
        val normalizedDomain = domain.lowercase().trim()
        
        cache[normalizedDomain]?.let { 
            Log.v(TAG, "Cache hit for domain: $normalizedDomain -> $it")
            return it 
        }
        
        val action = filterMutex.withLock {
            domainFilter.filterDomain(normalizedDomain)
        }
        
        if (cache.size < CACHE_SIZE_LIMIT) {
            cache[normalizedDomain] = action
        } else if (cache.size >= CACHE_SIZE_LIMIT) {
            clearCache()
            cache[normalizedDomain] = action
        }
        
        Log.v(TAG, "Filtered domain: $normalizedDomain -> $action")
        return action
    }
    
    suspend fun loadDefaultRules() {
        filterMutex.withLock {
            domainFilter.addRule("google.com", FilterAction.ALLOW)
            domainFilter.addRule("*.google.com", FilterAction.ALLOW, isWildcard = true)
            domainFilter.addRule("facebook.com", FilterAction.BLOCK)
            domainFilter.addRule("*.facebook.com", FilterAction.BLOCK, isWildcard = true)
            domainFilter.addRule("*.ads", FilterAction.BLOCK, isWildcard = true)
            domainFilter.addRule("*.doubleclick.net", FilterAction.BLOCK, isWildcard = true)
            clearCache()
        }
        Log.i(TAG, "Loaded default filter rules")
    }
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            clearCache()
        }
        Log.i(TAG, "Filter manager enabled: $enabled")
    }
    
    fun isEnabled(): Boolean = isEnabled
    
    suspend fun getRules(): List<FilterRule> {
        return filterMutex.withLock {
            domainFilter.getRules()
        }
    }
    
    suspend fun getRuleCount(): Int {
        return filterMutex.withLock {
            domainFilter.getRuleCount()
        }
    }
    
    suspend fun hasRule(domain: String): Boolean {
        return filterMutex.withLock {
            domainFilter.hasRule(domain)
        }
    }
    
    fun getCacheSize(): Int = cache.size
    
    private fun clearCache() {
        cache.clear()
        Log.d(TAG, "Cache cleared")
    }
    
    fun getStats(): FilterStats {
        return FilterStats(
            totalRules = domainFilter.getRuleCount(),
            cacheSize = cache.size,
            isEnabled = isEnabled
        )
    }
}

data class FilterStats(
    val totalRules: Int,
    val cacheSize: Int,
    val isEnabled: Boolean
)