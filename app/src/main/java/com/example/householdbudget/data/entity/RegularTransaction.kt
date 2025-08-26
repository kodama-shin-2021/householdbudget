package com.example.householdbudget.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.util.Date

@Entity(
    tableName = "regular_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Subcategory::class,
            parentColumns = ["id"],
            childColumns = ["subcategoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class RegularTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: BigDecimal,
    val type: TransactionType,
    val categoryId: Long,
    val subcategoryId: Long? = null,
    val description: String? = null,
    val frequency: RecurrenceFrequency,
    val interval: Int = 1, // 間隔（例：2週間毎なら2）
    val dayOfWeek: Int? = null, // 曜日（週次の場合）
    val dayOfMonth: Int? = null, // 月の何日（月次の場合）
    val startDate: Date,
    val endDate: Date? = null,
    val nextOccurrence: Date,
    val currency: String = "JPY",
    val isActive: Boolean = true,
    val autoExecute: Boolean = false, // 自動実行するかどうか
    val notifyBeforeDays: Int = 1, // 何日前に通知するか
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class RecurrenceFrequency {
    DAILY,    // 日次
    WEEKLY,   // 週次
    MONTHLY,  // 月次
    YEARLY    // 年次
}