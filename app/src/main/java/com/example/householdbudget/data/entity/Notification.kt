package com.example.householdbudget.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val type: NotificationType,
    val relatedEntityId: Long? = null, // 関連するエンティティのID
    val isRead: Boolean = false,
    val scheduledAt: Date? = null,
    val createdAt: Date = Date(),
    val readAt: Date? = null
)

enum class NotificationType {
    BUDGET_ALERT,        // 予算警告
    REGULAR_TRANSACTION, // 定期取引リマインダー
    GOAL_PROGRESS,       // 目標進捗通知
    MONTHLY_SUMMARY      // 月間サマリー
}