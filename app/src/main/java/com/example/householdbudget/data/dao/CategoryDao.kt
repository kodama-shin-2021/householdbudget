package com.example.householdbudget.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.data.entity.CategoryType

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): LiveData<List<Category>>
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?
    
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder ASC, name ASC")
    fun getCategoriesByType(type: CategoryType): LiveData<List<Category>>
    
    @Query("SELECT * FROM categories WHERE isDefault = 1 ORDER BY sortOrder ASC")
    fun getDefaultCategories(): LiveData<List<Category>>
    
    @Query("SELECT * FROM categories WHERE isDefault = 0 ORDER BY sortOrder ASC, name ASC")
    fun getCustomCategories(): LiveData<List<Category>>
    
    @Query("SELECT * FROM categories WHERE name LIKE '%' || :name || '%'")
    suspend fun searchCategoriesByName(name: String): List<Category>
    
    @Query("SELECT COUNT(*) FROM categories WHERE name = :name")
    suspend fun getCategoryCountByName(name: String): Int
    
    @Query("SELECT MAX(sortOrder) FROM categories WHERE type = :type")
    suspend fun getMaxSortOrderByType(type: CategoryType): Int?
    
    @Insert
    suspend fun insertCategory(category: Category): Long
    
    @Insert
    suspend fun insertCategories(categories: List<Category>)
    
    @Update
    suspend fun updateCategory(category: Category)
    
    @Delete
    suspend fun deleteCategory(category: Category)
    
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)
    
    @Query("UPDATE categories SET sortOrder = :newOrder WHERE id = :id")
    suspend fun updateCategorySortOrder(id: Long, newOrder: Int)
}