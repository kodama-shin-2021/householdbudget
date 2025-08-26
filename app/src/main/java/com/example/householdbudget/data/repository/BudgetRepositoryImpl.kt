package com.example.householdbudget.data.repository

import androidx.lifecycle.LiveData
import com.example.householdbudget.data.dao.BudgetDao
import com.example.householdbudget.data.dao.TransactionDao
import com.example.householdbudget.data.entity.Budget
import com.example.householdbudget.data.entity.BudgetPeriod
import com.example.householdbudget.domain.repository.BudgetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao
) : BudgetRepository {

    override fun getActiveBudgets(): LiveData<List<Budget>> {
        return budgetDao.getActiveBudgets()
    }

    override fun getAllBudgets(): LiveData<List<Budget>> {
        return budgetDao.getAllBudgets()
    }

    override suspend fun getBudgetById(id: Long): Budget? {
        return withContext(Dispatchers.IO) {
            budgetDao.getBudgetById(id)
        }
    }

    override fun getBudgetsByCategory(categoryId: Long): LiveData<List<Budget>> {
        return budgetDao.getBudgetsByCategory(categoryId)
    }

    override fun getBudgetsByPeriod(period: BudgetPeriod): LiveData<List<Budget>> {
        return budgetDao.getBudgetsByPeriod(period)
    }

    override fun getCurrentBudgets(currentDate: Date): LiveData<List<Budget>> {
        return budgetDao.getCurrentBudgets(currentDate)
    }

    override suspend fun getCurrentBudgetForCategory(categoryId: Long, currentDate: Date): Budget? {
        return withContext(Dispatchers.IO) {
            budgetDao.getCurrentBudgetForCategory(categoryId, currentDate)
        }
    }

    override suspend fun getCurrentBudgetForSubcategory(subcategoryId: Long, currentDate: Date): Budget? {
        return withContext(Dispatchers.IO) {
            budgetDao.getCurrentBudgetForSubcategory(subcategoryId, currentDate)
        }
    }

    override suspend fun insertBudget(budget: Budget): Long {
        return withContext(Dispatchers.IO) {
            budgetDao.insertBudget(budget)
        }
    }

    override suspend fun insertBudgets(budgets: List<Budget>) {
        withContext(Dispatchers.IO) {
            budgetDao.insertBudgets(budgets)
        }
    }

    override suspend fun updateBudget(budget: Budget) {
        withContext(Dispatchers.IO) {
            budgetDao.updateBudget(budget.copy(updatedAt = Date()))
        }
    }

    override suspend fun deleteBudget(budget: Budget) {
        withContext(Dispatchers.IO) {
            budgetDao.deleteBudget(budget)
        }
    }

    override suspend fun deleteBudgetById(id: Long) {
        withContext(Dispatchers.IO) {
            budgetDao.deleteBudgetById(id)
        }
    }

    override suspend fun deactivateBudget(id: Long) {
        withContext(Dispatchers.IO) {
            budgetDao.deactivateBudget(id)
        }
    }

    override suspend fun activateBudget(id: Long) {
        withContext(Dispatchers.IO) {
            budgetDao.activateBudget(id)
        }
    }

    override suspend fun getBudgetUsage(budgetId: Long): BigDecimal {
        return withContext(Dispatchers.IO) {
            val budget = budgetDao.getBudgetById(budgetId)
            if (budget != null) {
                val endDate = budget.endDate ?: Date()
                if (budget.subcategoryId != null) {
                    // サブカテゴリの場合は実装が複雑になるため、一旦カテゴリレベルで処理
                    transactionDao.getCategoryExpenseInPeriod(
                        budget.categoryId,
                        budget.startDate,
                        endDate
                    ) ?: BigDecimal.ZERO
                } else {
                    transactionDao.getCategoryExpenseInPeriod(
                        budget.categoryId,
                        budget.startDate,
                        endDate
                    ) ?: BigDecimal.ZERO
                }
            } else {
                BigDecimal.ZERO
            }
        }
    }

    override suspend fun getBudgetProgress(budgetId: Long): Float {
        return withContext(Dispatchers.IO) {
            val budget = budgetDao.getBudgetById(budgetId)
            if (budget != null) {
                val usage = getBudgetUsage(budgetId)
                if (budget.budgetAmount > BigDecimal.ZERO) {
                    (usage.toFloat() / budget.budgetAmount.toFloat()).coerceAtMost(1.0f)
                } else {
                    0.0f
                }
            } else {
                0.0f
            }
        }
    }

    override suspend fun checkBudgetAlerts(): List<Budget> {
        return withContext(Dispatchers.IO) {
            val currentDate = Date()
            val activeBudgets = budgetDao.getCurrentBudgets(currentDate).value ?: emptyList()
            
            activeBudgets.filter { budget ->
                val progress = getBudgetProgress(budget.id)
                progress >= budget.alertThreshold
            }
        }
    }
}