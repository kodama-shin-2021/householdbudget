package com.example.householdbudget.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.householdbudget.data.entity.CategoryType
import com.example.householdbudget.databinding.FragmentCategoryListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryListFragment : Fragment() {

    private var _binding: FragmentCategoryListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryViewModel by activityViewModels()
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var categoryType: CategoryType

    companion object {
        private const val ARG_CATEGORY_TYPE = "category_type"

        fun newInstance(categoryType: CategoryType): CategoryListFragment {
            return CategoryListFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CATEGORY_TYPE, categoryType)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryType = arguments?.getSerializable(ARG_CATEGORY_TYPE) as? CategoryType
            ?: CategoryType.EXPENSE
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            onCategoryClick = { category ->
                // Handle category click (expand/collapse)
            },
            onEditClick = { category ->
                viewModel.showEditCategoryDialog(category)
            },
            onDeleteClick = { category ->
                viewModel.deleteCategory(category)
            },
            onReorderStart = {
                // Enable drag mode
            }
        )

        binding.recyclerViewCategories.adapter = categoryAdapter
        
        // Setup drag and drop
        val itemTouchHelper = ItemTouchHelper(CategoryDragCallback(categoryAdapter) { categories ->
            viewModel.reorderCategories(categories)
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewCategories)
        categoryAdapter.setItemTouchHelper(itemTouchHelper)
    }

    private fun observeViewModel() {
        viewModel.getCategoriesByType(categoryType).observe(viewLifecycleOwner) { categories ->
            categoryAdapter.submitList(categories)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CategoryUiState.Loading -> {
                    // Show loading state if needed
                }
                is CategoryUiState.Success -> {
                    // Hide loading state
                }
                is CategoryUiState.Empty -> {
                    // Show empty state
                }
                is CategoryUiState.Error -> {
                    // Show error message
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class CategoryDragCallback(
        private val adapter: CategoryAdapter,
        private val onReorder: (List<com.example.householdbudget.data.entity.Category>) -> Unit
    ) : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        0
    ) {
        
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            
            adapter.moveItem(fromPosition, toPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // Not used
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                // Drag ended, save new order
                onReorder(adapter.getCurrentList())
            }
        }

        override fun isLongPressDragEnabled(): Boolean = false
        override fun isItemViewSwipeEnabled(): Boolean = false
    }
}