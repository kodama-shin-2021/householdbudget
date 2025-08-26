package com.example.householdbudget.ui.transaction

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.data.entity.CategoryType
import com.example.householdbudget.data.entity.RegularTransaction
import com.example.householdbudget.data.entity.RecurrenceFrequency
import com.example.householdbudget.data.entity.Transaction
import com.example.householdbudget.data.entity.TransactionType
import com.example.householdbudget.domain.repository.CategoryRepository
import com.example.householdbudget.domain.repository.RegularTransactionRepository
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
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val regularTransactionRepository: RegularTransactionRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<TransactionUiState>(TransactionUiState.Loading)
    val uiState: LiveData<TransactionUiState> = _uiState

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _filteredTransactions = MutableLiveData<List<Transaction>>()
    val filteredTransactions: LiveData<List<Transaction>> = _filteredTransactions

    private val _sortType = MutableLiveData<SortType>(SortType.DATE_DESC)
    val sortType: LiveData<SortType> = _sortType

    private val _filterState = MutableLiveData<FilterState>(FilterState())
    val filterState: LiveData<FilterState> = _filterState

    private val _totalIncome = MutableLiveData<BigDecimal>(BigDecimal.ZERO)
    val totalIncome: LiveData<BigDecimal> = _totalIncome

    private val _totalExpense = MutableLiveData<BigDecimal>(BigDecimal.ZERO)
    val totalExpense: LiveData<BigDecimal> = _totalExpense

    private val _balance = MutableLiveData<BigDecimal>(BigDecimal.ZERO)
    val balance: LiveData<BigDecimal> = _balance

    init {
        loadTransactions()
        loadCategories()
        calculateSummary()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            try {
                _uiState.value = TransactionUiState.Loading
                transactionRepository.getAllTransactions().collect { transactions ->
                    _transactions.value = transactions
                    applyFiltersAndSort()
                    _uiState.value = if (transactions.isNotEmpty()) {
                        TransactionUiState.Success
                    } else {
                        TransactionUiState.Empty
                    }
                }
            } catch (e: Exception) {
                _uiState.value = TransactionUiState.Error(e.message ?: "取引の読み込みに失敗しました")
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getAllCategories().collect { categories ->
                    _categories.value = categories
                }
            } catch (e: Exception) {
                // Handle error silently for categories
            }
        }
    }

    private fun calculateSummary() {
        viewModelScope.launch {
            try {
                val currentMonth = getCurrentMonthRange()
                val income = transactionRepository.getTotalIncomeInPeriod(
                    currentMonth.first, 
                    currentMonth.second
                )
                val expense = transactionRepository.getTotalExpenseInPeriod(
                    currentMonth.first, 
                    currentMonth.second
                )
                
                _totalIncome.value = income
                _totalExpense.value = expense
                _balance.value = income.subtract(expense)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    fun saveTransaction(
        amount: BigDecimal,
        type: TransactionType,
        categoryId: Long,
        subcategoryId: Long? = null,
        description: String? = null,
        date: Date = Date(),
        isRegular: Boolean = false,
        frequency: RecurrenceFrequency? = null
    ) {
        if (!validateTransaction(amount, type, categoryId, description)) {
            _uiState.value = TransactionUiState.Error("入力データが無効です")
            return
        }
        viewModelScope.launch {
            try {
                val transaction = Transaction(
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    subcategoryId = subcategoryId,
                    description = description?.takeIf { it.isNotBlank() },
                    date = date
                )

                val transactionId = transactionRepository.insertTransaction(transaction)

                // If this is a regular transaction, also save to regular transactions
                if (isRegular && frequency != null) {
                    val category = categoryRepository.getCategoryById(categoryId)
                    val regularTransaction = RegularTransaction(
                        name = category?.name ?: "定期取引",
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        subcategoryId = subcategoryId,
                        description = description?.takeIf { it.isNotBlank() },
                        frequency = frequency,
                        startDate = date,
                        nextOccurrence = calculateNextOccurrence(date, frequency)
                    )
                    regularTransactionRepository.insertRegularTransaction(regularTransaction)
                }

                _uiState.value = TransactionUiState.Success
                calculateSummary()
            } catch (e: Exception) {
                _uiState.value = TransactionUiState.Error("取引の保存に失敗しました: ${e.message}")
            }
        }
    }

    fun updateTransaction(
        transactionId: Long,
        amount: BigDecimal,
        type: TransactionType,
        categoryId: Long,
        subcategoryId: Long? = null,
        description: String? = null,
        date: Date = Date()
    ) {
        viewModelScope.launch {
            try {
                val existingTransaction = transactionRepository.getTransactionById(transactionId)
                if (existingTransaction != null) {
                    val updatedTransaction = existingTransaction.copy(
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        subcategoryId = subcategoryId,
                        description = description?.takeIf { it.isNotBlank() },
                        date = date,
                        updatedAt = Date()
                    )
                    transactionRepository.updateTransaction(updatedTransaction)
                    _uiState.value = TransactionUiState.Success
                    calculateSummary()
                }
            } catch (e: Exception) {
                _uiState.value = TransactionUiState.Error("取引の更新に失敗しました: ${e.message}")
            }
        }
    }

    fun deleteTransaction(transactionId: Long) {
        if (transactionId <= 0) {
            _uiState.value = TransactionUiState.Error("無効な取引IDです")
            return
        }
        
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransactionById(transactionId)
                _uiState.value = TransactionUiState.Success
                calculateSummary()
            } catch (e: Exception) {
                _uiState.value = TransactionUiState.Error("取引の削除に失敗しました: ${e.message}")
            }
        }
    }

    fun setSortType(sortType: SortType) {
        _sortType.value = sortType
        applyFiltersAndSort()
    }

    fun searchTransactions(query: String) {
        val currentFilter = _filterState.value ?: FilterState()
        _filterState.value = currentFilter.copy(searchQuery = query)
        applyFiltersAndSort()
    }

    fun setTransactionTypeFilter(type: TransactionType?) {
        val currentFilter = _filterState.value ?: FilterState()
        _filterState.value = currentFilter.copy(transactionType = type)
        applyFiltersAndSort()
    }

    fun setCategoryFilter(categoryId: Long?) {
        val currentFilter = _filterState.value ?: FilterState()
        _filterState.value = currentFilter.copy(categoryId = categoryId)
        applyFiltersAndSort()
    }

    fun setDateRangeFilter(startDate: Date?, endDate: Date?) {
        val currentFilter = _filterState.value ?: FilterState()
        _filterState.value = currentFilter.copy(startDate = startDate, endDate = endDate)
        applyFiltersAndSort()
    }

    fun clearFilters() {
        _filterState.value = FilterState()
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        val transactions = _transactions.value ?: return
        val filter = _filterState.value ?: FilterState()
        val sort = _sortType.value ?: SortType.DATE_DESC

        var filtered = transactions

        // Apply search filter
        if (filter.searchQuery.isNotBlank()) {
            filtered = filtered.filter { transaction ->
                val category = _categories.value?.find { it.id == transaction.categoryId }
                val description = transaction.description ?: ""
                val categoryName = category?.name ?: ""
                
                description.contains(filter.searchQuery, ignoreCase = true) ||
                categoryName.contains(filter.searchQuery, ignoreCase = true) ||
                transaction.amount.toString().contains(filter.searchQuery)
            }
        }

        // Apply transaction type filter
        filter.transactionType?.let { type ->
            filtered = filtered.filter { it.type == type }
        }

        // Apply category filter
        filter.categoryId?.let { categoryId ->
            filtered = filtered.filter { it.categoryId == categoryId }
        }

        // Apply date range filter
        if (filter.startDate != null && filter.endDate != null) {
            filtered = filtered.filter { transaction ->
                transaction.date.after(filter.startDate) || transaction.date == filter.startDate &&
                transaction.date.before(filter.endDate) || transaction.date == filter.endDate
            }
        }

        // Apply sorting
        filtered = when (sort) {
            SortType.DATE_DESC -> filtered.sortedByDescending { it.date }
            SortType.DATE_ASC -> filtered.sortedBy { it.date }
            SortType.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
            SortType.AMOUNT_ASC -> filtered.sortedBy { it.amount }
        }

        _filteredTransactions.value = filtered
    }

    fun getCategoriesByType(type: CategoryType): LiveData<List<Category>> {
        return categoryRepository.getCategoriesByType(type)
    }

    private fun calculateNextOccurrence(startDate: Date, frequency: RecurrenceFrequency): Date {
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        
        when (frequency) {
            RecurrenceFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceFrequency.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurrenceFrequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
            RecurrenceFrequency.DAILY -> calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return calendar.time
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
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time

        return Pair(startDate, endDate)
    }

    fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("M月d日", Locale.getDefault())
        return sdf.format(date)
    }

    fun formatDateHeader(date: Date): String {
        val sdf = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())
        return sdf.format(date)
    }

    fun validateTransaction(
        amount: BigDecimal,
        type: TransactionType,
        categoryId: Long,
        description: String?
    ): Boolean {
        return amount > BigDecimal.ZERO && 
               categoryId > 0 && 
               description?.trim()?.isNotEmpty() == true
    }

    fun addTransaction(
        amount: BigDecimal,
        type: String,
        categoryId: Long,
        description: String,
        date: Date = Date()
    ) {
        val transactionType = when (type) {
            "income" -> TransactionType.INCOME
            "expense" -> TransactionType.EXPENSE
            else -> TransactionType.EXPENSE
        }
        
        saveTransaction(
            amount = amount,
            type = transactionType,
            categoryId = categoryId,
            description = description,
            date = date
        )
    }

    fun filterByCategory(categoryId: Long) {
        setCategoryFilter(categoryId)
    }
}

sealed class TransactionUiState {
    object Loading : TransactionUiState()
    object Success : TransactionUiState()
    object Empty : TransactionUiState()
    data class Error(val message: String) : TransactionUiState()
}

enum class SortType {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC
}

data class FilterState(
    val searchQuery: String = "",
    val transactionType: TransactionType? = null,
    val categoryId: Long? = null,
    val startDate: Date? = null,
    val endDate: Date? = null
)