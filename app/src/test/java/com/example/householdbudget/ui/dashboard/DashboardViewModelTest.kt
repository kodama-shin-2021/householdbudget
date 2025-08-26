package com.example.householdbudget.ui.dashboard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.data.entity.Transaction
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
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.*

@ExperimentalCoroutinesApi
class DashboardViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var transactionRepository: TransactionRepository

    @Mock
    private lateinit var budgetRepository: BudgetRepository

    @Mock
    private lateinit var categoryRepository: CategoryRepository

    @Mock
    private lateinit var dashboardDataObserver: Observer<DashboardData>

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        viewModel = DashboardViewModel(
            transactionRepository,
            budgetRepository,
            categoryRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadDashboardData should calculate correct monthly totals`() = runTest {
        // Given
        val transactions = listOf(
            Transaction(
                id = 1,
                amount = BigDecimal("1000.00"),
                type = "income",
                categoryId = 1,
                date = Date(),
                description = "Salary"
            ),
            Transaction(
                id = 2,
                amount = BigDecimal("300.00"),
                type = "expense",
                categoryId = 2,
                date = Date(),
                description = "Groceries"
            )
        )

        whenever(transactionRepository.getTransactionsByDateRange(
            any(), any()
        )).thenReturn(flowOf(transactions))
        whenever(categoryRepository.getAllCategories()).thenReturn(flowOf(emptyList()))
        whenever(budgetRepository.getAllBudgets()).thenReturn(flowOf(emptyList()))

        viewModel.dashboardData.observeForever(dashboardDataObserver)

        // When
        viewModel.loadDashboardData()

        // Then
        testDispatcher.scheduler.advanceUntilIdle()

        val dashboardData = viewModel.dashboardData.value
        assert(dashboardData?.totalIncome == BigDecimal("1000.00"))
        assert(dashboardData?.totalExpense == BigDecimal("300.00"))
        assert(dashboardData?.balance == BigDecimal("700.00"))
    }

    @Test
    fun `switchPeriod should update current period and reload data`() = runTest {
        // Given
        whenever(transactionRepository.getTransactionsByDateRange(
            any(), any()
        )).thenReturn(flowOf(emptyList()))
        whenever(categoryRepository.getAllCategories()).thenReturn(flowOf(emptyList()))
        whenever(budgetRepository.getAllBudgets()).thenReturn(flowOf(emptyList()))

        // When
        viewModel.switchPeriod(DashboardPeriod.WEEKLY)

        // Then
        testDispatcher.scheduler.advanceUntilIdle()

        assert(viewModel.currentPeriod.value == DashboardPeriod.WEEKLY)
    }

    private fun <T> any(): T = org.mockito.kotlin.any()
}