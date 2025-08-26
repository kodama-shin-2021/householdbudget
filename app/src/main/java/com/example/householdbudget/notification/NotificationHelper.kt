package com.example.householdbudget.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.householdbudget.R
import com.example.householdbudget.ui.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val CHANNEL_ID_REGULAR_TRANSACTIONS = "regular_transactions"
        private const val CHANNEL_ID_BUDGET_ALERTS = "budget_alerts"
        private const val CHANNEL_ID_GOALS = "goals"
        
        private const val NOTIFICATION_ID_REGULAR_TRANSACTION_REMINDER = 1001
        private const val NOTIFICATION_ID_REGULAR_TRANSACTION_EXECUTED = 1002
        private const val NOTIFICATION_ID_REGULAR_TRANSACTION_UPCOMING = 1003
        private const val NOTIFICATION_ID_BUDGET_ALERT = 2001
        private const val NOTIFICATION_ID_BUDGET_EXCEEDED = 2002
        private const val NOTIFICATION_ID_BUDGET_NEAR_LIMIT = 2003
        private const val NOTIFICATION_ID_TOTAL_BUDGET_EXCEEDED = 2004
        private const val NOTIFICATION_ID_TOTAL_BUDGET_NEAR_LIMIT = 2005
        private const val NOTIFICATION_ID_GOAL_ACHIEVED = 3001
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Regular Transactions Channel
            val regularTransactionsChannel = NotificationChannel(
                CHANNEL_ID_REGULAR_TRANSACTIONS,
                context.getString(R.string.notification_channel_regular_transactions),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_regular_transactions_description)
            }

            // Budget Alerts Channel
            val budgetAlertsChannel = NotificationChannel(
                CHANNEL_ID_BUDGET_ALERTS,
                context.getString(R.string.notification_channel_budget_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_budget_alerts_description)
            }

            // Goals Channel
            val goalsChannel = NotificationChannel(
                CHANNEL_ID_GOALS,
                context.getString(R.string.notification_channel_goals),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_goals_description)
            }

            notificationManager.createNotificationChannels(listOf(
                regularTransactionsChannel,
                budgetAlertsChannel,
                goalsChannel
            ))
        }
    }

    fun showRegularTransactionReminderNotification(transactionName: String, amount: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REGULAR_TRANSACTIONS)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("定期取引のリマインダー")
            .setContentText("$transactionName (¥$amount) の実行時期です")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("定期取引「$transactionName」(¥$amount) の実行予定日になりました。アプリを開いて確認してください。"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_check,
                "今すぐ実行",
                createExecuteActionPendingIntent(transactionName)
            )
            .addAction(
                R.drawable.ic_schedule,
                "後で実行",
                createSnoozeActionPendingIntent(transactionName)
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID_REGULAR_TRANSACTION_REMINDER, notification)
    }

    fun showRegularTransactionExecutedNotification(transactionName: String, amount: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REGULAR_TRANSACTIONS)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle("定期取引を実行しました")
            .setContentText("$transactionName (¥$amount) を記録しました")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_REGULAR_TRANSACTION_EXECUTED, notification)
    }

    fun showRegularTransactionUpcomingNotification(transactionName: String, amount: String, daysUntil: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REGULAR_TRANSACTIONS)
            .setSmallIcon(R.drawable.ic_schedule)
            .setContentTitle("定期取引の予定")
            .setContentText("$transactionName (¥$amount) まで${daysUntil}日")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_REGULAR_TRANSACTION_UPCOMING, notification)
    }

    fun showBudgetAlertNotification(categoryName: String, usedAmount: String, budgetAmount: String, percentage: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BUDGET_ALERTS)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("予算アラート")
            .setContentText("${categoryName}の予算を${percentage}%使用しました")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${categoryName}カテゴリの予算使用率が${percentage}%に達しました。\n使用額: ¥$usedAmount / 予算: ¥$budgetAmount"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.warning_500))
            .build()

        notificationManager.notify(NOTIFICATION_ID_BUDGET_ALERT, notification)
    }

    fun showGoalAchievedNotification(goalName: String, targetAmount: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GOALS)
            .setSmallIcon(R.drawable.ic_celebration)
            .setContentTitle("目標達成！")
            .setContentText("「$goalName」を達成しました！")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("おめでとうございます！目標「$goalName」(¥$targetAmount) を達成しました。"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.success_500))
            .build()

        notificationManager.notify(NOTIFICATION_ID_GOAL_ACHIEVED, notification)
    }

    private fun createExecuteActionPendingIntent(transactionName: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_EXECUTE_REGULAR_TRANSACTION
            putExtra(NotificationActionReceiver.EXTRA_TRANSACTION_NAME, transactionName)
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createSnoozeActionPendingIntent(transactionName: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE_REGULAR_TRANSACTION
            putExtra(NotificationActionReceiver.EXTRA_TRANSACTION_NAME, transactionName)
        }
        return PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancelRegularTransactionNotification() {
        notificationManager.cancel(NOTIFICATION_ID_REGULAR_TRANSACTION_REMINDER)
    }

    fun showBudgetExceededNotification(categoryName: String, overAmount: String, budgetId: Long) {
        val notificationId = NOTIFICATION_ID_BUDGET_EXCEEDED + budgetId.toInt()

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to_budget", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BUDGET_ALERTS)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("予算を超過しました")
            .setContentText("$categoryName の予算を $overAmount 超過しています")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$categoryName の予算を $overAmount 超過しています。支出を見直してください。"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.error_500))
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showBudgetNearLimitNotification(categoryName: String, progress: Int, remaining: String, budgetId: Long) {
        val notificationId = NOTIFICATION_ID_BUDGET_NEAR_LIMIT + budgetId.toInt()

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to_budget", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BUDGET_ALERTS)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("予算上限に近づいています")
            .setContentText("$categoryName の予算を ${progress}% 使用しました")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$categoryName の予算を ${progress}% 使用しました（残り $remaining）。支出にご注意ください。"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.warning_500))
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showTotalBudgetExceededNotification(overAmount: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to_budget", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_TOTAL_BUDGET_EXCEEDED,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BUDGET_ALERTS)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("総予算を超過しました")
            .setContentText("月間の総予算を $overAmount 超過しています")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("月間の総予算を $overAmount 超過しています。支出を見直してください。"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.error_500))
            .build()

        notificationManager.notify(NOTIFICATION_ID_TOTAL_BUDGET_EXCEEDED, notification)
    }

    fun showTotalBudgetNearLimitNotification(progress: Int, remaining: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to_budget", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_TOTAL_BUDGET_NEAR_LIMIT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BUDGET_ALERTS)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("総予算の上限に近づいています")
            .setContentText("月間予算の ${progress}% を使用しました")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("月間予算の ${progress}% を使用しました（残り $remaining）。支出にご注意ください。"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.warning_500))
            .build()

        notificationManager.notify(NOTIFICATION_ID_TOTAL_BUDGET_NEAR_LIMIT, notification)
    }

    fun showGoalMilestoneNotification(goalName: String, milestone: Int, currentAmount: String, targetAmount: String) {
        val notificationId = NOTIFICATION_ID_GOAL_ACHIEVED + milestone

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to_goals", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GOALS)
            .setSmallIcon(R.drawable.ic_flag)
            .setContentTitle("目標の進捗 - ${milestone}%達成")
            .setContentText("「$goalName」の${milestone}%を達成しました")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("「$goalName」の${milestone}%を達成しました！\n現在の進捗: $currentAmount / 目標: $targetAmount"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.primary_500))
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}