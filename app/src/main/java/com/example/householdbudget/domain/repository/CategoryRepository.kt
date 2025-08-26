package com.example.householdbudget.domain.repository

import androidx.lifecycle.LiveData
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.data.entity.CategoryType

interface CategoryRepository {
    
    fun getAllCategories(): LiveData<List<Category>>
    
    suspend fun getCategoryById(id: Long): Category?
    
    fun getCategoriesByType(type: CategoryType): LiveData<List<Category>>
    
    fun getDefaultCategories(): LiveData<List<Category>>
    
    fun getCustomCategories(): LiveData<List<Category>>
    
    suspend fun searchCategoriesByName(name: String): List<Category>
    
    suspend fun getCategoryCountByName(name: String): Int
    
    suspend fun insertCategory(category: Category): Long
    
    suspend fun insertCategories(categories: List<Category>)
    
    suspend fun updateCategory(category: Category)
    
    suspend fun deleteCategory(category: Category)
    
    suspend fun deleteCategoryById(id: Long)
    
    suspend fun updateCategorySortOrder(id: Long, newOrder: Int)
    
    suspend fun reorderCategories(categories: List<Category>)
    
    suspend fun initializeDefaultCategories()
}