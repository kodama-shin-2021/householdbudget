package com.example.householdbudget.data.repository

import androidx.lifecycle.LiveData
import com.example.householdbudget.data.dao.RegularTransactionDao
import com.example.householdbudget.data.dao.TransactionDao
import com.example.householdbudget.data.entity.RecurrenceFrequency
import com.example.householdbudget.data.entity.RegularTransaction
import com.example.householdbudget.data.entity.Transaction
import com.example.householdbudget.data.entity.TransactionType
import com.example.householdbudget.domain.repository.RegularTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegularTransactionRepositoryImpl @Inject constructor(
    private val regularTransactionDao: RegularTransactionDao,
    private val transactionDao: TransactionDao
) : RegularTransactionRepository {

    override fun getActiveRegularTransactions(): LiveData<List<RegularTransaction>> {
        return regularTransactionDao.getActiveRegularTransactions()
    }

    override fun getAllRegularTransactions(): LiveData<List<RegularTransaction>> {
        return regularTransactionDao.getAllRegularTransactions()
    }

    override suspend fun getRegularTransactionById(id: Long): RegularTransaction? {
        return withContext(Dispatchers.IO) {
            regularTransactionDao.getRegularTransactionById(id)
        }
    }

    override fun getRegularTransactionsByType(type: TransactionType): LiveData<List<RegularTransaction>> {
        return regularTransactionDao.getRegularTransactionsByType(type)
    }

    override fun getRegularTransactionsByCategory(categoryId: Long): LiveData<List<RegularTransaction>> {
        return regularTransactionDao.getRegularTransactionsByCategory(categoryId)
    }

    override fun getRegularTransactionsByFrequency(frequency: RecurrenceFrequency): LiveData<List<RegularTransaction>> {
        return regularTransactionDao.getRegularTransactionsByFrequency(frequency)
    }

    override suspend fun getDueRegularTransactions(date: Date): List<RegularTransaction> {
        return withContext(Dispatchers.IO) {
            regularTransactionDao.getDueRegularTransactions(date)
        }
    }

    override suspend fun getUpcomingRegularTransactions(startDate: Date, endDate: Date): List<RegularTransaction> {
        return withContext(Dispatchers.IO) {
            regularTransactionDao.getUpcomingRegularTransactions(startDate, endDate)
        }
    }

    override suspend fun searchRegularTransactionsByName(name: String): List<RegularTransaction> {
        return withContext(Dispatchers.IO) {
            regularTransactionDao.searchRegularTransactionsByName(name)
        }
    }

    override suspend fun insertRegularTransaction(regularTransaction: RegularTransaction): Long {
        return withContext(Dispatchers.IO) {
            regularTransactionDao.insertRegularTransaction(regularTransaction)
        }
    }

    override suspend fun insertRegularTransactions(regularTransactions: List<RegularTransaction>) {
        withContext(Dispatchers.IO) {
            regularTransactionDao.insertRegularTransactions(regularTransactions)
        }
    }

    override suspend fun updateRegularTransaction(regularTransaction: RegularTransaction) {
        withContext(Dispatchers.IO) {
            regularTransactionDao.updateRegularTransaction(regularTransaction.copy(updatedAt = Date()))
        }
    }

    override suspend fun deleteRegularTransaction(regularTransaction: RegularTransaction) {
        withContext(Dispatchers.IO) {
            regularTransactionDao.deleteRegularTransaction(regularTransaction)
        }
    }

    override suspend fun deleteRegularTransactionById(id: Long) {
        withContext(Dispatchers.IO) {
            regularTransactionDao.deleteRegularTransactionById(id)
        }
    }

    override suspend fun deactivateRegularTransaction(id: Long) {
        withContext(Dispatchers.IO) {
            regularTransactionDao.deactivateRegularTransaction(id)
        }
    }

    override suspend fun activateRegularTransaction(id: Long) {
        withContext(Dispatchers.IO) {
            regularTransactionDao.activateRegularTransaction(id)
        }
    }

    override suspend fun updateNextOccurrence(id: Long, nextDate: Date) {
        withContext(Dispatchers.IO) {
            regularTransactionDao.updateNextOccurrence(id, nextDate)
        }
    }

    override suspend fun calculateNextOccurrence(regularTransaction: RegularTransaction): Date {
        return withContext(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            calendar.time = regularTransaction.nextOccurrence
            
            when (regularTransaction.frequency) {
                RecurrenceFrequency.DAILY -> {
                    calendar.add(Calendar.DAY_OF_MONTH, regularTransaction.interval)
                }
                RecurrenceFrequency.WEEKLY -> {
                    calendar.add(Calendar.WEEK_OF_YEAR, regularTransaction.interval)
                }
                RecurrenceFrequency.MONTHLY -> {
                    calendar.add(Calendar.MONTH, regularTransaction.interval)
                }
                RecurrenceFrequency.YEARLY -> {
                    calendar.add(Calendar.YEAR, regularTransaction.interval)
                }
            }
            
            calendar.time
        }
    }

    override suspend fun executeRegularTransaction(regularTransactionId: Long): Long {
        return withContext(Dispatchers.IO) {
            val regularTransaction = regularTransactionDao.getRegularTransactionById(regularTransactionId)
            if (regularTransaction != null) {
                // 通常の取引として記録
                val transaction = Transaction(
                    amount = regularTransaction.amount,
                    type = regularTransaction.type,
                    categoryId = regularTransaction.categoryId,
                    subcategoryId = regularTransaction.subcategoryId,
                    description = regularTransaction.description,
                    date = Date(),
                    currency = regularTransaction.currency,
                    regularTransactionId = regularTransactionId
                )
                
                val transactionId = transactionDao.insertTransaction(transaction)
                
                // 次回実行日を更新
                val nextOccurrence = calculateNextOccurrence(regularTransaction)
                regularTransactionDao.updateNextOccurrence(regularTransactionId, nextOccurrence)
                
                transactionId
            } else {
                throw IllegalArgumentException("Regular transaction not found: $regularTransactionId")
            }
        }
    }
}