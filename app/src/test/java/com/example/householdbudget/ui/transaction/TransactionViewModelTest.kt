package com.example.householdbudget.ui.transaction

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.data.entity.Transaction
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
import java.util.*

@ExperimentalCoroutinesApi
class TransactionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var transactionRepository: TransactionRepository

    @Mock
    private lateinit var categoryRepository: CategoryRepository

    @Mock
    private lateinit var transactionsObserver: Observer<List<Transaction>>

    @Mock
    private lateinit var categoriesObserver: Observer<List<Category>>

    private lateinit var viewModel: TransactionViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        viewModel = TransactionViewModel(
            transactionRepository,
            categoryRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addTransaction should call repository insert with correct data`() = runTest {
        // Given
        val amount = BigDecimal("100.50")
        val type = "expense"
        val categoryId = 1L
        val description = "Test transaction"
        val date = Date()

        // When
        viewModel.addTransaction(amount, type, categoryId, description, date)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(transactionRepository).insertTransaction(
            org.mockito.kotlin.argThat { transaction ->
                transaction.amount == amount &&
                transaction.type == type &&
                transaction.categoryId == categoryId &&
                transaction.description == description
            }
        )
    }

    @Test
    fun `deleteTransaction should call repository delete`() = runTest {
        // Given
        val transactionId = 1L

        // When
        viewModel.deleteTransaction(transactionId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(transactionRepository).deleteTransaction(transactionId)
    }

    @Test
    fun `searchTransactions should filter by description`() = runTest {
        // Given
        val allTransactions = listOf(
            Transaction(1, BigDecimal("100"), "expense", 1, Date(), "Groceries"),
            Transaction(2, BigDecimal("50"), "expense", 1, Date(), "Coffee"),
            Transaction(3, BigDecimal("1000"), "income", 2, Date(), "Salary")
        )
        
        whenever(transactionRepository.getAllTransactions()).thenReturn(flowOf(allTransactions))
        viewModel.transactions.observeForever(transactionsObserver)

        // When
        viewModel.searchTransactions("Gr")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val filteredTransactions = viewModel.transactions.value
        assert(filteredTransactions?.size == 1)
        assert(filteredTransactions?.first()?.description == "Groceries")
    }

    @Test
    fun `filterByCategory should show only transactions from selected category`() = runTest {
        // Given
        val allTransactions = listOf(
            Transaction(1, BigDecimal("100"), "expense", 1, Date(), "Food"),
            Transaction(2, BigDecimal("50"), "expense", 2, Date(), "Transport"),
            Transaction(3, BigDecimal("200"), "expense", 1, Date(), "Restaurant")
        )
        
        whenever(transactionRepository.getAllTransactions()).thenReturn(flowOf(allTransactions))
        viewModel.transactions.observeForever(transactionsObserver)

        // When
        viewModel.filterByCategory(1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val filteredTransactions = viewModel.transactions.value
        assert(filteredTransactions?.size == 2)
        assert(filteredTransactions?.all { it.categoryId == 1L } == true)
    }

    @Test
    fun `validation should return false for invalid amount`() {
        // Given
        val invalidAmount = BigDecimal.ZERO

        // When
        val isValid = viewModel.validateTransaction(invalidAmount, "expense", 1, "Test")

        // Then
        assert(!isValid)
    }

    @Test
    fun `validation should return true for valid transaction data`() {
        // Given
        val validAmount = BigDecimal("100.00")

        // When
        val isValid = viewModel.validateTransaction(validAmount, "expense", 1, "Test")

        // Then
        assert(isValid)
    }
}