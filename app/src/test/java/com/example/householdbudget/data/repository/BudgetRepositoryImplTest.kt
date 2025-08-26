package com.example.householdbudget.data.repository

import com.example.householdbudget.data.dao.BudgetDao
import com.example.householdbudget.data.entity.Budget
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class BudgetRepositoryImplTest {

    @Mock
    private lateinit var budgetDao: BudgetDao

    private lateinit var repository: BudgetRepositoryImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = BudgetRepositoryImpl(budgetDao)
    }

    @Test
    fun getAllBudgets_ShouldReturnFlowFromDao() = runTest {
        // Given
        val budgets = listOf(
            Budget(1, 1, BigDecimal("500"), "monthly"),
            Budget(2, 2, BigDecimal("200"), "weekly")
        )
        whenever(budgetDao.getAllBudgets()).thenReturn(flowOf(budgets))

        // When
        val result = repository.getAllBudgets().first()

        // Then
        assert(result == budgets)
        verify(budgetDao).getAllBudgets()
    }

    @Test
    fun insertBudget_ShouldCallDaoInsert() = runTest {
        // Given
        val budget = Budget(0, 1, BigDecimal("500"), "monthly")

        // When
        repository.insertBudget(budget)

        // Then
        verify(budgetDao).insertBudget(budget)
    }

    @Test
    fun updateBudget_ShouldCallDaoUpdate() = runTest {
        // Given
        val budget = Budget(1, 1, BigDecimal("600"), "monthly")

        // When
        repository.updateBudget(budget)

        // Then
        verify(budgetDao).updateBudget(budget)
    }

    @Test
    fun deleteBudget_ShouldCallDaoDelete() = runTest {
        // Given
        val budgetId = 1L

        // When
        repository.deleteBudget(budgetId)

        // Then
        verify(budgetDao).deleteBudget(budgetId)
    }

    @Test
    fun getBudgetByCategory_ShouldReturnFlowFromDao() = runTest {
        // Given
        val categoryId = 1L
        val budget = Budget(1, categoryId, BigDecimal("500"), "monthly")
        whenever(budgetDao.getBudgetByCategory(categoryId)).thenReturn(flowOf(budget))

        // When
        val result = repository.getBudgetByCategory(categoryId).first()

        // Then
        assert(result == budget)
        verify(budgetDao).getBudgetByCategory(categoryId)
    }
}