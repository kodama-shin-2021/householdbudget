package com.example.householdbudget.data.repository

import androidx.lifecycle.LiveData
import com.example.householdbudget.data.dao.TransactionDao
import com.example.householdbudget.data.entity.Transaction
import com.example.householdbudget.data.entity.TransactionType
import com.example.householdbudget.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): LiveData<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return withContext(Dispatchers.IO) {
            transactionDao.getTransactionById(id)
        }
    }

    override fun getTransactionsByType(type: TransactionType): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsByType(type)
    }

    override fun getTransactionsByCategory(categoryId: Long): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(categoryId)
    }

    override fun getTransactionsByDateRange(startDate: Date, endDate: Date): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
    }

    override fun getTransactionsByCategoryAndDateRange(
        categoryId: Long,
        startDate: Date,
        endDate: Date
    ): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsByCategoryAndDateRange(categoryId, startDate, endDate)
    }

    override fun getTransactionsSortedByAmount(): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsSortedByAmount()
    }

    override fun searchTransactions(keyword: String): LiveData<List<Transaction>> {
        return transactionDao.searchTransactions(keyword)
    }

    override suspend fun getTotalIncomeInPeriod(startDate: Date, endDate: Date): BigDecimal {
        return withContext(Dispatchers.IO) {
            transactionDao.getTotalIncomeInPeriod(startDate, endDate) ?: BigDecimal.ZERO
        }
    }

    override suspend fun getTotalExpenseInPeriod(startDate: Date, endDate: Date): BigDecimal {
        return withContext(Dispatchers.IO) {
            transactionDao.getTotalExpenseInPeriod(startDate, endDate) ?: BigDecimal.ZERO
        }
    }

    override suspend fun getCategoryExpenseInPeriod(categoryId: Long, startDate: Date, endDate: Date): BigDecimal {
        return withContext(Dispatchers.IO) {
            transactionDao.getCategoryExpenseInPeriod(categoryId, startDate, endDate) ?: BigDecimal.ZERO
        }
    }

    override fun getRecentTransactions(limit: Int): LiveData<List<Transaction>> {
        return transactionDao.getRecentTransactions(limit)
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return withContext(Dispatchers.IO) {
            transactionDao.insertTransaction(transaction)
        }
    }

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        withContext(Dispatchers.IO) {
            transactionDao.insertTransactions(transactions)
        }
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        withContext(Dispatchers.IO) {
            transactionDao.updateTransaction(transaction.copy(updatedAt = Date()))
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        withContext(Dispatchers.IO) {
            transactionDao.deleteTransaction(transaction)
        }
    }

    override suspend fun deleteTransactionById(id: Long) {
        withContext(Dispatchers.IO) {
            transactionDao.deleteTransactionById(id)
        }
    }

    override suspend fun deleteAllTransactions() {
        withContext(Dispatchers.IO) {
            transactionDao.deleteAllTransactions()
        }
    }
}