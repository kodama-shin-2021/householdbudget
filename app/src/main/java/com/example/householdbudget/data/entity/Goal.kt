package com.example.householdbudget.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.util.Date

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val type: GoalType,
    val targetAmount: BigDecimal,
    val currentAmount: BigDecimal = BigDecimal.ZERO,
    val targetDate: Date? = null,
    val currency: String = "JPY",
    val isAchieved: Boolean = false,
    val achievedAt: Date? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class GoalType {
    SAVINGS,          // 貯金目標
    EXPENSE_REDUCTION // 支出削減目標
}