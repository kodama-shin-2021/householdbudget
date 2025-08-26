package com.example.householdbudget.domain.repository

import androidx.lifecycle.LiveData
import com.example.householdbudget.data.entity.Transaction
import com.example.householdbudget.data.entity.TransactionType
import java.math.BigDecimal
import java.util.Date

interface TransactionRepository {
    
    fun getAllTransactions(): LiveData<List<Transaction>>
    
    suspend fun getTransactionById(id: Long): Transaction?
    
    fun getTransactionsByType(type: TransactionType): LiveData<List<Transaction>>
    
    fun getTransactionsByCategory(categoryId: Long): LiveData<List<Transaction>>
    
    fun getTransactionsByDateRange(startDate: Date, endDate: Date): LiveData<List<Transaction>>
    
    fun getTransactionsByCategoryAndDateRange(
        categoryId: Long,
        startDate: Date,
        endDate: Date
    ): LiveData<List<Transaction>>
    
    fun getTransactionsSortedByAmount(): LiveData<List<Transaction>>
    
    fun searchTransactions(keyword: String): LiveData<List<Transaction>>
    
    suspend fun getTotalIncomeInPeriod(startDate: Date, endDate: Date): BigDecimal
    
    suspend fun getTotalExpenseInPeriod(startDate: Date, endDate: Date): BigDecimal
    
    suspend fun getCategoryExpenseInPeriod(categoryId: Long, startDate: Date, endDate: Date): BigDecimal
    
    fun getRecentTransactions(limit: Int): LiveData<List<Transaction>>
    
    suspend fun insertTransaction(transaction: Transaction): Long
    
    suspend fun insertTransactions(transactions: List<Transaction>)
    
    suspend fun updateTransaction(transaction: Transaction)
    
    suspend fun deleteTransaction(transaction: Transaction)
    
    suspend fun deleteTransactionById(id: Long)
    
    suspend fun deleteAllTransactions()
}