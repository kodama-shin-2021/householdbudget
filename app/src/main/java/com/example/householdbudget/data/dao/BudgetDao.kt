package com.example.householdbudget.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.householdbudget.data.entity.Budget
import com.example.householdbudget.data.entity.BudgetPeriod
import java.util.Date

@Dao
interface BudgetDao {
    
    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY startDate DESC")
    fun getActiveBudgets(): LiveData<List<Budget>>
    
    @Query("SELECT * FROM budgets ORDER BY startDate DESC")
    fun getAllBudgets(): LiveData<List<Budget>>
    
    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Long): Budget?
    
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND isActive = 1")
    fun getBudgetsByCategory(categoryId: Long): LiveData<List<Budget>>
    
    @Query("SELECT * FROM budgets WHERE period = :period AND isActive = 1")
    fun getBudgetsByPeriod(period: BudgetPeriod): LiveData<List<Budget>>
    
    @Query("SELECT * FROM budgets WHERE :currentDate BETWEEN startDate AND COALESCE(endDate, :currentDate) AND isActive = 1")
    fun getCurrentBudgets(currentDate: Date): LiveData<List<Budget>>
    
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND :currentDate BETWEEN startDate AND COALESCE(endDate, :currentDate) AND isActive = 1")
    suspend fun getCurrentBudgetForCategory(categoryId: Long, currentDate: Date): Budget?
    
    @Query("SELECT * FROM budgets WHERE subcategoryId = :subcategoryId AND :currentDate BETWEEN startDate AND COALESCE(endDate, :currentDate) AND isActive = 1")
    suspend fun getCurrentBudgetForSubcategory(subcategoryId: Long, currentDate: Date): Budget?
    
    @Insert
    suspend fun insertBudget(budget: Budget): Long
    
    @Insert
    suspend fun insertBudgets(budgets: List<Budget>)
    
    @Update
    suspend fun updateBudget(budget: Budget)
    
    @Delete
    suspend fun deleteBudget(budget: Budget)
    
    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)
    
    @Query("UPDATE budgets SET isActive = 0 WHERE id = :id")
    suspend fun deactivateBudget(id: Long)
    
    @Query("UPDATE budgets SET isActive = 1 WHERE id = :id")
    suspend fun activateBudget(id: Long)
}