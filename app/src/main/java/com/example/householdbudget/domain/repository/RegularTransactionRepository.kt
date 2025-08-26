package com.example.householdbudget.domain.repository

import androidx.lifecycle.LiveData
import com.example.householdbudget.data.entity.RecurrenceFrequency
import com.example.householdbudget.data.entity.RegularTransaction
import com.example.householdbudget.data.entity.TransactionType
import java.util.Date

interface RegularTransactionRepository {
    
    fun getActiveRegularTransactions(): LiveData<List<RegularTransaction>>
    
    fun getAllRegularTransactions(): LiveData<List<RegularTransaction>>
    
    suspend fun getRegularTransactionById(id: Long): RegularTransaction?
    
    fun getRegularTransactionsByType(type: TransactionType): LiveData<List<RegularTransaction>>
    
    fun getRegularTransactionsByCategory(categoryId: Long): LiveData<List<RegularTransaction>>
    
    fun getRegularTransactionsByFrequency(frequency: RecurrenceFrequency): LiveData<List<RegularTransaction>>
    
    suspend fun getDueRegularTransactions(date: Date): List<RegularTransaction>
    
    suspend fun getUpcomingRegularTransactions(startDate: Date, endDate: Date): List<RegularTransaction>
    
    suspend fun searchRegularTransactionsByName(name: String): List<RegularTransaction>
    
    suspend fun insertRegularTransaction(regularTransaction: RegularTransaction): Long
    
    suspend fun insertRegularTransactions(regularTransactions: List<RegularTransaction>)
    
    suspend fun updateRegularTransaction(regularTransaction: RegularTransaction)
    
    suspend fun deleteRegularTransaction(regularTransaction: RegularTransaction)
    
    suspend fun deleteRegularTransactionById(id: Long)
    
    suspend fun deactivateRegularTransaction(id: Long)
    
    suspend fun activateRegularTransaction(id: Long)
    
    suspend fun updateNextOccurrence(id: Long, nextDate: Date)
    
    suspend fun calculateNextOccurrence(regularTransaction: RegularTransaction): Date
    
    suspend fun executeRegularTransaction(regularTransactionId: Long): Long
}