package com.example.householdbudget.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.util.Date

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Subcategory::class,
            parentColumns = ["id"],
            childColumns = ["subcategoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val subcategoryId: Long? = null,
    val budgetAmount: BigDecimal,
    val period: BudgetPeriod,
    val startDate: Date,
    val endDate: Date? = null,
    val currency: String = "JPY",
    val alertThreshold: Float = 0.8f, // 80%で警告
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class BudgetPeriod {
    WEEKLY,   // 週次
    MONTHLY,  // 月次
    YEARLY,   // 年次
    CUSTOM    // カスタム期間
}