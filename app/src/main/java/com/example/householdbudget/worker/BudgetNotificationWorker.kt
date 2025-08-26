package com.example.householdbudget.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.householdbudget.data.entity.Budget
import com.example.householdbudget.domain.repository.BudgetRepository
import com.example.householdbudget.domain.repository.CategoryRepository
import com.example.householdbudget.domain.repository.TransactionRepository
import com.example.householdbudget.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.math.BigDecimal
import java.util.Calendar
import java.util.Date

@HiltWorker
class BudgetNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            checkBudgetAlerts()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun checkBudgetAlerts() {
        val activeBudgets = budgetRepository.getActiveBudgets().value ?: emptyList()
        val categories = categoryRepository.getAllCategories().value ?: emptyList()

        for (budget in activeBudgets) {
            val category = categories.find { it.id == budget.categoryId }
            if (category != null) {
                checkCategoryBudget(budget, category.name)
            }
        }

        // Check total budget
        checkTotalBudget(activeBudgets)
    }

    private suspend fun checkCategoryBudget(budget: Budget, categoryName: String) {
        val dateRange = getCurrentPeriodRange(budget)
        val usedAmount = transactionRepository.getCategoryExpenseInPeriod(
            budget.categoryId,
            dateRange.first,
            dateRange.second
        )

        val progress = if (budget.budgetAmount > BigDecimal.ZERO) {
            (usedAmount.toDouble() / budget.budgetAmount.toDouble() * 100).toInt()
        } else {
            0
        }

        when {
            progress > 100 -> {
                // Budget exceeded
                val overAmount = usedAmount.subtract(budget.budgetAmount)
                notificationHelper.showBudgetExceededNotification(
                    categoryName,
                    formatCurrency(overAmount),
                    budget.id
                )
            }
            progress >= budget.alertThreshold -> {
                // Near budget limit
                val remaining = budget.budgetAmount.subtract(usedAmount)
                notificationHelper.showBudgetNearLimitNotification(
                    categoryName,
                    progress,
                    formatCurrency(remaining),
                    budget.id
                )
            }
        }
    }

    private suspend fun checkTotalBudget(budgets: List<Budget>) {
        val totalBudget = budgets.sumOf { it.budgetAmount }
        if (totalBudget <= BigDecimal.ZERO) return

        // Use monthly period for total budget calculation
        val dateRange = getCurrentMonthRange()
        val totalUsed = transactionRepository.getTotalExpenseInPeriod(
            dateRange.first,
            dateRange.second
        )

        val progress = (totalUsed.toDouble() / totalBudget.toDouble() * 100).toInt()

        when {
            progress > 100 -> {
                val overAmount = totalUsed.subtract(totalBudget)
                notificationHelper.showTotalBudgetExceededNotification(
                    formatCurrency(overAmount)
                )
            }
            progress >= 80 -> {
                val remaining = totalBudget.subtract(totalUsed)
                notificationHelper.showTotalBudgetNearLimitNotification(
                    progress,
                    formatCurrency(remaining)
                )
            }
        }
    }

    private fun getCurrentPeriodRange(budget: Budget): Pair<Date, Date> {
        val calendar = Calendar.getInstance()

        return when (budget.period) {
            "WEEKLY" -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time

                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endDate = calendar.time

                Pair(startDate, endDate)
            }
            "MONTHLY" -> {
                getCurrentMonthRange()
            }
            "YEARLY" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time

                calendar.add(Calendar.YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endDate = calendar.time

                Pair(startDate, endDate)
            }
            else -> getCurrentMonthRange()
        }
    }

    private fun getCurrentMonthRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.time

        return Pair(startDate, endDate)
    }

    private fun formatCurrency(amount: BigDecimal): String {
        return "¥${String.format("%,.0f", amount.toFloat())}"
    }
}