package com.example.householdbudget.ui.category

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.databinding.ItemCategoryBinding
import java.util.*

class CategoryAdapter(
    private val onCategoryClick: (Category) -> Unit,
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit,
    private val onReorderStart: () -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private var itemTouchHelper: ItemTouchHelper? = null
    private val expandedCategories = mutableSetOf<Long>()

    fun setItemTouchHelper(itemTouchHelper: ItemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val currentList = currentList.toMutableList()
        Collections.swap(currentList, fromPosition, toPosition)
        submitList(currentList)
    }

    fun getCurrentList(): List<Category> = currentList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.apply {
                tvCategoryName.text = category.name
                
                // Set category icon and color
                ivCategoryIcon.setImageResource(category.iconResId)
                try {
                    val color = Color.parseColor(category.color)
                    ivCategoryIcon.backgroundTintList = ColorStateList.valueOf(color)
                } catch (e: IllegalArgumentException) {
                    // Use default color if invalid color string
                }

                // Show/hide subcategory count (placeholder for now)
                tvSubcategoryCount.text = "0個のサブカテゴリ"
                tvSubcategoryCount.visibility = View.VISIBLE

                // Handle budget info (placeholder for now)
                layoutBudgetInfo.visibility = View.GONE
                progressBudget.visibility = View.GONE

                // Handle expansion state
                val isExpanded = expandedCategories.contains(category.id)
                recyclerViewSubcategories.visibility = if (isExpanded) View.VISIBLE else View.GONE
                ivExpandArrow.rotation = if (isExpanded) 180f else 0f
                ivExpandArrow.visibility = View.VISIBLE

                // Set up click listeners
                root.setOnClickListener {
                    onCategoryClick(category)
                    toggleExpansion(category.id)
                }

                ivExpandArrow.setOnClickListener {
                    toggleExpansion(category.id)
                }

                btnMoreOptions.setOnClickListener { view ->
                    showCategoryOptionsMenu(view, category)
                }

                // Set up drag handle
                ivDragHandle.visibility = View.VISIBLE
                ivDragHandle.setOnTouchListener { _, _ ->
                    onReorderStart()
                    itemTouchHelper?.startDrag(this@CategoryViewHolder)
                    true
                }
            }
        }

        private fun toggleExpansion(categoryId: Long) {
            if (expandedCategories.contains(categoryId)) {
                expandedCategories.remove(categoryId)
            } else {
                expandedCategories.add(categoryId)
            }
            notifyItemChanged(adapterPosition)
        }

        private fun showCategoryOptionsMenu(view: View, category: Category) {
            val popup = androidx.appcompat.widget.PopupMenu(view.context, view)
            popup.menuInflater.inflate(com.example.householdbudget.R.menu.menu_category_options, popup.menu)
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    com.example.householdbudget.R.id.action_edit -> {
                        onEditClick(category)
                        true
                    }
                    com.example.householdbudget.R.id.action_delete -> {
                        if (!category.isDefault) {
                            onDeleteClick(category)
                        }
                        true
                    }
                    else -> false
                }
            }
            
            // Disable delete for default categories
            popup.menu.findItem(com.example.householdbudget.R.id.action_delete).isEnabled = !category.isDefault
            popup.show()
        }
    }
}

class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
    override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem == newItem
    }
}