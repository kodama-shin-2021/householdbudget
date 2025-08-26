package com.example.householdbudget.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconResId: Int,
    val color: String,
    val type: CategoryType,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class CategoryType {
    INCOME,    // 収入
    EXPENSE    // 支出
}