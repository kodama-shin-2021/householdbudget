package com.example.householdbudget.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdbudget.data.entity.Budget
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.data.entity.Transaction
import com.example.householdbudget.domain.repository.BudgetRepository
import com.example.householdbudget.domain.repository.CategoryRepository
import com.example.householdbudget.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<DashboardUiState>(DashboardUiState.Loading)
    val uiState: LiveData<DashboardUiState> = _uiState

    private val _currentPeriod = MutableLiveData<PeriodType>(PeriodType.MONTHLY)
    val currentPeriod: LiveData<PeriodType> = _currentPeriod

    private val _selectedDate = MutableLiveData<Date>(Date())
    val selectedDate: LiveData<Date> = _selectedDate

    private val _totalIncome = MutableLiveData<BigDecimal>(BigDecimal.ZERO)
    val totalIncome: LiveData<BigDecimal> = _totalIncome

    private val _totalExpense = MutableLiveData<BigDecimal>(BigDecimal.ZERO)
    val totalExpense: LiveData<BigDecimal> = _totalExpense

    private val _balance = MutableLiveData<BigDecimal>(BigDecimal.ZERO)
    val balance: LiveData<BigDecimal> = _balance

    private val _budgetProgressList = MutableLiveData<List<BudgetProgressItem>>()
    val budgetProgressList: LiveData<List<BudgetProgressItem>> = _budgetProgressList

    private val _recentTransactions = MutableLiveData<List<Transaction>>()
    val recentTransactions: LiveData<List<Transaction>> = _recentTransactions

    private val _categoryExpenses = MutableLiveData<List<CategoryExpenseItem>>()
    val categoryExpenses: LiveData<List<CategoryExpenseItem>> = _categoryExpenses

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _periodDisplayText = MutableLiveData<String>()
    val periodDisplayText: LiveData<String> = _periodDisplayText

    init {
        loadCategories()
        refreshData()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getAllCategories().observeForever { categories ->
                    _categories.value = categories
                    refreshData()
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error("カテゴリの読み込みに失敗しました")
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            try {
                _uiState.value = DashboardUiState.Loading
                
                val dateRange = getCurrentPeriodRange()
                updatePeriodDisplayText()
                
                // Load summary data
                loadSummaryData(dateRange)
                
                // Load budget progress
                loadBudgetProgress(dateRange)
                
                // Load recent transactions
                loadRecentTransactions()
                
                // Load category expenses for chart
                loadCategoryExpenses(dateRange)
                
                _uiState.value = DashboardUiState.Success
                
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "データの読み込みに失敗しました")
            }
        }
    }

    private suspend fun loadSummaryData(dateRange: Pair<Date, Date>) {
        val income = transactionRepository.getTotalIncomeInPeriod(dateRange.first, dateRange.second)
        val expense = transactionRepository.getTotalExpenseInPeriod(dateRange.first, dateRange.second)
        
        _totalIncome.value = income
        _totalExpense.value = expense
        _balance.value = income.subtract(expense)
    }

    private suspend fun loadBudgetProgress(dateRange: Pair<Date, Date>) {
        val activeBudgets = budgetRepository.getActiveBudgets().value ?: emptyList()
        val categories = _categories.value ?: emptyList()
        
        val budgetProgressItems = mutableListOf<BudgetProgressItem>()
        
        for (budget in activeBudgets.take(5)) { // Show top 5 budgets
            val category = categories.find { it.id == budget.categoryId }
            if (category != null) {
                val usage = budgetRepository.getBudgetUsage(budget.id)
                val progress = budgetRepository.getBudgetProgress(budget.id)
                
                budgetProgressItems.add(
                    BudgetProgressItem(
                        categoryId = category.id,
                        categoryName = category.name,
                        categoryIcon = category.iconResId,
                        categoryColor = category.color,
                        budgetAmount = budget.budgetAmount,
                        usedAmount = usage,
                        progressPercentage = (progress * 100).toInt(),
                        isOverBudget = progress > 1.0f
                    )
                )
            }
        }
        
        _budgetProgressList.value = budgetProgressItems
    }

    private suspend fun loadRecentTransactions() {
        transactionRepository.getRecentTransactions(10).observeForever { transactions ->
            _recentTransactions.value = transactions.take(5) // Show only 5 recent transactions
        }
    }

    private suspend fun loadCategoryExpenses(dateRange: Pair<Date, Date>) {
        val categories = _categories.value ?: emptyList()
        val categoryExpenseItems = mutableListOf<CategoryExpenseItem>()
        
        for (category in categories) {
            val expense = transactionRepository.getCategoryExpenseInPeriod(
                category.id,
                dateRange.first,
                dateRange.second
            )
            
            if (expense > BigDecimal.ZERO) {
                categoryExpenseItems.add(
                    CategoryExpenseItem(
                        categoryId = category.id,
                        categoryName = category.name,
                        amount = expense,
                        color = category.color
                    )
                )
            }
        }
        
        _categoryExpenses.value = categoryExpenseItems.sortedByDescending { it.amount }
    }

    fun setPeriodType(periodType: PeriodType) {
        _currentPeriod.value = periodType
        refreshData()
    }

    fun navigateToPreviousPeriod() {
        val currentDate = _selectedDate.value ?: Date()
        val newDate = when (_currentPeriod.value) {
            PeriodType.DAILY -> {
                val calendar = Calendar.getInstance()
                calendar.time = currentDate
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.time
            }
            PeriodType.WEEKLY -> {
                val calendar = Calendar.getInstance()
                calendar.time = currentDate
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.time
            }
            PeriodType.MONTHLY -> {
                val calendar = Calendar.getInstance()
                calendar.time = currentDate
                calendar.add(Calendar.MONTH, -1)
                calendar.time
            }
            PeriodType.YEARLY -> {
                val calendar = Calendar.getInstance()
                calendar.time = currentDate
                calendar.add(Calendar.YEAR, -1)
                calendar.time
            }
            null -> currentDate
        }
        
        _selectedDate.value = newDate
        refreshData()
    }

    fun navigateToNextPeriod() {
        val currentDate = _selectedDate.value ?: Date()
        val newDate = when (_currentPeriod.value) {
            PeriodType.DAILY -> {
                val calendar = Calendar.getInstance()
                calendar.time = currentDate
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                calendar.time
            }
            PeriodType.WEEKLY -> {
                val calendar = Calendar.getInstance()
                calendar.time = currentDate
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.time
            }
            PeriodType.MONTHLY -> {
                val calendar = Calendar.getInstance()
                calendar.time = currentDate
                calendar.add(Calendar.MONTH, 1)
                calendar.time
            }
            PeriodType.YEARLY -> {
                val calendar = Calendar.getInstance()
                calendar.time = currentDate
                calendar.add(Calendar.YEAR, 1)
                calendar.time
            }
            null -> currentDate
        }
        
        _selectedDate.value = newDate
        refreshData()
    }

    private fun getCurrentPeriodRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        calendar.time = _selectedDate.value ?: Date()

        return when (_currentPeriod.value) {
            PeriodType.DAILY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time

                calendar.add(Calendar.DAY_OF_MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endDate = calendar.time

                Pair(startDate, endDate)
            }
            PeriodType.WEEKLY -> {
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
            PeriodType.MONTHLY -> {
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
            PeriodType.YEARLY -> {
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
            null -> {
                val now = Date()
                Pair(now, now)
            }
        }
    }

    private fun updatePeriodDisplayText() {
        val selectedDate = _selectedDate.value ?: Date()
        val periodType = _currentPeriod.value ?: PeriodType.MONTHLY

        val displayText = when (periodType) {
            PeriodType.DAILY -> {
                SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(selectedDate)
            }
            PeriodType.WEEKLY -> {
                val calendar = Calendar.getInstance()
                calendar.time = selectedDate
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val weekStart = calendar.time
                calendar.add(Calendar.DAY_OF_MONTH, 6)
                val weekEnd = calendar.time
                
                val startFormat = SimpleDateFormat("M/d", Locale.getDefault())
                val endFormat = SimpleDateFormat("M/d", Locale.getDefault())
                "${startFormat.format(weekStart)} - ${endFormat.format(weekEnd)}"
            }
            PeriodType.MONTHLY -> {
                SimpleDateFormat("yyyy年M月", Locale.getDefault()).format(selectedDate)
            }
            PeriodType.YEARLY -> {
                SimpleDateFormat("yyyy年", Locale.getDefault()).format(selectedDate)
            }
        }

        _periodDisplayText.value = displayText
    }

    fun getCategoryById(categoryId: Long): Category? {
        return _categories.value?.find { it.id == categoryId }
    }

    fun formatCurrency(amount: BigDecimal): String {
        return "¥${String.format("%,.0f", amount.toFloat())}"
    }

    fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("M/d", Locale.getDefault())
        return sdf.format(date)
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    object Success : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

enum class PeriodType {
    DAILY,
    WEEKLY, 
    MONTHLY,
    YEARLY
}

data class BudgetProgressItem(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: Int,
    val categoryColor: String,
    val budgetAmount: BigDecimal,
    val usedAmount: BigDecimal,
    val progressPercentage: Int,
    val isOverBudget: Boolean
)

data class CategoryExpenseItem(
    val categoryId: Long,
    val categoryName: String,
    val amount: BigDecimal,
    val color: String
)