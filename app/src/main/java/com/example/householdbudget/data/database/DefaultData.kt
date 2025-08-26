package com.example.householdbudget.data.database

import com.example.householdbudget.R
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.data.entity.CategoryType
import com.example.householdbudget.data.entity.Settings
import com.example.householdbudget.data.entity.SettingType
import com.example.householdbudget.data.entity.Subcategory

object DefaultData {
    
    fun getDefaultIncomeCategories(): List<Category> {
        return listOf(
            Category(
                name = "給与",
                iconResId = R.drawable.ic_work,
                color = "#4CAF50",
                type = CategoryType.INCOME,
                isDefault = true,
                sortOrder = 1
            ),
            Category(
                name = "副業",
                iconResId = R.drawable.ic_business,
                color = "#8BC34A",
                type = CategoryType.INCOME,
                isDefault = true,
                sortOrder = 2
            ),
            Category(
                name = "投資・資産運用",
                iconResId = R.drawable.ic_trending_up,
                color = "#CDDC39",
                type = CategoryType.INCOME,
                isDefault = true,
                sortOrder = 3
            ),
            Category(
                name = "その他収入",
                iconResId = R.drawable.ic_attach_money,
                color = "#66BB6A",
                type = CategoryType.INCOME,
                isDefault = true,
                sortOrder = 4
            )
        )
    }
    
    fun getDefaultExpenseCategories(): List<Category> {
        return listOf(
            Category(
                name = "食費",
                iconResId = R.drawable.ic_restaurant,
                color = "#FF9800",
                type = CategoryType.EXPENSE,
                isDefault = true,
                sortOrder = 1
            ),
            Category(
                name = "住居費",
                iconResId = R.drawable.ic_home,
                color = "#795548",
                type = CategoryType.EXPENSE,
                isDefault = true,
                sortOrder = 2
            ),
            Category(
                name = "交通費",
                iconResId = R.drawable.ic_directions_car,
                color = "#2196F3",
                type = CategoryType.EXPENSE,
                isDefault = true,
                sortOrder = 3
            ),
            Category(
                name = "光熱費",
                iconResId = R.drawable.ic_flash_on,
                color = "#FFC107",
                type = CategoryType.EXPENSE,
                isDefault = true,
                sortOrder = 4
            ),
            Category(
                name = "通信費",
                iconResId = R.drawable.ic_phone,
                color = "#9C27B0",
                type = CategoryType.EXPENSE,
                isDefault = true,
                sortOrder = 5
            ),
            Category(
                name = "医療費",
                iconResId = R.drawable.ic_local_hospital,
                color = "#F44336",
                type = CategoryType.EXPENSE,
                isDefault = true,
                sortOrder = 6
            ),
            Category(
                name = "娯楽費",
                iconResId = R.drawable.ic_movie,
                color = "#E91E63",
                type = CategoryType.EXPENSE,
                isDefault = true,
                sortOrder = 7
            ),
            Category(
                name = "衣服・美容",
                iconResId = R.drawable.ic_shopping_bag,
                color = "#9E9E9E",
                type = CategoryType.EXPENSE,
                isDefault = true,
                sortOrder = 8
            ),
            Category(
                name = "教育費",
                iconResId = R.drawable.ic_school,
                color = "#3F51B5",
                type = CategoryType.EXPENSE,
                isDefault = true,
                sortOrder = 9
            ),
            Category(
                name = "その他支出",
                iconResId = R.drawable.ic_more_horiz,
                color = "#607D8B",
                type = CategoryType.EXPENSE,
                isDefault = true,
                sortOrder = 10
            )
        )
    }
    
    fun getDefaultSubcategories(categoryId: Long, categoryName: String): List<Subcategory> {
        return when (categoryName) {
            "食費" -> listOf(
                Subcategory(categoryId = categoryId, name = "外食", sortOrder = 1),
                Subcategory(categoryId = categoryId, name = "食材", sortOrder = 2),
                Subcategory(categoryId = categoryId, name = "飲料", sortOrder = 3),
                Subcategory(categoryId = categoryId, name = "お菓子・嗜好品", sortOrder = 4)
            )
            "住居費" -> listOf(
                Subcategory(categoryId = categoryId, name = "家賃・住宅ローン", sortOrder = 1),
                Subcategory(categoryId = categoryId, name = "修繕・リフォーム", sortOrder = 2),
                Subcategory(categoryId = categoryId, name = "家具・インテリア", sortOrder = 3)
            )
            "交通費" -> listOf(
                Subcategory(categoryId = categoryId, name = "電車・バス", sortOrder = 1),
                Subcategory(categoryId = categoryId, name = "ガソリン", sortOrder = 2),
                Subcategory(categoryId = categoryId, name = "タクシー", sortOrder = 3),
                Subcategory(categoryId = categoryId, name = "駐車場代", sortOrder = 4)
            )
            "光熱費" -> listOf(
                Subcategory(categoryId = categoryId, name = "電気代", sortOrder = 1),
                Subcategory(categoryId = categoryId, name = "ガス代", sortOrder = 2),
                Subcategory(categoryId = categoryId, name = "水道代", sortOrder = 3)
            )
            "通信費" -> listOf(
                Subcategory(categoryId = categoryId, name = "携帯電話", sortOrder = 1),
                Subcategory(categoryId = categoryId, name = "インターネット", sortOrder = 2),
                Subcategory(categoryId = categoryId, name = "固定電話", sortOrder = 3)
            )
            "娯楽費" -> listOf(
                Subcategory(categoryId = categoryId, name = "映画・エンタメ", sortOrder = 1),
                Subcategory(categoryId = categoryId, name = "旅行", sortOrder = 2),
                Subcategory(categoryId = categoryId, name = "本・雑誌", sortOrder = 3),
                Subcategory(categoryId = categoryId, name = "ゲーム・アプリ", sortOrder = 4)
            )
            else -> emptyList()
        }
    }
    
    fun getDefaultSettings(): List<Settings> {
        return listOf(
            Settings(
                key = "default_currency",
                value = "JPY",
                type = SettingType.STRING
            ),
            Settings(
                key = "budget_alert_threshold",
                value = "0.8",
                type = SettingType.FLOAT
            ),
            Settings(
                key = "enable_notifications",
                value = "true",
                type = SettingType.BOOLEAN
            ),
            Settings(
                key = "notification_time",
                value = "20:00",
                type = SettingType.STRING
            ),
            Settings(
                key = "first_day_of_week",
                value = "1", // Monday
                type = SettingType.INT
            ),
            Settings(
                key = "enable_dark_mode",
                value = "false",
                type = SettingType.BOOLEAN
            )
        )
    }
}