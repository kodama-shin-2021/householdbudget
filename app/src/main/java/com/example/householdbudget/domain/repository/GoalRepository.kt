package com.example.householdbudget.domain.repository

import androidx.lifecycle.LiveData
import com.example.householdbudget.data.entity.Goal
import com.example.householdbudget.data.entity.GoalType
import java.math.BigDecimal
import java.util.Date

interface GoalRepository {
    
    fun getAllGoals(): LiveData<List<Goal>>
    
    suspend fun getGoalById(id: Long): Goal?
    
    fun getActiveGoals(): LiveData<List<Goal>>
    
    fun getAchievedGoals(): LiveData<List<Goal>>
    
    fun getGoalsByType(type: GoalType): LiveData<List<Goal>>
    
    suspend fun getOverdueGoals(date: Date): List<Goal>
    
    suspend fun searchGoalsByName(name: String): List<Goal>
    
    suspend fun insertGoal(goal: Goal): Long
    
    suspend fun insertGoals(goals: List<Goal>)
    
    suspend fun updateGoal(goal: Goal)
    
    suspend fun deleteGoal(goal: Goal)
    
    suspend fun deleteGoalById(id: Long)
    
    suspend fun markGoalAsAchieved(id: Long, achievedAt: Date)
    
    suspend fun updateGoalProgress(goalId: Long, amount: BigDecimal)
    
    suspend fun calculateGoalProgress(goalId: Long): Float
    
    suspend fun checkGoalAchievements(): List<Goal>
}