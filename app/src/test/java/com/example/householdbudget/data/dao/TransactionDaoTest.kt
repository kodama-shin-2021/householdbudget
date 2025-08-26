package com.example.householdbudget.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.householdbudget.data.database.HouseholdBudgetDatabase
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.data.entity.Transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.util.*

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var database: HouseholdBudgetDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HouseholdBudgetDatabase::class.java
        ).allowMainThreadQueries().build()
        
        transactionDao = database.transactionDao()
        categoryDao = database.categoryDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetTransaction() = runTest {
        // Setup category first
        val category = Category(
            id = 1,
            name = "Test Category",
            icon = "test",
            color = "#FF0000"
        )
        categoryDao.insertCategory(category)

        // Insert transaction
        val transaction = Transaction(
            id = 1,
            amount = BigDecimal("100.50"),
            type = "expense",
            categoryId = 1,
            date = Date(),
            description = "Test transaction"
        )
        
        transactionDao.insertTransaction(transaction)

        // Retrieve and verify
        val retrieved = transactionDao.getTransactionById(1).first()
        assert(retrieved?.amount == BigDecimal("100.50"))
        assert(retrieved?.description == "Test transaction")
    }

    @Test
    fun getTransactionsByDateRange() = runTest {
        // Setup category
        val category = Category(
            id = 1,
            name = "Test Category",
            icon = "test",
            color = "#FF0000"
        )
        categoryDao.insertCategory(category)

        // Create transactions with different dates
        val today = Date()
        val yesterday = Date(today.time - 24 * 60 * 60 * 1000)
        val tomorrow = Date(today.time + 24 * 60 * 60 * 1000)

        val transactions = listOf(
            Transaction(1, BigDecimal("100"), "expense", 1, yesterday, "Yesterday"),
            Transaction(2, BigDecimal("200"), "expense", 1, today, "Today"),
            Transaction(3, BigDecimal("300"), "expense", 1, tomorrow, "Tomorrow")
        )

        transactions.forEach { transactionDao.insertTransaction(it) }

        // Query date range (yesterday to today)
        val rangeTransactions = transactionDao.getTransactionsByDateRange(
            yesterday, today
        ).first()

        assert(rangeTransactions.size == 2)
        assert(rangeTransactions.any { it.description == "Yesterday" })
        assert(rangeTransactions.any { it.description == "Today" })
        assert(!rangeTransactions.any { it.description == "Tomorrow" })
    }

    @Test
    fun getExpensesByCategoryForPeriod() = runTest {
        // Setup category
        val category = Category(
            id = 1,
            name = "Test Category",
            icon = "test",
            color = "#FF0000"
        )
        categoryDao.insertCategory(category)

        // Insert expense transactions
        val transactions = listOf(
            Transaction(1, BigDecimal("100"), "expense", 1, Date(), "Expense 1"),
            Transaction(2, BigDecimal("200"), "expense", 1, Date(), "Expense 2"),
            Transaction(3, BigDecimal("150"), "income", 1, Date(), "Income")
        )

        transactions.forEach { transactionDao.insertTransaction(it) }

        // Get expenses for category
        val startDate = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        val endDate = Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)
        
        val totalExpenses = transactionDao.getExpensesByCategoryForPeriod(
            1, startDate, endDate
        ).first()

        assert(totalExpenses == BigDecimal("300")) // Only expenses, not income
    }

    @Test
    fun deleteTransaction() = runTest {
        // Setup category
        val category = Category(
            id = 1,
            name = "Test Category",
            icon = "test",
            color = "#FF0000"
        )
        categoryDao.insertCategory(category)

        // Insert transaction
        val transaction = Transaction(
            id = 1,
            amount = BigDecimal("100"),
            type = "expense",
            categoryId = 1,
            date = Date(),
            description = "To delete"
        )
        transactionDao.insertTransaction(transaction)

        // Verify it exists
        val before = transactionDao.getAllTransactions().first()
        assert(before.size == 1)

        // Delete transaction
        transactionDao.deleteTransaction(1)

        // Verify it's gone
        val after = transactionDao.getAllTransactions().first()
        assert(after.isEmpty())
    }

    @Test
    fun updateTransaction() = runTest {
        // Setup category
        val category = Category(
            id = 1,
            name = "Test Category",
            icon = "test",
            color = "#FF0000"
        )
        categoryDao.insertCategory(category)

        // Insert transaction
        val transaction = Transaction(
            id = 1,
            amount = BigDecimal("100"),
            type = "expense",
            categoryId = 1,
            date = Date(),
            description = "Original"
        )
        transactionDao.insertTransaction(transaction)

        // Update transaction
        val updated = transaction.copy(
            amount = BigDecimal("200"),
            description = "Updated"
        )
        transactionDao.updateTransaction(updated)

        // Verify update
        val retrieved = transactionDao.getTransactionById(1).first()
        assert(retrieved?.amount == BigDecimal("200"))
        assert(retrieved?.description == "Updated")
    }
}