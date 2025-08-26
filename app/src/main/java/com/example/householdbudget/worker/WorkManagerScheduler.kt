package com.example.householdbudget.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    fun scheduleRegularTransactionWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .setRequiresStorageNotLow(false)
            .build()

        val regularTransactionWorkRequest = PeriodicWorkRequestBuilder<RegularTransactionWorker>(
            // Check every 24 hours
            24, TimeUnit.HOURS,
            // Flex interval of 2 hours (can run within 2 hours of the 24 hour mark)
            2, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            RegularTransactionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            regularTransactionWorkRequest
        )
    }

    fun cancelRegularTransactionWork() {
        workManager.cancelUniqueWork(RegularTransactionWorker.WORK_NAME)
    }

    fun scheduleImmediateCheck() {
        val immediateWorkRequest = androidx.work.OneTimeWorkRequestBuilder<RegularTransactionWorker>()
            .build()

        workManager.enqueue(immediateWorkRequest)
    }
}