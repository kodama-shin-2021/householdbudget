package com.example.householdbudget.data.repository

import androidx.lifecycle.LiveData
import com.example.householdbudget.data.dao.GoalDao
import com.example.householdbudget.data.entity.Goal
import com.example.householdbudget.data.entity.GoalType
import com.example.householdbudget.domain.repository.GoalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao
) : GoalRepository {

    override fun getAllGoals(): LiveData<List<Goal>> {
        return goalDao.getAllGoals()
    }

    override suspend fun getGoalById(id: Long): Goal? {
        return withContext(Dispatchers.IO) {
            goalDao.getGoalById(id)
        }
    }

    override fun getActiveGoals(): LiveData<List<Goal>> {
        return goalDao.getActiveGoals()
    }

    override fun getAchievedGoals(): LiveData<List<Goal>> {
        return goalDao.getAchievedGoals()
    }

    override fun getGoalsByType(type: GoalType): LiveData<List<Goal>> {
        return goalDao.getGoalsByType(type)
    }

    override suspend fun getOverdueGoals(date: Date): List<Goal> {
        return withContext(Dispatchers.IO) {
            goalDao.getOverdueGoals(date)
        }
    }

    override suspend fun searchGoalsByName(name: String): List<Goal> {
        return withContext(Dispatchers.IO) {
            goalDao.searchGoalsByName(name)
        }
    }

    override suspend fun insertGoal(goal: Goal): Long {
        return withContext(Dispatchers.IO) {
            goalDao.insertGoal(goal)
        }
    }

    override suspend fun insertGoals(goals: List<Goal>) {
        withContext(Dispatchers.IO) {
            goalDao.insertGoals(goals)
        }
    }

    override suspend fun updateGoal(goal: Goal) {
        withContext(Dispatchers.IO) {
            goalDao.updateGoal(goal.copy(updatedAt = Date()))
        }
    }

    override suspend fun deleteGoal(goal: Goal) {
        withContext(Dispatchers.IO) {
            goalDao.deleteGoal(goal)
        }
    }

    override suspend fun deleteGoalById(id: Long) {
        withContext(Dispatchers.IO) {
            goalDao.deleteGoalById(id)
        }
    }

    override suspend fun markGoalAsAchieved(id: Long, achievedAt: Date) {
        withContext(Dispatchers.IO) {
            goalDao.markGoalAsAchieved(id, achievedAt)
        }
    }

    override suspend fun updateGoalProgress(goalId: Long, amount: BigDecimal) {
        withContext(Dispatchers.IO) {
            val goal = goalDao.getGoalById(goalId)
            if (goal != null) {
                val updatedGoal = goal.copy(
                    currentAmount = amount,
                    updatedAt = Date(),
                    isAchieved = amount >= goal.targetAmount,
                    achievedAt = if (amount >= goal.targetAmount && !goal.isAchieved) Date() else goal.achievedAt
                )
                goalDao.updateGoal(updatedGoal)
            }
        }
    }

    override suspend fun calculateGoalProgress(goalId: Long): Float {
        return withContext(Dispatchers.IO) {
            val goal = goalDao.getGoalById(goalId)
            if (goal != null && goal.targetAmount > BigDecimal.ZERO) {
                (goal.currentAmount.toFloat() / goal.targetAmount.toFloat()).coerceAtMost(1.0f)
            } else {
                0.0f
            }
        }
    }

    override suspend fun checkGoalAchievements(): List<Goal> {
        return withContext(Dispatchers.IO) {
            val activeGoals = goalDao.getActiveGoals().value ?: emptyList()
            activeGoals.filter { goal ->
                goal.currentAmount >= goal.targetAmount && !goal.isAchieved
            }
        }
    }
}