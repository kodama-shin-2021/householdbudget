package com.example.householdbudget.ui.goal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.data.entity.Goal
import com.example.householdbudget.data.entity.GoalStatus
import com.example.householdbudget.data.entity.GoalType
import com.example.householdbudget.data.entity.TransactionType
import com.example.householdbudget.domain.repository.CategoryRepository
import com.example.householdbudget.domain.repository.GoalRepository
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
class GoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<GoalUiState>(GoalUiState.Loading)
    val uiState: LiveData<GoalUiState> = _uiState

    private val _goals = MutableLiveData<List<GoalItem>>(emptyList())
    val goals: LiveData<List<GoalItem>> = _goals

    private val _filteredGoals = MutableLiveData<List<GoalItem>>(emptyList())
    val filteredGoals: LiveData<List<GoalItem>> = _filteredGoals

    private val _achievements = MutableLiveData<List<Achievement>>(emptyList())
    val achievements: LiveData<List<Achievement>> = _achievements

    private val _categories = MutableLiveData<List<Category>>(emptyList())
    val categories: LiveData<List<Category>> = _categories

    private val _activeGoalsCount = MutableLiveData<Int>(0)
    val activeGoalsCount: LiveData<Int> = _activeGoalsCount

    private val _achievedGoalsCount = MutableLiveData<Int>(0)
    val achievedGoalsCount: LiveData<Int> = _achievedGoalsCount

    private val _currentFilter = MutableLiveData<GoalFilter>(GoalFilter.ALL)
    val currentFilter: LiveData<GoalFilter> = _currentFilter

    private val _sortOrder = MutableLiveData<GoalSortOrder>(GoalSortOrder.PROGRESS_DESC)
    val sortOrder: LiveData<GoalSortOrder> = _sortOrder

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
                _uiState.value = GoalUiState.Error("カテゴリの読み込みに失敗しました")
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            try {
                _uiState.value = GoalUiState.Loading
                
                loadGoals()
                loadAchievements()
                updateCounts()
                
                _uiState.value = GoalUiState.Success
            } catch (e: Exception) {
                _uiState.value = GoalUiState.Error(e.message ?: "データの読み込みに失敗しました")
            }
        }
    }

    private suspend fun loadGoals() {
        val goals = goalRepository.getAllGoals().value ?: emptyList()
        val categories = _categories.value ?: emptyList()
        val goalItems = mutableListOf<GoalItem>()

        for (goal in goals) {
            val category = categories.find { it.id == goal.categoryId }
            val progress = calculateGoalProgress(goal)
            val timeRemaining = calculateTimeRemaining(goal)
            
            goalItems.add(
                GoalItem(
                    goal = goal,
                    category = category,
                    currentAmount = progress.currentAmount,
                    progress = progress.progressPercentage,
                    isAchieved = progress.isAchieved,
                    daysRemaining = timeRemaining.days,
                    isOverdue = timeRemaining.isOverdue,
                    milestonesPassed = progress.milestonesPassed
                )
            )
        }

        _goals.value = goalItems
        applyFilterAndSort()
    }

    private suspend fun loadAchievements() {
        val goals = goalRepository.getGoalsByStatus(GoalStatus.ACHIEVED).value ?: emptyList()
        val categories = _categories.value ?: emptyList()
        val achievementList = mutableListOf<Achievement>()

        for (goal in goals.take(5)) { // Show recent 5 achievements
            val category = categories.find { it.id == goal.categoryId }
            achievementList.add(
                Achievement(
                    goalName = goal.name,
                    targetAmount = goal.targetAmount,
                    achievedDate = goal.achievedDate ?: goal.targetDate,
                    category = category
                )
            )
        }

        _achievements.value = achievementList.sortedByDescending { it.achievedDate }
    }

    private fun updateCounts() {
        val goals = _goals.value ?: emptyList()
        _activeGoalsCount.value = goals.count { !it.isAchieved && it.goal.status == GoalStatus.ACTIVE }
        _achievedGoalsCount.value = goals.count { it.isAchieved }
    }

    private suspend fun calculateGoalProgress(goal: Goal): GoalProgress {
        val currentAmount = when (goal.type) {
            GoalType.SAVING -> {
                // For saving goals, calculate based on income transactions in related category
                if (goal.categoryId != null) {
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
                // For expense reduction goals, calculate reduction from baseline
                if (goal.categoryId != null) {
                    val currentExpense = transactionRepository.getCategoryExpenseInPeriod(
                        goal.categoryId,
                        goal.createdDate,
                        Date()
                    )
                    // If target is to reduce to X, current progress is (baseline - current expense)
                    goal.currentAmount.subtract(currentExpense).coerceAtLeast(BigDecimal.ZERO)
                } else {
                    goal.currentAmount
                }
            }
        }

        val progressPercentage = if (goal.targetAmount > BigDecimal.ZERO) {
            (currentAmount.toDouble() / goal.targetAmount.toDouble() * 100).toInt().coerceAtMost(100)
        } else {
            0
        }

        val isAchieved = currentAmount >= goal.targetAmount
        
        // Calculate passed milestones
        val milestones = listOf(25, 50, 75, 90)
        val milestonesPassed = milestones.filter { progressPercentage >= it }

        return GoalProgress(
            currentAmount = currentAmount,
            progressPercentage = progressPercentage,
            isAchieved = isAchieved,
            milestonesPassed = milestonesPassed
        )
    }

    private fun calculateTimeRemaining(goal: Goal): TimeRemaining {
        val today = Calendar.getInstance().time
        val targetDate = goal.targetDate
        
        val diffInMillis = targetDate.time - today.time
        val days = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        
        return TimeRemaining(
            days = days.coerceAtLeast(0),
            isOverdue = days < 0
        )
    }

    fun setFilter(filter: GoalFilter) {
        _currentFilter.value = filter
        applyFilterAndSort()
    }

    fun setSortOrder(sortOrder: GoalSortOrder) {
        _sortOrder.value = sortOrder
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        val goals = _goals.value ?: emptyList()
        val filter = _currentFilter.value ?: GoalFilter.ALL
        val sortOrder = _sortOrder.value ?: GoalSortOrder.PROGRESS_DESC

        // Apply filter
        val filteredGoals = when (filter) {
            GoalFilter.ALL -> goals
            GoalFilter.SAVING -> goals.filter { it.goal.type == GoalType.SAVING }
            GoalFilter.EXPENSE_REDUCTION -> goals.filter { it.goal.type == GoalType.EXPENSE_REDUCTION }
            GoalFilter.ACTIVE -> goals.filter { !it.isAchieved && it.goal.status == GoalStatus.ACTIVE }
            GoalFilter.ACHIEVED -> goals.filter { it.isAchieved }
        }

        // Apply sort
        val sortedGoals = when (sortOrder) {
            GoalSortOrder.PROGRESS_DESC -> filteredGoals.sortedByDescending { it.progress }
            GoalSortOrder.PROGRESS_ASC -> filteredGoals.sortedBy { it.progress }
            GoalSortOrder.TARGET_DATE_ASC -> filteredGoals.sortedBy { it.goal.targetDate }
            GoalSortOrder.TARGET_DATE_DESC -> filteredGoals.sortedByDescending { it.goal.targetDate }
            GoalSortOrder.TARGET_AMOUNT_ASC -> filteredGoals.sortedBy { it.goal.targetAmount }
            GoalSortOrder.TARGET_AMOUNT_DESC -> filteredGoals.sortedByDescending { it.goal.targetAmount }
            GoalSortOrder.NAME_ASC -> filteredGoals.sortedBy { it.goal.name }
        }

        _filteredGoals.value = sortedGoals
    }

    fun saveGoal(
        name: String,
        type: GoalType,
        targetAmount: BigDecimal,
        currentAmount: BigDecimal,
        targetDate: Date,
        categoryId: Long?,
        description: String?,
        milestoneNotifications: List<Int>
    ) {
        viewModelScope.launch {
            try {
                val goal = Goal(
                    name = name,
                    type = type,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    targetDate = targetDate,
                    categoryId = categoryId,
                    description = description,
                    status = GoalStatus.ACTIVE,
                    milestoneNotifications = milestoneNotifications.joinToString(","),
                    createdDate = Date()
                )

                goalRepository.insertGoal(goal)
                refreshData()
            } catch (e: Exception) {
                _uiState.value = GoalUiState.Error("目標の保存に失敗しました")
            }
        }
    }

    fun updateGoal(
        goalId: Long,
        name: String,
        targetAmount: BigDecimal,
        currentAmount: BigDecimal,
        targetDate: Date,
        categoryId: Long?,
        description: String?,
        milestoneNotifications: List<Int>
    ) {
        viewModelScope.launch {
            try {
                val existingGoal = goalRepository.getGoalById(goalId)
                if (existingGoal != null) {
                    val updatedGoal = existingGoal.copy(
                        name = name,
                        targetAmount = targetAmount,
                        currentAmount = currentAmount,
                        targetDate = targetDate,
                        categoryId = categoryId,
                        description = description,
                        milestoneNotifications = milestoneNotifications.joinToString(",")
                    )
                    goalRepository.updateGoal(updatedGoal)
                    refreshData()
                }
            } catch (e: Exception) {
                _uiState.value = GoalUiState.Error("目標の更新に失敗しました")
            }
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            try {
                goalRepository.deleteGoal(goalId)
                refreshData()
            } catch (e: Exception) {
                _uiState.value = GoalUiState.Error("目標の削除に失敗しました")
            }
        }
    }

    fun toggleGoalStatus(goalId: Long) {
        viewModelScope.launch {
            try {
                val goal = goalRepository.getGoalById(goalId)
                if (goal != null) {
                    val newStatus = if (goal.status == GoalStatus.ACTIVE) GoalStatus.PAUSED else GoalStatus.ACTIVE
                    val updatedGoal = goal.copy(status = newStatus)
                    goalRepository.updateGoal(updatedGoal)
                    refreshData()
                }
            } catch (e: Exception) {
                _uiState.value = GoalUiState.Error("目標ステータスの変更に失敗しました")
            }
        }
    }

    fun markGoalAsAchieved(goalId: Long) {
        viewModelScope.launch {
            try {
                val goal = goalRepository.getGoalById(goalId)
                if (goal != null) {
                    val updatedGoal = goal.copy(
                        status = GoalStatus.ACHIEVED,
                        achievedDate = Date()
                    )
                    goalRepository.updateGoal(updatedGoal)
                    refreshData()
                }
            } catch (e: Exception) {
                _uiState.value = GoalUiState.Error("目標達成の記録に失敗しました")
            }
        }
    }

    fun formatCurrency(amount: BigDecimal): String {
        return "¥${String.format("%,.0f", amount.toFloat())}"
    }

    fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())
        return sdf.format(date)
    }

    fun getDaysUntilTarget(targetDate: Date): String {
        val today = Calendar.getInstance().time
        val diffInMillis = targetDate.time - today.time
        val days = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        
        return when {
            days < 0 -> "${-days}日経過"
            days == 0 -> "今日まで"
            days == 1 -> "あと1日"
            else -> "あと${days}日"
        }
    }
}

sealed class GoalUiState {
    object Loading : GoalUiState()
    object Success : GoalUiState()
    data class Error(val message: String) : GoalUiState()
}

enum class GoalFilter {
    ALL,
    SAVING,
    EXPENSE_REDUCTION,
    ACTIVE,
    ACHIEVED
}

enum class GoalSortOrder {
    PROGRESS_DESC,
    PROGRESS_ASC,
    TARGET_DATE_ASC,
    TARGET_DATE_DESC,
    TARGET_AMOUNT_ASC,
    TARGET_AMOUNT_DESC,
    NAME_ASC
}

data class GoalItem(
    val goal: Goal,
    val category: Category?,
    val currentAmount: BigDecimal,
    val progress: Int,
    val isAchieved: Boolean,
    val daysRemaining: Int,
    val isOverdue: Boolean,
    val milestonesPassed: List<Int>
)

data class Achievement(
    val goalName: String,
    val targetAmount: BigDecimal,
    val achievedDate: Date,
    val category: Category?
)

data class GoalProgress(
    val currentAmount: BigDecimal,
    val progressPercentage: Int,
    val isAchieved: Boolean,
    val milestonesPassed: List<Int>
)

data class TimeRemaining(
    val days: Int,
    val isOverdue: Boolean
)