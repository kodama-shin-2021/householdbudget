package com.example.householdbudget.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.householdbudget.data.entity.Subcategory

@Dao
interface SubcategoryDao {
    
    @Query("SELECT * FROM subcategories ORDER BY sortOrder ASC, name ASC")
    fun getAllSubcategories(): LiveData<List<Subcategory>>
    
    @Query("SELECT * FROM subcategories WHERE id = :id")
    suspend fun getSubcategoryById(id: Long): Subcategory?
    
    @Query("SELECT * FROM subcategories WHERE categoryId = :categoryId ORDER BY sortOrder ASC, name ASC")
    fun getSubcategoriesByCategory(categoryId: Long): LiveData<List<Subcategory>>
    
    @Query("SELECT * FROM subcategories WHERE name LIKE '%' || :name || '%'")
    suspend fun searchSubcategoriesByName(name: String): List<Subcategory>
    
    @Query("SELECT COUNT(*) FROM subcategories WHERE categoryId = :categoryId AND name = :name")
    suspend fun getSubcategoryCountByName(categoryId: Long, name: String): Int
    
    @Query("SELECT MAX(sortOrder) FROM subcategories WHERE categoryId = :categoryId")
    suspend fun getMaxSortOrderByCategory(categoryId: Long): Int?
    
    @Insert
    suspend fun insertSubcategory(subcategory: Subcategory): Long
    
    @Insert
    suspend fun insertSubcategories(subcategories: List<Subcategory>)
    
    @Update
    suspend fun updateSubcategory(subcategory: Subcategory)
    
    @Delete
    suspend fun deleteSubcategory(subcategory: Subcategory)
    
    @Query("DELETE FROM subcategories WHERE id = :id")
    suspend fun deleteSubcategoryById(id: Long)
    
    @Query("DELETE FROM subcategories WHERE categoryId = :categoryId")
    suspend fun deleteSubcategoriesByCategory(categoryId: Long)
    
    @Query("UPDATE subcategories SET sortOrder = :newOrder WHERE id = :id")
    suspend fun updateSubcategorySortOrder(id: Long, newOrder: Int)
}