package net.yalab.andromitron.filter

data class FilterRule(
    val domain: String,
    val action: FilterAction,
    val isWildcard: Boolean = false
)

enum class FilterAction {
    BLOCK,
    ALLOW,
    PROXY
}

class DomainFilter {
    private val rules = mutableListOf<FilterRule>()
    private val exactRules = mutableMapOf<String, FilterAction>()
    private val wildcardRules = mutableListOf<FilterRule>()
    
    fun addRule(domain: String, action: FilterAction, isWildcard: Boolean = false) {
        val normalizedDomain = domain.lowercase().trim()
        val rule = FilterRule(normalizedDomain, action, isWildcard)
        rules.add(rule)
        
        if (isWildcard) {
            wildcardRules.add(rule)
        } else {
            exactRules[normalizedDomain] = action
        }
    }
    
    fun removeRule(domain: String) {
        val normalizedDomain = domain.lowercase().trim()
        rules.removeAll { it.domain == normalizedDomain }
        exactRules.remove(normalizedDomain)
        wildcardRules.removeAll { it.domain == normalizedDomain }
    }
    
    fun clearRules() {
        rules.clear()
        exactRules.clear()
        wildcardRules.clear()
    }
    
    fun filterDomain(domain: String): FilterAction {
        val normalizedDomain = domain.lowercase().trim()
        
        exactRules[normalizedDomain]?.let { return it }
        
        for (rule in wildcardRules) {
            if (matchesWildcard(normalizedDomain, rule.domain)) {
                return rule.action
            }
        }
        
        return FilterAction.ALLOW
    }
    
    private fun matchesWildcard(domain: String, pattern: String): Boolean {
        val normalizedPattern = pattern.lowercase().trim()
        val normalizedDomain = domain.lowercase().trim()
        
        return when {
            normalizedPattern.startsWith("*.") -> {
                val suffix = normalizedPattern.substring(2)
                normalizedDomain.endsWith(".$suffix") || normalizedDomain == suffix
            }
            normalizedPattern.contains("*") -> {
                val regex = normalizedPattern
                    .replace(".", "\\.")
                    .replace("*", ".*")
                normalizedDomain.matches(regex.toRegex())
            }
            else -> normalizedDomain == normalizedPattern
        }
    }
    
    fun getRules(): List<FilterRule> = rules.toList()
    
    fun getRuleCount(): Int = rules.size
    
    fun hasRule(domain: String): Boolean {
        val normalizedDomain = domain.lowercase().trim()
        return rules.any { it.domain == normalizedDomain }
    }
    
    fun getActionForDomain(domain: String): FilterAction? {
        val normalizedDomain = domain.lowercase().trim()
        return exactRules[normalizedDomain] 
            ?: wildcardRules.firstOrNull { matchesWildcard(normalizedDomain, it.domain) }?.action
    }
}