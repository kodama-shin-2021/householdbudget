package com.example.householdbudget.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.householdbudget.domain.repository.RegularTransactionRepository
import com.example.householdbudget.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date

@HiltWorker
class RegularTransactionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val regularTransactionRepository: RegularTransactionRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "regular_transaction_work"
    }

    override suspend fun doWork(): Result {
        return try {
            val currentDate = Date()
            
            // Get due regular transactions
            val dueTransactions = regularTransactionRepository.getDueRegularTransactions(currentDate)
            
            for (regularTransaction in dueTransactions) {
                try {
                    if (regularTransaction.autoExecute) {
                        // Execute the regular transaction automatically
                        regularTransactionRepository.executeRegularTransaction(regularTransaction.id)
                        
                        // Show success notification
                        notificationHelper.showRegularTransactionExecutedNotification(
                            regularTransaction.name,
                            regularTransaction.amount.toString()
                        )
                    } else {
                        // Show reminder notification
                        notificationHelper.showRegularTransactionReminderNotification(
                            regularTransaction.name,
                            regularTransaction.amount.toString()
                        )
                    }
                } catch (e: Exception) {
                    // Log error but continue with other transactions
                    continue
                }
            }
            
            // Get upcoming transactions for reminder notifications
            val upcomingTransactions = regularTransactionRepository.getUpcomingRegularTransactions(
                currentDate,
                getDatePlusDays(currentDate, 7) // Next 7 days
            )
            
            for (regularTransaction in upcomingTransactions) {
                val daysUntilExecution = calculateDaysUntil(currentDate, regularTransaction.nextOccurrence)
                
                if (daysUntilExecution == regularTransaction.notifyBeforeDays && !regularTransaction.autoExecute) {
                    // Send reminder notification
                    notificationHelper.showRegularTransactionUpcomingNotification(
                        regularTransaction.name,
                        regularTransaction.amount.toString(),
                        daysUntilExecution
                    )
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun getDatePlusDays(date: Date, days: Int): Date {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = date
        calendar.add(java.util.Calendar.DAY_OF_MONTH, days)
        return calendar.time
    }

    private fun calculateDaysUntil(fromDate: Date, toDate: Date): Int {
        val diffInMillis = toDate.time - fromDate.time
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
    }
}