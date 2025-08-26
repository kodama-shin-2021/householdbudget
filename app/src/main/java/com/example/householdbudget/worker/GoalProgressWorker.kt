package com.example.householdbudget.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.householdbudget.data.entity.Goal
import com.example.householdbudget.data.entity.GoalStatus
import com.example.householdbudget.data.entity.GoalType
import com.example.householdbudget.domain.repository.CategoryRepository
import com.example.householdbudget.domain.repository.GoalRepository
import com.example.householdbudget.domain.repository.TransactionRepository
import com.example.householdbudget.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.math.BigDecimal
import java.util.Date

@HiltWorker
class GoalProgressWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            updateGoalProgress()
            checkGoalAchievements()
            checkMilestones()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun updateGoalProgress() {
        val activeGoals = goalRepository.getGoalsByStatus(GoalStatus.ACTIVE).value ?: emptyList()
        
        for (goal in activeGoals) {
            val updatedProgress = calculateGoalProgress(goal)
            if (updatedProgress != goal.currentAmount) {
                val updatedGoal = goal.copy(currentAmount = updatedProgress)
                goalRepository.updateGoal(updatedGoal)
                
                // Check if goal is now achieved
                if (updatedProgress >= goal.targetAmount && goal.status != GoalStatus.ACHIEVED) {
                    markGoalAsAchieved(updatedGoal)
                }
            }
        }
    }

    private suspend fun calculateGoalProgress(goal: Goal): BigDecimal {
        return when (goal.type) {
            GoalType.SAVING -> {
                if (goal.categoryId != null) {
                    // Category-specific savings (income in that category)
                    transactionRepository.getCategoryIncomeInPeriod(
                        goal.categoryId,
                        goal.createdDate,
                        Date()
                    )
                } else {
                    // Total savings (income - expenses)
                    val totalIncome = transactionRepository.getTotalIncomeInPeriod(goal.createdDate, Date())
                    val totalExpense = transactionRepository.getTotalExpenseInPeriod(goal.createdDate, Date())
                    totalIncome.subtract(totalExpense).coerceAtLeast(BigDecimal.ZERO)
                }
            }
            GoalType.EXPENSE_REDUCTION -> {
                if (goal.categoryId != null) {
                    // Calculate expense reduction from baseline
                    val currentExpense = transactionRepository.getCategoryExpenseInPeriod(
                        goal.categoryId,
                        goal.createdDate,
                        Date()
                    )
                    // Progress = baseline - current expense (reduction amount)
                    goal.targetAmount.subtract(currentExpense).coerceAtLeast(BigDecimal.ZERO)
                } else {
                    goal.currentAmount
                }
            }
        }
    }

    private suspend fun markGoalAsAchieved(goal: Goal) {
        val achievedGoal = goal.copy(
            status = GoalStatus.ACHIEVED,
            achievedDate = Date()
        )
        goalRepository.updateGoal(achievedGoal)
        
        // Send achievement notification
        notificationHelper.showGoalAchievedNotification(
            goal.name,
            formatCurrency(goal.targetAmount)
        )
    }

    private suspend fun checkGoalAchievements() {
        val activeGoals = goalRepository.getGoalsByStatus(GoalStatus.ACTIVE).value ?: emptyList()
        
        for (goal in activeGoals) {
            val currentProgress = calculateGoalProgress(goal)
            if (currentProgress >= goal.targetAmount) {
                markGoalAsAchieved(goal)
            }
        }
    }

    private suspend fun checkMilestones() {
        val activeGoals = goalRepository.getGoalsByStatus(GoalStatus.ACTIVE).value ?: emptyList()
        
        for (goal in activeGoals) {
            val currentProgress = calculateGoalProgress(goal)
            val progressPercentage = if (goal.targetAmount > BigDecimal.ZERO) {
                (currentProgress.toDouble() / goal.targetAmount.toDouble() * 100).toInt()
            } else {
                0
            }
            
            val milestones = goal.milestoneNotifications?.split(",")?.mapNotNull { 
                it.toIntOrNull() 
            } ?: emptyList()
            
            val passedMilestones = goal.passedMilestones?.split(",")?.mapNotNull { 
                it.toIntOrNull() 
            } ?: emptyList()
            
            for (milestone in milestones) {
                if (progressPercentage >= milestone && milestone !in passedMilestones) {
                    // Send milestone notification
                    notificationHelper.showGoalMilestoneNotification(
                        goal.name,
                        milestone,
                        formatCurrency(currentProgress),
                        formatCurrency(goal.targetAmount)
                    )
                    
                    // Update passed milestones
                    val newPassedMilestones = (passedMilestones + milestone).sorted()
                    val updatedGoal = goal.copy(
                        passedMilestones = newPassedMilestones.joinToString(",")
                    )
                    goalRepository.updateGoal(updatedGoal)
                }
            }
        }
    }

    private fun formatCurrency(amount: BigDecimal): String {
        return "¥${String.format("%,.0f", amount.toFloat())}"
    }
}