package com.example.householdbudget.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.util.Date

@Entity(
    tableName = "transactions",
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
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: BigDecimal,
    val type: TransactionType,
    val categoryId: Long,
    val subcategoryId: Long? = null,
    val description: String? = null,
    val date: Date,
    val currency: String = "JPY",
    val location: String? = null,
    val tags: String? = null, // JSON array of tags
    val regularTransactionId: Long? = null, // 定期取引からの場合
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class TransactionType {
    INCOME,    // 収入
    EXPENSE    // 支出
}