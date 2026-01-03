package net.yalab.andromitron.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import net.yalab.andromitron.filter.FilterAction
import net.yalab.andromitron.filter.FilterRule

@Entity(
    tableName = "filter_rules",
    indices = [
        Index(value = ["domain"], unique = true),
        Index(value = ["action"]),
        Index(value = ["is_wildcard"])
    ]
)
data class FilterRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "domain")
    val domain: String,
    
    @ColumnInfo(name = "action")
    val action: String,
    
    @ColumnInfo(name = "is_wildcard")
    val isWildcard: Boolean = false,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toFilterRule(): FilterRule {
        return FilterRule(
            domain = domain,
            action = FilterAction.valueOf(action),
            isWildcard = isWildcard
        )
    }
    
    companion object {
        fun fromFilterRule(rule: FilterRule): FilterRuleEntity {
            return FilterRuleEntity(
                domain = rule.domain,
                action = rule.action.name,
                isWildcard = rule.isWildcard
            )
        }
    }
}