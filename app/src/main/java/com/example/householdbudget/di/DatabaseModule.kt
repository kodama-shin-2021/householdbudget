package com.example.householdbudget.di

import android.content.Context
import androidx.room.Room
import com.example.householdbudget.data.dao.*
import com.example.householdbudget.data.database.HouseholdBudgetDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideHouseholdBudgetDatabase(@ApplicationContext context: Context): HouseholdBudgetDatabase {
        return HouseholdBudgetDatabase.getDatabase(context)
    }

    @Provides
    fun provideTransactionDao(database: HouseholdBudgetDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideCategoryDao(database: HouseholdBudgetDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideSubcategoryDao(database: HouseholdBudgetDatabase): SubcategoryDao {
        return database.subcategoryDao()
    }

    @Provides
    fun provideBudgetDao(database: HouseholdBudgetDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    fun provideRegularTransactionDao(database: HouseholdBudgetDatabase): RegularTransactionDao {
        return database.regularTransactionDao()
    }

    @Provides
    fun provideGoalDao(database: HouseholdBudgetDatabase): GoalDao {
        return database.goalDao()
    }

    @Provides
    fun provideSettingsDao(database: HouseholdBudgetDatabase): SettingsDao {
        return database.settingsDao()
    }

    @Provides
    fun provideNotificationDao(database: HouseholdBudgetDatabase): NotificationDao {
        return database.notificationDao()
    }
}