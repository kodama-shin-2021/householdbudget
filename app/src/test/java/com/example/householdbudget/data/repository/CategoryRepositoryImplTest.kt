package com.example.householdbudget.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.example.householdbudget.data.dao.CategoryDao
import com.example.householdbudget.data.dao.SubcategoryDao
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.data.entity.CategoryType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.util.Date

@ExperimentalCoroutinesApi
class CategoryRepositoryImplTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var categoryDao: CategoryDao

    @Mock
    private lateinit var subcategoryDao: SubcategoryDao

    private lateinit var repository: CategoryRepositoryImpl

    private val sampleCategory = Category(
        id = 1L,
        name = "Food",
        iconResId = 0,
        color = "#FF0000",
        type = CategoryType.EXPENSE,
        isDefault = true,
        sortOrder = 1
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = CategoryRepositoryImpl(categoryDao, subcategoryDao)
    }

    @Test
    fun `getAllCategories returns LiveData from DAO`() {
        // Given
        val liveData = MutableLiveData<List<Category>>()
        whenever(categoryDao.getAllCategories()).thenReturn(liveData)

        // When
        val result = repository.getAllCategories()

        // Then
        verify(categoryDao).getAllCategories()
        assert(result == liveData)
    }

    @Test
    fun `getCategoryById returns category from DAO`() = runTest {
        // Given
        val categoryId = 1L
        whenever(categoryDao.getCategoryById(categoryId)).thenReturn(sampleCategory)

        // When
        val result = repository.getCategoryById(categoryId)

        // Then
        verify(categoryDao).getCategoryById(categoryId)
        assert(result == sampleCategory)
    }

    @Test
    fun `insertCategory calls DAO and returns ID`() = runTest {
        // Given
        val expectedId = 1L
        whenever(categoryDao.insertCategory(sampleCategory)).thenReturn(expectedId)

        // When
        val result = repository.insertCategory(sampleCategory)

        // Then
        verify(categoryDao).insertCategory(sampleCategory)
        assert(result == expectedId)
    }

    @Test
    fun `updateCategory calls DAO with updated timestamp`() = runTest {
        // When
        repository.updateCategory(sampleCategory)

        // Then
        verify(categoryDao).updateCategory(argThat { updatedAt != sampleCategory.updatedAt })
    }

    @Test
    fun `deleteCategory calls DAO`() = runTest {
        // When
        repository.deleteCategory(sampleCategory)

        // Then
        verify(categoryDao).deleteCategory(sampleCategory)
    }

    @Test
    fun `getCategoriesByType returns LiveData from DAO`() {
        // Given
        val type = CategoryType.EXPENSE
        val liveData = MutableLiveData<List<Category>>()
        whenever(categoryDao.getCategoriesByType(type)).thenReturn(liveData)

        // When
        val result = repository.getCategoriesByType(type)

        // Then
        verify(categoryDao).getCategoriesByType(type)
        assert(result == liveData)
    }

    @Test
    fun `reorderCategories updates sort order for all categories`() = runTest {
        // Given
        val categories = listOf(
            sampleCategory.copy(id = 1L),
            sampleCategory.copy(id = 2L),
            sampleCategory.copy(id = 3L)
        )

        // When
        repository.reorderCategories(categories)

        // Then
        verify(categoryDao).updateCategorySortOrder(1L, 0)
        verify(categoryDao).updateCategorySortOrder(2L, 1)
        verify(categoryDao).updateCategorySortOrder(3L, 2)
    }

    @Test
    fun `initializeDefaultCategories inserts income and expense categories`() = runTest {
        // Given
        whenever(categoryDao.insertCategory(any())).thenReturn(1L, 2L, 3L)

        // When
        repository.initializeDefaultCategories()

        // Then
        verify(categoryDao).insertCategories(any())
        verify(categoryDao, atLeastOnce()).insertCategory(any())
        verify(subcategoryDao, atLeastOnce()).insertSubcategories(any())
    }
}