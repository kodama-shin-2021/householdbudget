package com.example.householdbudget.ui.budget.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.householdbudget.R
import com.example.householdbudget.databinding.ItemCategoryBudgetBinding
import com.example.householdbudget.ui.budget.BudgetPeriod
import com.example.householdbudget.ui.budget.CategoryBudgetItem
import java.math.BigDecimal

class CategoryBudgetAdapter(
    private val onItemClick: (CategoryBudgetItem) -> Unit,
    private val onMenuClick: (CategoryBudgetItem) -> Unit
) : ListAdapter<CategoryBudgetItem, CategoryBudgetAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryBudgetBinding.inflate(
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
        private val binding: ItemCategoryBudgetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryBudgetItem) {
            binding.apply {
                // Set category info
                tvCategoryName.text = item.category.name
                tvBudgetPeriod.text = when (item.budget.period) {
                    "WEEKLY" -> "週間予算"
                    "MONTHLY" -> "月間予算"
                    "YEARLY" -> "年間予算"
                    else -> "月間予算"
                }

                // Set category icon
                ivCategoryIcon.setImageResource(item.category.iconResId)
                try {
                    val colorInt = android.graphics.Color.parseColor(item.category.color)
                    ivCategoryIcon.setColorFilter(colorInt)
                } catch (e: IllegalArgumentException) {
                    ivCategoryIcon.setColorFilter(
                        ContextCompat.getColor(root.context, R.color.primary_500)
                    )
                }

                // Set budget amounts
                tvUsedAmount.text = formatCurrency(item.usedAmount)
                tvBudgetAmount.text = formatCurrency(item.budget.budgetAmount)
                tvRemainingAmount.text = formatCurrency(item.remainingAmount)

                // Set progress
                progressBudget.progress = item.progress.coerceAtMost(100)
                tvProgressPercentage.text = "${item.progress}%"

                // Set colors based on budget status
                when {
                    item.isOverBudget -> {
                        val errorColor = ContextCompat.getColor(root.context, R.color.error_500)
                        tvUsedAmount.setTextColor(errorColor)
                        tvProgressPercentage.setTextColor(errorColor)
                        progressBudget.setIndicatorColor(errorColor)
                        
                        // Show warning
                        layoutWarning.visibility = View.VISIBLE
                        tvWarningMessage.text = "予算を超過しています"
                        tvWarningMessage.setTextColor(
                            ContextCompat.getColor(root.context, R.color.error_700)
                        )
                    }
                    item.isNearLimit -> {
                        val warningColor = ContextCompat.getColor(root.context, R.color.warning_500)
                        tvUsedAmount.setTextColor(warningColor)
                        tvProgressPercentage.setTextColor(warningColor)
                        progressBudget.setIndicatorColor(warningColor)
                        
                        // Show warning
                        layoutWarning.visibility = View.VISIBLE
                        tvWarningMessage.text = "予算上限に近づいています"
                        tvWarningMessage.setTextColor(
                            ContextCompat.getColor(root.context, R.color.warning_700)
                        )
                    }
                    else -> {
                        val normalColor = ContextCompat.getColor(root.context, R.color.success_500)
                        tvUsedAmount.setTextColor(
                            ContextCompat.getColor(root.context, R.color.on_surface)
                        )
                        tvProgressPercentage.setTextColor(
                            ContextCompat.getColor(root.context, R.color.on_surface_variant)
                        )
                        progressBudget.setIndicatorColor(normalColor)
                        
                        // Hide warning
                        layoutWarning.visibility = View.GONE
                    }
                }

                // Set remaining amount color
                if (item.remainingAmount < BigDecimal.ZERO) {
                    tvRemainingAmount.setTextColor(
                        ContextCompat.getColor(root.context, R.color.error_500)
                    )
                } else {
                    tvRemainingAmount.setTextColor(
                        ContextCompat.getColor(root.context, R.color.on_surface)
                    )
                }

                // Set click listeners
                root.setOnClickListener {
                    onItemClick(item)
                }

                btnMenu.setOnClickListener {
                    onMenuClick(item)
                }

                // Apply inactive state if budget is disabled
                root.alpha = if (item.budget.isActive) 1.0f else 0.6f
            }
        }

        private fun formatCurrency(amount: BigDecimal): String {
            return "¥${String.format("%,.0f", amount.toFloat())}"
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CategoryBudgetItem>() {
        override fun areItemsTheSame(oldItem: CategoryBudgetItem, newItem: CategoryBudgetItem): Boolean {
            return oldItem.budget.id == newItem.budget.id
        }

        override fun areContentsTheSame(oldItem: CategoryBudgetItem, newItem: CategoryBudgetItem): Boolean {
            return oldItem == newItem
        }
    }
}