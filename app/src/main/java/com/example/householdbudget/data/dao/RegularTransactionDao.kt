package com.example.householdbudget.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.householdbudget.data.entity.RecurrenceFrequency
import com.example.householdbudget.data.entity.RegularTransaction
import com.example.householdbudget.data.entity.TransactionType
import java.util.Date

@Dao
interface RegularTransactionDao {
    
    @Query("SELECT * FROM regular_transactions WHERE isActive = 1 ORDER BY nextOccurrence ASC")
    fun getActiveRegularTransactions(): LiveData<List<RegularTransaction>>
    
    @Query("SELECT * FROM regular_transactions ORDER BY createdAt DESC")
    fun getAllRegularTransactions(): LiveData<List<RegularTransaction>>
    
    @Query("SELECT * FROM regular_transactions WHERE id = :id")
    suspend fun getRegularTransactionById(id: Long): RegularTransaction?
    
    @Query("SELECT * FROM regular_transactions WHERE type = :type AND isActive = 1")
    fun getRegularTransactionsByType(type: TransactionType): LiveData<List<RegularTransaction>>
    
    @Query("SELECT * FROM regular_transactions WHERE categoryId = :categoryId AND isActive = 1")
    fun getRegularTransactionsByCategory(categoryId: Long): LiveData<List<RegularTransaction>>
    
    @Query("SELECT * FROM regular_transactions WHERE frequency = :frequency AND isActive = 1")
    fun getRegularTransactionsByFrequency(frequency: RecurrenceFrequency): LiveData<List<RegularTransaction>>
    
    @Query("SELECT * FROM regular_transactions WHERE nextOccurrence <= :date AND isActive = 1 ORDER BY nextOccurrence ASC")
    suspend fun getDueRegularTransactions(date: Date): List<RegularTransaction>
    
    @Query("SELECT * FROM regular_transactions WHERE nextOccurrence BETWEEN :startDate AND :endDate AND isActive = 1 ORDER BY nextOccurrence ASC")
    suspend fun getUpcomingRegularTransactions(startDate: Date, endDate: Date): List<RegularTransaction>
    
    @Query("SELECT * FROM regular_transactions WHERE name LIKE '%' || :name || '%'")
    suspend fun searchRegularTransactionsByName(name: String): List<RegularTransaction>
    
    @Insert
    suspend fun insertRegularTransaction(regularTransaction: RegularTransaction): Long
    
    @Insert
    suspend fun insertRegularTransactions(regularTransactions: List<RegularTransaction>)
    
    @Update
    suspend fun updateRegularTransaction(regularTransaction: RegularTransaction)
    
    @Delete
    suspend fun deleteRegularTransaction(regularTransaction: RegularTransaction)
    
    @Query("DELETE FROM regular_transactions WHERE id = :id")
    suspend fun deleteRegularTransactionById(id: Long)
    
    @Query("UPDATE regular_transactions SET isActive = 0 WHERE id = :id")
    suspend fun deactivateRegularTransaction(id: Long)
    
    @Query("UPDATE regular_transactions SET isActive = 1 WHERE id = :id")
    suspend fun activateRegularTransaction(id: Long)
    
    @Query("UPDATE regular_transactions SET nextOccurrence = :nextDate WHERE id = :id")
    suspend fun updateNextOccurrence(id: Long, nextDate: Date)
}