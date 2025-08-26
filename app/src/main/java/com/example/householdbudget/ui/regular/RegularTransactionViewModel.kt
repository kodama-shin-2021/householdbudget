package com.example.householdbudget.ui.regular

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.data.entity.CategoryType
import com.example.householdbudget.data.entity.RecurrenceFrequency
import com.example.householdbudget.data.entity.RegularTransaction
import com.example.householdbudget.data.entity.TransactionType
import com.example.householdbudget.domain.repository.CategoryRepository
import com.example.householdbudget.domain.repository.RegularTransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RegularTransactionViewModel @Inject constructor(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<RegularTransactionUiState>(RegularTransactionUiState.Loading)
    val uiState: LiveData<RegularTransactionUiState> = _uiState

    private val _regularTransactions = MutableLiveData<List<RegularTransaction>>()
    val regularTransactions: LiveData<List<RegularTransaction>> = _regularTransactions

    private val _filteredTransactions = MutableLiveData<List<RegularTransaction>>()
    val filteredTransactions: LiveData<List<RegularTransaction>> = _filteredTransactions

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _filterStatus = MutableLiveData<FilterStatus>(FilterStatus.ACTIVE)
    val filterStatus: LiveData<FilterStatus> = _filterStatus

    private val _monthlyIncomeTotal = MutableLiveData<BigDecimal>(BigDecimal.ZERO)
    val monthlyIncomeTotal: LiveData<BigDecimal> = _monthlyIncomeTotal

    private val _monthlyExpenseTotal = MutableLiveData<BigDecimal>(BigDecimal.ZERO)
    val monthlyExpenseTotal: LiveData<BigDecimal> = _monthlyExpenseTotal

    private val _upcomingExecutionsCount = MutableLiveData<Int>(0)
    val upcomingExecutionsCount: LiveData<Int> = _upcomingExecutionsCount

    private val dateFormat = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())

    init {
        loadRegularTransactions()
        loadCategories()
        calculateSummary()
    }

    private fun loadRegularTransactions() {
        viewModelScope.launch {
            try {
                _uiState.value = RegularTransactionUiState.Loading
                regularTransactionRepository.getAllRegularTransactions().observeForever { transactions ->
                    _regularTransactions.value = transactions
                    applyFilter()
                    calculateSummary()
                    if (transactions.isNotEmpty()) {
                        _uiState.value = RegularTransactionUiState.Success
                    } else {
                        _uiState.value = RegularTransactionUiState.Empty
                    }
                }
            } catch (e: Exception) {
                _uiState.value = RegularTransactionUiState.Error(e.message ?: "定期取引の読み込みに失敗しました")
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getAllCategories().observeForever { categories ->
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
                val transactions = _regularTransactions.value ?: emptyList()
                val activeTransactions = transactions.filter { it.isActive }

                var monthlyIncome = BigDecimal.ZERO
                var monthlyExpense = BigDecimal.ZERO

                activeTransactions.forEach { regularTransaction ->
                    val monthlyAmount = calculateMonthlyAmount(regularTransaction)
                    when (regularTransaction.type) {
                        TransactionType.INCOME -> monthlyIncome = monthlyIncome.add(monthlyAmount)
                        TransactionType.EXPENSE -> monthlyExpense = monthlyExpense.add(monthlyAmount)
                    }
                }

                _monthlyIncomeTotal.value = monthlyIncome
                _monthlyExpenseTotal.value = monthlyExpense

                // Calculate upcoming executions (next 7 days)
                val nextWeek = Calendar.getInstance()
                nextWeek.add(Calendar.DAY_OF_MONTH, 7)
                
                val upcomingCount = activeTransactions.count { transaction ->
                    transaction.nextOccurrence.before(nextWeek.time) || 
                    transaction.nextOccurrence == nextWeek.time
                }
                _upcomingExecutionsCount.value = upcomingCount
                
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    private fun calculateMonthlyAmount(regularTransaction: RegularTransaction): BigDecimal {
        return when (regularTransaction.frequency) {
            RecurrenceFrequency.DAILY -> {
                // Approximate 30 days per month
                regularTransaction.amount.multiply(BigDecimal(30)).divide(BigDecimal(regularTransaction.interval))
            }
            RecurrenceFrequency.WEEKLY -> {
                // Approximate 4.33 weeks per month
                regularTransaction.amount.multiply(BigDecimal(4.33)).divide(BigDecimal(regularTransaction.interval))
            }
            RecurrenceFrequency.MONTHLY -> {
                regularTransaction.amount.divide(BigDecimal(regularTransaction.interval))
            }
            RecurrenceFrequency.YEARLY -> {
                regularTransaction.amount.divide(BigDecimal(12 * regularTransaction.interval))
            }
        }
    }

    fun saveRegularTransaction(
        name: String,
        amount: BigDecimal,
        type: TransactionType,
        categoryId: Long,
        subcategoryId: Long? = null,
        description: String? = null,
        frequency: RecurrenceFrequency,
        interval: Int = 1,
        startDate: Date,
        endDate: Date? = null,
        dayOfWeek: Int? = null,
        dayOfMonth: Int? = null,
        autoExecute: Boolean = false,
        notifyBeforeDays: Int = 1
    ) {
        if (name.isBlank()) {
            _uiState.value = RegularTransactionUiState.Error("定期取引名を入力してください")
            return
        }

        if (amount <= BigDecimal.ZERO) {
            _uiState.value = RegularTransactionUiState.Error("金額は0より大きい値を入力してください")
            return
        }

        viewModelScope.launch {
            try {
                val nextOccurrence = calculateNextOccurrence(startDate, frequency, interval, dayOfWeek, dayOfMonth)
                
                val regularTransaction = RegularTransaction(
                    name = name.trim(),
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    subcategoryId = subcategoryId,
                    description = description?.takeIf { it.isNotBlank() },
                    frequency = frequency,
                    interval = interval,
                    dayOfWeek = dayOfWeek,
                    dayOfMonth = dayOfMonth,
                    startDate = startDate,
                    endDate = endDate,
                    nextOccurrence = nextOccurrence,
                    autoExecute = autoExecute,
                    notifyBeforeDays = notifyBeforeDays,
                    isActive = true
                )

                regularTransactionRepository.insertRegularTransaction(regularTransaction)
                _uiState.value = RegularTransactionUiState.Success
            } catch (e: Exception) {
                _uiState.value = RegularTransactionUiState.Error("定期取引の保存に失敗しました: ${e.message}")
            }
        }
    }

    fun updateRegularTransaction(
        regularTransactionId: Long,
        name: String,
        amount: BigDecimal,
        type: TransactionType,
        categoryId: Long,
        subcategoryId: Long? = null,
        description: String? = null,
        frequency: RecurrenceFrequency,
        interval: Int = 1,
        startDate: Date,
        endDate: Date? = null,
        dayOfWeek: Int? = null,
        dayOfMonth: Int? = null,
        autoExecute: Boolean = false,
        notifyBeforeDays: Int = 1
    ) {
        viewModelScope.launch {
            try {
                val existingTransaction = regularTransactionRepository.getRegularTransactionById(regularTransactionId)
                if (existingTransaction != null) {
                    val nextOccurrence = calculateNextOccurrence(startDate, frequency, interval, dayOfWeek, dayOfMonth)
                    
                    val updatedTransaction = existingTransaction.copy(
                        name = name.trim(),
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        subcategoryId = subcategoryId,
                        description = description?.takeIf { it.isNotBlank() },
                        frequency = frequency,
                        interval = interval,
                        dayOfWeek = dayOfWeek,
                        dayOfMonth = dayOfMonth,
                        startDate = startDate,
                        endDate = endDate,
                        nextOccurrence = nextOccurrence,
                        autoExecute = autoExecute,
                        notifyBeforeDays = notifyBeforeDays,
                        updatedAt = Date()
                    )
                    
                    regularTransactionRepository.updateRegularTransaction(updatedTransaction)
                    _uiState.value = RegularTransactionUiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = RegularTransactionUiState.Error("定期取引の更新に失敗しました: ${e.message}")
            }
        }
    }

    fun deleteRegularTransaction(regularTransactionId: Long) {
        viewModelScope.launch {
            try {
                regularTransactionRepository.deleteRegularTransactionById(regularTransactionId)
                _uiState.value = RegularTransactionUiState.Success
            } catch (e: Exception) {
                _uiState.value = RegularTransactionUiState.Error("定期取引の削除に失敗しました: ${e.message}")
            }
        }
    }

    fun toggleRegularTransactionStatus(regularTransactionId: Long) {
        viewModelScope.launch {
            try {
                val transaction = regularTransactionRepository.getRegularTransactionById(regularTransactionId)
                if (transaction != null) {
                    if (transaction.isActive) {
                        regularTransactionRepository.deactivateRegularTransaction(regularTransactionId)
                    } else {
                        regularTransactionRepository.activateRegularTransaction(regularTransactionId)
                    }
                    _uiState.value = RegularTransactionUiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = RegularTransactionUiState.Error("ステータスの変更に失敗しました: ${e.message}")
            }
        }
    }

    fun executeRegularTransactionNow(regularTransactionId: Long) {
        viewModelScope.launch {
            try {
                regularTransactionRepository.executeRegularTransaction(regularTransactionId)
                _uiState.value = RegularTransactionUiState.Success
            } catch (e: Exception) {
                _uiState.value = RegularTransactionUiState.Error("定期取引の実行に失敗しました: ${e.message}")
            }
        }
    }

    fun setFilterStatus(status: FilterStatus) {
        _filterStatus.value = status
        applyFilter()
    }

    private fun applyFilter() {
        val transactions = _regularTransactions.value ?: return
        val status = _filterStatus.value ?: FilterStatus.ACTIVE

        val filtered = when (status) {
            FilterStatus.ACTIVE -> transactions.filter { it.isActive }
            FilterStatus.INACTIVE -> transactions.filter { !it.isActive }
            FilterStatus.ALL -> transactions
        }

        _filteredTransactions.value = filtered
    }

    fun getCategoriesByType(type: CategoryType): LiveData<List<Category>> {
        return categoryRepository.getCategoriesByType(type)
    }

    private fun calculateNextOccurrence(
        startDate: Date,
        frequency: RecurrenceFrequency,
        interval: Int,
        dayOfWeek: Int? = null,
        dayOfMonth: Int? = null
    ): Date {
        val calendar = Calendar.getInstance()
        calendar.time = startDate

        when (frequency) {
            RecurrenceFrequency.DAILY -> {
                calendar.add(Calendar.DAY_OF_MONTH, interval)
            }
            RecurrenceFrequency.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, interval)
                dayOfWeek?.let { calendar.set(Calendar.DAY_OF_WEEK, it) }
            }
            RecurrenceFrequency.MONTHLY -> {
                calendar.add(Calendar.MONTH, interval)
                dayOfMonth?.let { 
                    // Handle end of month cases
                    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    calendar.set(Calendar.DAY_OF_MONTH, minOf(it, maxDay))
                }
            }
            RecurrenceFrequency.YEARLY -> {
                calendar.add(Calendar.YEAR, interval)
            }
        }

        return calendar.time
    }

    fun formatDate(date: Date): String {
        return dateFormat.format(date)
    }

    fun formatFrequency(regularTransaction: RegularTransaction): String {
        val frequencyText = when (regularTransaction.frequency) {
            RecurrenceFrequency.DAILY -> "毎日"
            RecurrenceFrequency.WEEKLY -> "毎週"
            RecurrenceFrequency.MONTHLY -> "毎月"
            RecurrenceFrequency.YEARLY -> "毎年"
        }

        val intervalText = if (regularTransaction.interval > 1) {
            "${regularTransaction.interval}${getIntervalUnit(regularTransaction.frequency)}ごと"
        } else {
            frequencyText
        }

        return when (regularTransaction.frequency) {
            RecurrenceFrequency.WEEKLY -> {
                val dayOfWeek = regularTransaction.dayOfWeek?.let { getDayOfWeekName(it) } ?: ""
                "$intervalText $dayOfWeek"
            }
            RecurrenceFrequency.MONTHLY -> {
                val dayOfMonth = regularTransaction.dayOfMonth?.let { "${it}日" } ?: ""
                "$intervalText $dayOfMonth"
            }
            else -> intervalText
        }
    }

    private fun getIntervalUnit(frequency: RecurrenceFrequency): String {
        return when (frequency) {
            RecurrenceFrequency.DAILY -> "日"
            RecurrenceFrequency.WEEKLY -> "週"
            RecurrenceFrequency.MONTHLY -> "ヶ月"
            RecurrenceFrequency.YEARLY -> "年"
        }
    }

    private fun getDayOfWeekName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "日曜日"
            Calendar.MONDAY -> "月曜日"
            Calendar.TUESDAY -> "火曜日"
            Calendar.WEDNESDAY -> "水曜日"
            Calendar.THURSDAY -> "木曜日"
            Calendar.FRIDAY -> "金曜日"
            Calendar.SATURDAY -> "土曜日"
            else -> ""
        }
    }

    fun calculateDaysUntilNext(nextOccurrence: Date): Int {
        val today = Calendar.getInstance()
        val next = Calendar.getInstance()
        next.time = nextOccurrence
        
        val diffInMillis = next.timeInMillis - today.timeInMillis
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
    }

    fun calculateProgressUntilNext(regularTransaction: RegularTransaction): Float {
        val today = Date()
        val lastExecution = regularTransaction.startDate // Simplified - should track actual last execution
        val nextExecution = regularTransaction.nextOccurrence
        
        val totalPeriod = nextExecution.time - lastExecution.time
        val elapsed = today.time - lastExecution.time
        
        return if (totalPeriod > 0) {
            (elapsed.toFloat() / totalPeriod.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
}

sealed class RegularTransactionUiState {
    object Loading : RegularTransactionUiState()
    object Success : RegularTransactionUiState()
    object Empty : RegularTransactionUiState()
    data class Error(val message: String) : RegularTransactionUiState()
}

enum class FilterStatus {
    ACTIVE,
    INACTIVE,
    ALL
}