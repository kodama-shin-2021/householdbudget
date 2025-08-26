package com.example.householdbudget.data.repository

import androidx.lifecycle.LiveData
import com.example.householdbudget.data.dao.CategoryDao
import com.example.householdbudget.data.dao.SubcategoryDao
import com.example.householdbudget.data.database.DefaultData
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.data.entity.CategoryType
import com.example.householdbudget.domain.repository.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val subcategoryDao: SubcategoryDao
) : CategoryRepository {

    override fun getAllCategories(): LiveData<List<Category>> {
        return categoryDao.getAllCategories()
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return withContext(Dispatchers.IO) {
            categoryDao.getCategoryById(id)
        }
    }

    override fun getCategoriesByType(type: CategoryType): LiveData<List<Category>> {
        return categoryDao.getCategoriesByType(type)
    }

    override fun getDefaultCategories(): LiveData<List<Category>> {
        return categoryDao.getDefaultCategories()
    }

    override fun getCustomCategories(): LiveData<List<Category>> {
        return categoryDao.getCustomCategories()
    }

    override suspend fun searchCategoriesByName(name: String): List<Category> {
        return withContext(Dispatchers.IO) {
            categoryDao.searchCategoriesByName(name)
        }
    }

    override suspend fun getCategoryCountByName(name: String): Int {
        return withContext(Dispatchers.IO) {
            categoryDao.getCategoryCountByName(name)
        }
    }

    override suspend fun insertCategory(category: Category): Long {
        return withContext(Dispatchers.IO) {
            categoryDao.insertCategory(category)
        }
    }

    override suspend fun insertCategories(categories: List<Category>) {
        withContext(Dispatchers.IO) {
            categoryDao.insertCategories(categories)
        }
    }

    override suspend fun updateCategory(category: Category) {
        withContext(Dispatchers.IO) {
            categoryDao.updateCategory(category.copy(updatedAt = Date()))
        }
    }

    override suspend fun deleteCategory(category: Category) {
        withContext(Dispatchers.IO) {
            categoryDao.deleteCategory(category)
        }
    }

    override suspend fun deleteCategoryById(id: Long) {
        withContext(Dispatchers.IO) {
            categoryDao.deleteCategoryById(id)
        }
    }

    override suspend fun updateCategorySortOrder(id: Long, newOrder: Int) {
        withContext(Dispatchers.IO) {
            categoryDao.updateCategorySortOrder(id, newOrder)
        }
    }

    override suspend fun reorderCategories(categories: List<Category>) {
        withContext(Dispatchers.IO) {
            categories.forEachIndexed { index, category ->
                categoryDao.updateCategorySortOrder(category.id, index)
            }
        }
    }

    override suspend fun initializeDefaultCategories() {
        withContext(Dispatchers.IO) {
            // 収入カテゴリの初期化
            val incomeCategories = DefaultData.getDefaultIncomeCategories()
            categoryDao.insertCategories(incomeCategories)

            // 支出カテゴリの初期化
            val expenseCategories = DefaultData.getDefaultExpenseCategories()
            val insertedCategoryIds = mutableMapOf<String, Long>()
            
            expenseCategories.forEach { category ->
                val categoryId = categoryDao.insertCategory(category)
                insertedCategoryIds[category.name] = categoryId
                
                // サブカテゴリの挿入
                val subcategories = DefaultData.getDefaultSubcategories(categoryId, category.name)
                if (subcategories.isNotEmpty()) {
                    subcategoryDao.insertSubcategories(subcategories)
                }
            }
        }
    }
}