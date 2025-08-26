package com.example.householdbudget.ui.budget

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.householdbudget.data.entity.Budget
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.domain.repository.BudgetRepository
import com.example.householdbudget.domain.repository.CategoryRepository
import com.example.householdbudget.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class BudgetViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var budgetRepository: BudgetRepository

    @Mock
    private lateinit var categoryRepository: CategoryRepository

    @Mock
    private lateinit var transactionRepository: TransactionRepository

    @Mock
    private lateinit var categoryBudgetsObserver: Observer<List<CategoryBudget>>

    private lateinit var viewModel: BudgetViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        viewModel = BudgetViewModel(
            budgetRepository,
            categoryRepository,
            transactionRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveBudget should call repository insert`() = runTest {
        // Given
        val budget = Budget(
            id = 0,
            categoryId = 1,
            amount = BigDecimal("500.00"),
            period = "monthly"
        )

        // When
        viewModel.saveBudget(budget)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(budgetRepository).insertBudget(budget)
    }

    @Test
    fun `deleteBudget should call repository delete`() = runTest {
        // Given
        val budgetId = 1L

        // When
        viewModel.deleteBudget(budgetId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(budgetRepository).deleteBudget(budgetId)
    }

    @Test
    fun `loadBudgets should combine budget and category data correctly`() = runTest {
        // Given
        val categories = listOf(
            Category(id = 1, name = "Food", icon = "food", color = "#FF0000"),
            Category(id = 2, name = "Transport", icon = "car", color = "#00FF00")
        )
        
        val budgets = listOf(
            Budget(id = 1, categoryId = 1, amount = BigDecimal("500.00"), period = "monthly"),
            Budget(id = 2, categoryId = 2, amount = BigDecimal("200.00"), period = "monthly")
        )

        whenever(categoryRepository.getAllCategories()).thenReturn(flowOf(categories))
        whenever(budgetRepository.getAllBudgets()).thenReturn(flowOf(budgets))
        whenever(transactionRepository.getExpensesByCategoryForPeriod(
            any(), any(), any()
        )).thenReturn(flowOf(BigDecimal.ZERO))

        viewModel.categoryBudgets.observeForever(categoryBudgetsObserver)

        // When
        viewModel.loadBudgets()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val categoryBudgets = viewModel.categoryBudgets.value
        assert(categoryBudgets?.size == 2)
        assert(categoryBudgets?.find { it.category.name == "Food" }?.budget?.amount == BigDecimal("500.00"))
    }

    private fun <T> any(): T = org.mockito.kotlin.any()
}