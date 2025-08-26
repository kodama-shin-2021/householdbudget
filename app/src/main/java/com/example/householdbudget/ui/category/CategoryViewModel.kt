package com.example.householdbudget.ui.category

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.data.entity.CategoryType
import com.example.householdbudget.data.entity.Subcategory
import com.example.householdbudget.domain.repository.CategoryRepository
import com.example.householdbudget.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<CategoryUiState>(CategoryUiState.Loading)
    val uiState: LiveData<CategoryUiState> = _uiState

    private val _selectedCategoryType = MutableLiveData<CategoryType>(CategoryType.EXPENSE)
    val selectedCategoryType: LiveData<CategoryType> = _selectedCategoryType

    private val _showEditDialog = MutableLiveData<CategoryEditDialogState?>()
    val showEditDialog: LiveData<CategoryEditDialogState?> = _showEditDialog

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _subcategories = MutableLiveData<Map<Long, List<Subcategory>>>()
    val subcategories: LiveData<Map<Long, List<Subcategory>>> = _subcategories

    init {
        loadCategories()
        initializeDefaultCategoriesIfNeeded()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                _uiState.value = CategoryUiState.Loading
                categoryRepository.getAllCategories().observeForever { categories ->
                    _categories.value = categories
                    if (categories.isNotEmpty()) {
                        _uiState.value = CategoryUiState.Success
                    } else {
                        _uiState.value = CategoryUiState.Empty
                    }
                }
            } catch (e: Exception) {
                _uiState.value = CategoryUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    private fun initializeDefaultCategoriesIfNeeded() {
        viewModelScope.launch {
            try {
                val existingCategories = categoryRepository.getAllCategories().value
                if (existingCategories.isNullOrEmpty()) {
                    categoryRepository.initializeDefaultCategories()
                }
            } catch (e: Exception) {
                // Log error but don't show to user as this is background initialization
            }
        }
    }

    fun setCategoryType(type: CategoryType) {
        _selectedCategoryType.value = type
    }

    fun getCategoriesByType(type: CategoryType): LiveData<List<Category>> {
        return categoryRepository.getCategoriesByType(type)
    }

    fun showAddCategoryDialog(type: CategoryType) {
        _showEditDialog.value = CategoryEditDialogState.Add(type)
    }

    fun showEditCategoryDialog(category: Category) {
        _showEditDialog.value = CategoryEditDialogState.Edit(category)
    }

    fun dismissEditDialog() {
        _showEditDialog.value = null
    }

    fun saveCategory(
        categoryId: Long? = null,
        name: String,
        type: CategoryType,
        iconResId: Int,
        color: String
    ) {
        if (name.isBlank()) {
            _uiState.value = CategoryUiState.Error("カテゴリ名を入力してください")
            return
        }

        viewModelScope.launch {
            try {
                if (categoryId == null) {
                    // Add new category
                    val sortOrder = getNextSortOrder(type)
                    val category = Category(
                        name = name.trim(),
                        iconResId = iconResId,
                        color = color,
                        type = type,
                        isDefault = false,
                        sortOrder = sortOrder
                    )
                    categoryRepository.insertCategory(category)
                } else {
                    // Update existing category
                    val existingCategory = categoryRepository.getCategoryById(categoryId)
                    if (existingCategory != null) {
                        val updatedCategory = existingCategory.copy(
                            name = name.trim(),
                            iconResId = iconResId,
                            color = color,
                            updatedAt = Date()
                        )
                        categoryRepository.updateCategory(updatedCategory)
                    }
                }
                dismissEditDialog()
                _uiState.value = CategoryUiState.Success
            } catch (e: Exception) {
                _uiState.value = CategoryUiState.Error(e.message ?: "カテゴリの保存に失敗しました")
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                // Check if category is used in transactions
                // For now, just delete - in real app would check constraints
                categoryRepository.deleteCategory(category)
                _uiState.value = CategoryUiState.Success
            } catch (e: Exception) {
                _uiState.value = CategoryUiState.Error("カテゴリの削除に失敗しました")
            }
        }
    }

    fun reorderCategories(categories: List<Category>) {
        viewModelScope.launch {
            try {
                categoryRepository.reorderCategories(categories)
            } catch (e: Exception) {
                _uiState.value = CategoryUiState.Error("並び順の更新に失敗しました")
            }
        }
    }

    private suspend fun getNextSortOrder(type: CategoryType): Int {
        val categories = categoryRepository.getCategoriesByType(type).value ?: emptyList()
        return categories.maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
    }

    fun searchCategories(query: String) {
        viewModelScope.launch {
            try {
                if (query.isBlank()) {
                    loadCategories()
                } else {
                    val results = categoryRepository.searchCategoriesByName(query)
                    _categories.value = results
                    _uiState.value = if (results.isEmpty()) {
                        CategoryUiState.Empty
                    } else {
                        CategoryUiState.Success
                    }
                }
            } catch (e: Exception) {
                _uiState.value = CategoryUiState.Error("検索に失敗しました")
            }
        }
    }
}

sealed class CategoryUiState {
    object Loading : CategoryUiState()
    object Success : CategoryUiState()
    object Empty : CategoryUiState()
    data class Error(val message: String) : CategoryUiState()
}

sealed class CategoryEditDialogState {
    data class Add(val type: CategoryType) : CategoryEditDialogState()
    data class Edit(val category: Category) : CategoryEditDialogState()
}