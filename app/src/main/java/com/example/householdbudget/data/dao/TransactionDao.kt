package com.example.householdbudget.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.householdbudget.data.entity.Transaction
import com.example.householdbudget.data.entity.TransactionType
import java.math.BigDecimal
import java.util.Date

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?
    
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: TransactionType): LiveData<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: Long): LiveData<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Date, endDate: Date): LiveData<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate AND categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategoryAndDateRange(
        categoryId: Long,
        startDate: Date,
        endDate: Date
    ): LiveData<List<Transaction>>
    
    @Query("SELECT * FROM transactions ORDER BY amount DESC")
    fun getTransactionsSortedByAmount(): LiveData<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE description LIKE '%' || :keyword || '%' ORDER BY date DESC")
    fun searchTransactions(keyword: String): LiveData<List<Transaction>>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalIncomeInPeriod(startDate: Date, endDate: Date): BigDecimal?
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpenseInPeriod(startDate: Date, endDate: Date): BigDecimal?
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND categoryId = :categoryId AND date BETWEEN :startDate AND :endDate")
    suspend fun getCategoryExpenseInPeriod(categoryId: Long, startDate: Date, endDate: Date): BigDecimal?
    
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): LiveData<List<Transaction>>
    
    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long
    
    @Insert
    suspend fun insertTransactions(transactions: List<Transaction>)
    
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
    
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}