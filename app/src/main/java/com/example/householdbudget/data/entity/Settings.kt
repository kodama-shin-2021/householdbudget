package com.example.householdbudget.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey
    val key: String,
    val value: String,
    val type: SettingType,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class SettingType {
    STRING,
    INT,
    BOOLEAN,
    FLOAT
}