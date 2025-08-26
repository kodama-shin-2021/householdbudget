package com.example.householdbudget.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.householdbudget.data.dao.*
import com.example.householdbudget.data.entity.*

@Database(
    entities = [
        Transaction::class,
        Category::class,
        Subcategory::class,
        Budget::class,
        RegularTransaction::class,
        Goal::class,
        Settings::class,
        Notification::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HouseholdBudgetDatabase : RoomDatabase() {
    
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun subcategoryDao(): SubcategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun regularTransactionDao(): RegularTransactionDao
    abstract fun goalDao(): GoalDao
    abstract fun settingsDao(): SettingsDao
    abstract fun notificationDao(): NotificationDao
    
    companion object {
        @Volatile
        private var INSTANCE: HouseholdBudgetDatabase? = null
        
        const val DATABASE_NAME = "household_budget_database"
        
        fun getDatabase(context: Context): HouseholdBudgetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HouseholdBudgetDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // 初期データの挿入は Repository で行う
            }
        }
        
        // 将来のマイグレーション用
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // マイグレーション処理をここに記述
            }
        }
    }
}