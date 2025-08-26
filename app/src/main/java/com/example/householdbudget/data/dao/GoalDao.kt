package com.example.householdbudget.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.householdbudget.data.entity.Goal
import com.example.householdbudget.data.entity.GoalType
import java.util.Date

@Dao
interface GoalDao {
    
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllGoals(): LiveData<List<Goal>>
    
    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: Long): Goal?
    
    @Query("SELECT * FROM goals WHERE isAchieved = 0 ORDER BY targetDate ASC")
    fun getActiveGoals(): LiveData<List<Goal>>
    
    @Query("SELECT * FROM goals WHERE isAchieved = 1 ORDER BY achievedAt DESC")
    fun getAchievedGoals(): LiveData<List<Goal>>
    
    @Query("SELECT * FROM goals WHERE type = :type ORDER BY createdAt DESC")
    fun getGoalsByType(type: GoalType): LiveData<List<Goal>>
    
    @Query("SELECT * FROM goals WHERE targetDate <= :date AND isAchieved = 0")
    suspend fun getOverdueGoals(date: Date): List<Goal>
    
    @Query("SELECT * FROM goals WHERE name LIKE '%' || :name || '%'")
    suspend fun searchGoalsByName(name: String): List<Goal>
    
    @Insert
    suspend fun insertGoal(goal: Goal): Long
    
    @Insert
    suspend fun insertGoals(goals: List<Goal>)
    
    @Update
    suspend fun updateGoal(goal: Goal)
    
    @Delete
    suspend fun deleteGoal(goal: Goal)
    
    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)
    
    @Query("UPDATE goals SET isAchieved = 1, achievedAt = :achievedAt WHERE id = :id")
    suspend fun markGoalAsAchieved(id: Long, achievedAt: Date)
}