package com.example.householdbudget.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.householdbudget.R
import com.example.householdbudget.databinding.ItemBudgetProgressBinding
import com.example.householdbudget.ui.dashboard.BudgetProgressItem
import java.math.BigDecimal

class BudgetProgressAdapter(
    private val onItemClick: (BudgetProgressItem) -> Unit
) : ListAdapter<BudgetProgressItem, BudgetProgressAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBudgetProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemBudgetProgressBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BudgetProgressItem) {
            binding.apply {
                // Set category icon and color
                ivCategoryIcon.setImageResource(item.categoryIcon)
                try {
                    val colorInt = android.graphics.Color.parseColor(item.categoryColor)
                    ivCategoryIcon.setColorFilter(colorInt)
                } catch (e: IllegalArgumentException) {
                    ivCategoryIcon.setColorFilter(
                        ContextCompat.getColor(root.context, R.color.primary_500)
                    )
                }

                // Set category name
                tvCategoryName.text = item.categoryName

                // Set budget amounts
                tvUsedAmount.text = formatCurrency(item.usedAmount)
                tvBudgetAmount.text = formatCurrency(item.budgetAmount)

                // Set progress
                progressBarBudget.progress = item.progressPercentage
                tvProgressPercentage.text = "${item.progressPercentage}%"

                // Set progress bar color based on budget status
                val progressColor = if (item.isOverBudget) {
                    ContextCompat.getColor(root.context, R.color.error_500)
                } else {
                    ContextCompat.getColor(root.context, R.color.success_500)
                }
                progressBarBudget.progressTintList = 
                    android.content.res.ColorStateList.valueOf(progressColor)

                // Set text color for over budget
                if (item.isOverBudget) {
                    tvUsedAmount.setTextColor(
                        ContextCompat.getColor(root.context, R.color.error_500)
                    )
                    tvProgressPercentage.setTextColor(
                        ContextCompat.getColor(root.context, R.color.error_500)
                    )
                } else {
                    tvUsedAmount.setTextColor(
                        ContextCompat.getColor(root.context, R.color.on_surface)
                    )
                    tvProgressPercentage.setTextColor(
                        ContextCompat.getColor(root.context, R.color.on_surface_variant)
                    )
                }

                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }

        private fun formatCurrency(amount: BigDecimal): String {
            return "¥${String.format("%,.0f", amount.toFloat())}"
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<BudgetProgressItem>() {
        override fun areItemsTheSame(oldItem: BudgetProgressItem, newItem: BudgetProgressItem): Boolean {
            return oldItem.categoryId == newItem.categoryId
        }

        override fun areContentsTheSame(oldItem: BudgetProgressItem, newItem: BudgetProgressItem): Boolean {
            return oldItem == newItem
        }
    }
}