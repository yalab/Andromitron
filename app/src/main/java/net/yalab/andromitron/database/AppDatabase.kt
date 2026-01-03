package net.yalab.andromitron.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.yalab.andromitron.filter.FilterAction

@Database(
    entities = [FilterRuleEntity::class, ConnectionLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun filterRuleDao(): FilterRuleDao
    abstract fun connectionLogDao(): ConnectionLogDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "andromitron_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        fun getInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            ).build()
        }
        
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database.filterRuleDao())
                    }
                }
            }
        }
        
        private suspend fun populateDatabase(filterRuleDao: FilterRuleDao) {
            // Insert default filter rules
            val defaultRules = listOf(
                FilterRuleEntity(
                    domain = "*.ads",
                    action = FilterAction.BLOCK.name,
                    isWildcard = true
                ),
                FilterRuleEntity(
                    domain = "*.doubleclick.net",
                    action = FilterAction.BLOCK.name,
                    isWildcard = true
                ),
                FilterRuleEntity(
                    domain = "*.googleadservices.com",
                    action = FilterAction.BLOCK.name,
                    isWildcard = true
                ),
                FilterRuleEntity(
                    domain = "google.com",
                    action = FilterAction.ALLOW.name,
                    isWildcard = false
                ),
                FilterRuleEntity(
                    domain = "*.google.com",
                    action = FilterAction.ALLOW.name,
                    isWildcard = true
                ),
                FilterRuleEntity(
                    domain = "github.com",
                    action = FilterAction.ALLOW.name,
                    isWildcard = false
                ),
                FilterRuleEntity(
                    domain = "*.github.com",
                    action = FilterAction.ALLOW.name,
                    isWildcard = true
                )
            )
            
            filterRuleDao.insertRules(defaultRules)
        }
    }
}