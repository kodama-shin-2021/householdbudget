package com.example.householdbudget.ui.budget

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdbudget.data.entity.Budget
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.domain.repository.BudgetRepository
import com.example.householdbudget.domain.repository.CategoryRepository
import com.example.householdbudget.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<BudgetUiState>(BudgetUiState.Loading)
    val uiState: LiveData<BudgetUiState> = _uiState

    private val _currentPeriod = MutableLiveData<BudgetPeriod>(BudgetPeriod.MONTHLY)
    val currentPeriod: LiveData<BudgetPeriod> = _currentPeriod

    private val _totalBudget = MutableLiveData<BigDecimal>(BigDecimal.ZERO)
    val totalBudget: LiveData<BigDecimal> = _totalBudget

    private val _totalUsed = MutableLiveData<BigDecimal>(BigDecimal.ZERO)
    val totalUsed: LiveData<BigDecimal> = _totalUsed

    private val _totalRemaining = MutableLiveData<BigDecimal>(BigDecimal.ZERO)
    val totalRemaining: LiveData<BigDecimal> = _totalRemaining

    private val _categoryBudgets = MutableLiveData<List<CategoryBudgetItem>>(emptyList())
    val categoryBudgets: LiveData<List<CategoryBudgetItem>> = _categoryBudgets

    private val _categories = MutableLiveData<List<Category>>(emptyList())
    val categories: LiveData<List<Category>> = _categories

    private val _budgetAlert = MutableLiveData<BudgetAlert?>()
    val budgetAlert: LiveData<BudgetAlert?> = _budgetAlert

    init {
        loadCategories()
        refreshData()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getAllCategories().observeForever { categories ->
                    _categories.value = categories
                }
            } catch (e: Exception) {
                _uiState.value = BudgetUiState.Error("カテゴリの読み込みに失敗しました")
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            try {
                _uiState.value = BudgetUiState.Loading
                
                val period = _currentPeriod.value ?: BudgetPeriod.MONTHLY
                val dateRange = getCurrentPeriodRange(period)
                
                loadTotalBudgetData(period, dateRange)
                loadCategoryBudgets(period, dateRange)
                checkBudgetAlerts()
                
                _uiState.value = BudgetUiState.Success
            } catch (e: Exception) {
                _uiState.value = BudgetUiState.Error(e.message ?: "データの読み込みに失敗しました")
            }
        }
    }

    private suspend fun loadTotalBudgetData(period: BudgetPeriod, dateRange: Pair<Date, Date>) {
        val budgets = budgetRepository.getBudgetsByPeriod(period.name).value ?: emptyList()
        val totalBudgetAmount = budgets.sumOf { it.budgetAmount }
        
        val totalExpense = transactionRepository.getTotalExpenseInPeriod(
            dateRange.first, 
            dateRange.second
        )
        
        _totalBudget.value = totalBudgetAmount
        _totalUsed.value = totalExpense
        _totalRemaining.value = totalBudgetAmount.subtract(totalExpense)
    }

    private suspend fun loadCategoryBudgets(period: BudgetPeriod, dateRange: Pair<Date, Date>) {
        val budgets = budgetRepository.getBudgetsByPeriod(period.name).value ?: emptyList()
        val categories = _categories.value ?: emptyList()
        val categoryBudgetItems = mutableListOf<CategoryBudgetItem>()

        for (budget in budgets) {
            val category = categories.find { it.id == budget.categoryId }
            if (category != null) {
                val usedAmount = transactionRepository.getCategoryExpenseInPeriod(
                    budget.categoryId,
                    dateRange.first,
                    dateRange.second
                )
                
                val remainingAmount = budget.budgetAmount.subtract(usedAmount)
                val progress = if (budget.budgetAmount > BigDecimal.ZERO) {
                    (usedAmount.toDouble() / budget.budgetAmount.toDouble() * 100).toInt()
                } else {
                    0
                }
                
                val isOverBudget = usedAmount > budget.budgetAmount
                val isNearLimit = progress >= budget.alertThreshold

                categoryBudgetItems.add(
                    CategoryBudgetItem(
                        budget = budget,
                        category = category,
                        usedAmount = usedAmount,
                        remainingAmount = remainingAmount,
                        progress = progress,
                        isOverBudget = isOverBudget,
                        isNearLimit = isNearLimit
                    )
                )
            }
        }

        _categoryBudgets.value = categoryBudgetItems.sortedByDescending { it.progress }
    }

    private suspend fun checkBudgetAlerts() {
        val categoryBudgets = _categoryBudgets.value ?: emptyList()
        val overBudgetCount = categoryBudgets.count { it.isOverBudget }
        val nearLimitCount = categoryBudgets.count { it.isNearLimit && !it.isOverBudget }
        
        val totalBudget = _totalBudget.value ?: BigDecimal.ZERO
        val totalUsed = _totalUsed.value ?: BigDecimal.ZERO
        val totalProgress = if (totalBudget > BigDecimal.ZERO) {
            (totalUsed.toDouble() / totalBudget.toDouble() * 100).toInt()
        } else {
            0
        }

        val alert = when {
            totalProgress > 100 -> {
                BudgetAlert(
                    type = BudgetAlertType.OVER_BUDGET,
                    message = "総予算を${totalProgress - 100}%超過しています。支出を見直してください。"
                )
            }
            totalProgress >= 80 -> {
                BudgetAlert(
                    type = BudgetAlertType.NEAR_LIMIT,
                    message = "総予算の${totalProgress}%を使用しています。残りの支出にご注意ください。"
                )
            }
            overBudgetCount > 0 -> {
                BudgetAlert(
                    type = BudgetAlertType.CATEGORY_OVER_BUDGET,
                    message = "${overBudgetCount}個のカテゴリで予算を超過しています。"
                )
            }
            nearLimitCount > 0 -> {
                BudgetAlert(
                    type = BudgetAlertType.CATEGORY_NEAR_LIMIT,
                    message = "${nearLimitCount}個のカテゴリで予算上限に近づいています。"
                )
            }
            else -> null
        }

        _budgetAlert.value = alert
    }

    fun setPeriod(period: BudgetPeriod) {
        _currentPeriod.value = period
        refreshData()
    }

    fun saveBudget(
        categoryId: Long?,
        budgetAmount: BigDecimal,
        period: BudgetPeriod,
        startDate: Date,
        alertThreshold: Int,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                val budget = Budget(
                    categoryId = categoryId ?: 0L,
                    budgetAmount = budgetAmount,
                    period = period.name,
                    startDate = startDate,
                    endDate = calculateEndDate(startDate, period),
                    alertThreshold = alertThreshold,
                    isActive = true,
                    notes = notes
                )

                budgetRepository.insertBudget(budget)
                refreshData()
            } catch (e: Exception) {
                _uiState.value = BudgetUiState.Error("予算の保存に失敗しました")
            }
        }
    }

    fun updateBudget(
        budgetId: Long,
        budgetAmount: BigDecimal,
        alertThreshold: Int,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                val existingBudget = budgetRepository.getBudgetById(budgetId)
                if (existingBudget != null) {
                    val updatedBudget = existingBudget.copy(
                        budgetAmount = budgetAmount,
                        alertThreshold = alertThreshold,
                        notes = notes
                    )
                    budgetRepository.updateBudget(updatedBudget)
                    refreshData()
                }
            } catch (e: Exception) {
                _uiState.value = BudgetUiState.Error("予算の更新に失敗しました")
            }
        }
    }

    fun deleteBudget(budgetId: Long) {
        viewModelScope.launch {
            try {
                budgetRepository.deleteBudget(budgetId)
                refreshData()
            } catch (e: Exception) {
                _uiState.value = BudgetUiState.Error("予算の削除に失敗しました")
            }
        }
    }

    fun toggleBudgetStatus(budgetId: Long) {
        viewModelScope.launch {
            try {
                val budget = budgetRepository.getBudgetById(budgetId)
                if (budget != null) {
                    val updatedBudget = budget.copy(isActive = !budget.isActive)
                    budgetRepository.updateBudget(updatedBudget)
                    refreshData()
                }
            } catch (e: Exception) {
                _uiState.value = BudgetUiState.Error("予算ステータスの変更に失敗しました")
            }
        }
    }

    private fun getCurrentPeriodRange(period: BudgetPeriod): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        calendar.time = Date()

        return when (period) {
            BudgetPeriod.WEEKLY -> {
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
            BudgetPeriod.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time

                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endDate = calendar.time

                Pair(startDate, endDate)
            }
            BudgetPeriod.YEARLY -> {
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
        }
    }

    private fun calculateEndDate(startDate: Date, period: BudgetPeriod): Date {
        val calendar = Calendar.getInstance()
        calendar.time = startDate

        return when (period) {
            BudgetPeriod.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                calendar.time
            }
            BudgetPeriod.MONTHLY -> {
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                calendar.time
            }
            BudgetPeriod.YEARLY -> {
                calendar.add(Calendar.YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                calendar.time
            }
        }
    }

    fun formatCurrency(amount: BigDecimal): String {
        return "¥${String.format("%,.0f", amount.toFloat())}"
    }
}

sealed class BudgetUiState {
    object Loading : BudgetUiState()
    object Success : BudgetUiState()
    data class Error(val message: String) : BudgetUiState()
}

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY
}

data class CategoryBudgetItem(
    val budget: Budget,
    val category: Category,
    val usedAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val progress: Int,
    val isOverBudget: Boolean,
    val isNearLimit: Boolean
)

data class BudgetAlert(
    val type: BudgetAlertType,
    val message: String
)

enum class BudgetAlertType {
    OVER_BUDGET,
    NEAR_LIMIT,
    CATEGORY_OVER_BUDGET,
    CATEGORY_NEAR_LIMIT
}