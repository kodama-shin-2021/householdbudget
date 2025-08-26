package com.example.householdbudget.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.example.householdbudget.data.dao.TransactionDao
import com.example.householdbudget.data.entity.Transaction
import com.example.householdbudget.data.entity.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.util.Date

@ExperimentalCoroutinesApi
class TransactionRepositoryImplTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var transactionDao: TransactionDao

    private lateinit var repository: TransactionRepositoryImpl

    private val sampleTransaction = Transaction(
        id = 1L,
        amount = BigDecimal("100.00"),
        type = TransactionType.EXPENSE,
        categoryId = 1L,
        description = "Test transaction",
        date = Date()
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = TransactionRepositoryImpl(transactionDao)
    }

    @Test
    fun `getAllTransactions returns LiveData from DAO`() {
        // Given
        val liveData = MutableLiveData<List<Transaction>>()
        whenever(transactionDao.getAllTransactions()).thenReturn(liveData)

        // When
        val result = repository.getAllTransactions()

        // Then
        verify(transactionDao).getAllTransactions()
        assert(result == liveData)
    }

    @Test
    fun `getTransactionById returns transaction from DAO`() = runTest {
        // Given
        val transactionId = 1L
        whenever(transactionDao.getTransactionById(transactionId)).thenReturn(sampleTransaction)

        // When
        val result = repository.getTransactionById(transactionId)

        // Then
        verify(transactionDao).getTransactionById(transactionId)
        assert(result == sampleTransaction)
    }

    @Test
    fun `insertTransaction calls DAO and returns ID`() = runTest {
        // Given
        val expectedId = 1L
        whenever(transactionDao.insertTransaction(sampleTransaction)).thenReturn(expectedId)

        // When
        val result = repository.insertTransaction(sampleTransaction)

        // Then
        verify(transactionDao).insertTransaction(sampleTransaction)
        assert(result == expectedId)
    }

    @Test
    fun `updateTransaction calls DAO with updated timestamp`() = runTest {
        // When
        repository.updateTransaction(sampleTransaction)

        // Then
        verify(transactionDao).updateTransaction(argThat { updatedAt != sampleTransaction.updatedAt })
    }

    @Test
    fun `deleteTransaction calls DAO`() = runTest {
        // When
        repository.deleteTransaction(sampleTransaction)

        // Then
        verify(transactionDao).deleteTransaction(sampleTransaction)
    }

    @Test
    fun `getTotalIncomeInPeriod returns zero when DAO returns null`() = runTest {
        // Given
        val startDate = Date()
        val endDate = Date()
        whenever(transactionDao.getTotalIncomeInPeriod(startDate, endDate)).thenReturn(null)

        // When
        val result = repository.getTotalIncomeInPeriod(startDate, endDate)

        // Then
        assert(result == BigDecimal.ZERO)
    }

    @Test
    fun `getTotalIncomeInPeriod returns value from DAO`() = runTest {
        // Given
        val startDate = Date()
        val endDate = Date()
        val expectedAmount = BigDecimal("500.00")
        whenever(transactionDao.getTotalIncomeInPeriod(startDate, endDate)).thenReturn(expectedAmount)

        // When
        val result = repository.getTotalIncomeInPeriod(startDate, endDate)

        // Then
        assert(result == expectedAmount)
    }
}