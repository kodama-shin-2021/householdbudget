package com.example.householdbudget.domain.repository

import androidx.lifecycle.LiveData
import com.example.householdbudget.data.entity.Budget
import com.example.householdbudget.data.entity.BudgetPeriod
import java.math.BigDecimal
import java.util.Date

interface BudgetRepository {
    
    fun getActiveBudgets(): LiveData<List<Budget>>
    
    fun getAllBudgets(): LiveData<List<Budget>>
    
    suspend fun getBudgetById(id: Long): Budget?
    
    fun getBudgetsByCategory(categoryId: Long): LiveData<List<Budget>>
    
    fun getBudgetsByPeriod(period: BudgetPeriod): LiveData<List<Budget>>
    
    fun getCurrentBudgets(currentDate: Date): LiveData<List<Budget>>
    
    suspend fun getCurrentBudgetForCategory(categoryId: Long, currentDate: Date): Budget?
    
    suspend fun getCurrentBudgetForSubcategory(subcategoryId: Long, currentDate: Date): Budget?
    
    suspend fun insertBudget(budget: Budget): Long
    
    suspend fun insertBudgets(budgets: List<Budget>)
    
    suspend fun updateBudget(budget: Budget)
    
    suspend fun deleteBudget(budget: Budget)
    
    suspend fun deleteBudgetById(id: Long)
    
    suspend fun deactivateBudget(id: Long)
    
    suspend fun activateBudget(id: Long)
    
    suspend fun getBudgetUsage(budgetId: Long): BigDecimal
    
    suspend fun getBudgetProgress(budgetId: Long): Float
    
    suspend fun checkBudgetAlerts(): List<Budget>
}