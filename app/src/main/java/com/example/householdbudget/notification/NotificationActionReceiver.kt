package com.example.householdbudget.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.householdbudget.worker.WorkManagerScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_EXECUTE_REGULAR_TRANSACTION = "com.example.householdbudget.EXECUTE_REGULAR_TRANSACTION"
        const val ACTION_SNOOZE_REGULAR_TRANSACTION = "com.example.householdbudget.SNOOZE_REGULAR_TRANSACTION"
        const val EXTRA_TRANSACTION_NAME = "transaction_name"
    }

    @Inject
    lateinit var workManagerScheduler: WorkManagerScheduler

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_EXECUTE_REGULAR_TRANSACTION -> {
                val transactionName = intent.getStringExtra(EXTRA_TRANSACTION_NAME)
                handleExecuteRegularTransaction(transactionName)
            }
            ACTION_SNOOZE_REGULAR_TRANSACTION -> {
                val transactionName = intent.getStringExtra(EXTRA_TRANSACTION_NAME)
                handleSnoozeRegularTransaction(transactionName)
            }
        }
    }

    private fun handleExecuteRegularTransaction(transactionName: String?) {
        // Cancel the notification
        notificationHelper.cancelRegularTransactionNotification()
        
        // Schedule immediate check to execute the transaction
        workManagerScheduler.scheduleImmediateCheck()
        
        // Show confirmation notification
        transactionName?.let {
            notificationHelper.showRegularTransactionExecutedNotification(it, "")
        }
    }

    private fun handleSnoozeRegularTransaction(transactionName: String?) {
        // Cancel the notification
        notificationHelper.cancelRegularTransactionNotification()
        
        // The user will be reminded again in the next scheduled check
    }
}