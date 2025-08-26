package com.example.householdbudget.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.householdbudget.R
import com.example.householdbudget.data.entity.Transaction
import com.example.householdbudget.data.entity.TransactionType
import com.example.householdbudget.databinding.ItemRecentTransactionBinding
import java.text.SimpleDateFormat
import java.util.Locale

class RecentTransactionAdapter(
    private val onItemClick: (Transaction) -> Unit,
    private val getCategoryName: (Long) -> String? = { null },
    private val getCategoryIcon: (Long) -> Int? = { null },
    private val getCategoryColor: (Long) -> String? = { null }
) : ListAdapter<Transaction, RecentTransactionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentTransactionBinding.inflate(
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
        private val binding: ItemRecentTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.apply {
                // Set category name
                val categoryName = getCategoryName(transaction.categoryId) ?: "不明なカテゴリ"
                tvCategoryName.text = categoryName

                // Set description
                if (transaction.description.isNullOrBlank()) {
                    tvDescription.visibility = View.GONE
                } else {
                    tvDescription.visibility = View.VISIBLE
                    tvDescription.text = transaction.description
                }

                // Set transaction date
                val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
                tvTransactionDate.text = dateFormat.format(transaction.date)

                // Set amount with color
                val amountText = formatCurrency(transaction.amount)
                tvAmount.text = amountText
                
                val amountColor = when (transaction.type) {
                    TransactionType.INCOME -> ContextCompat.getColor(root.context, R.color.success_500)
                    TransactionType.EXPENSE -> ContextCompat.getColor(root.context, R.color.error_500)
                }
                tvAmount.setTextColor(amountColor)

                // Set category icon and color
                val iconResId = getCategoryIcon(transaction.categoryId)
                if (iconResId != null) {
                    ivCategoryIcon.setImageResource(iconResId)
                } else {
                    // Default icon based on transaction type
                    val defaultIcon = when (transaction.type) {
                        TransactionType.INCOME -> R.drawable.ic_add
                        TransactionType.EXPENSE -> R.drawable.ic_remove
                    }
                    ivCategoryIcon.setImageResource(defaultIcon)
                }

                // Set icon color
                val categoryColor = getCategoryColor(transaction.categoryId)
                if (categoryColor != null) {
                    try {
                        val colorInt = android.graphics.Color.parseColor(categoryColor)
                        ivCategoryIcon.setColorFilter(colorInt)
                    } catch (e: IllegalArgumentException) {
                        setDefaultIconColor(transaction.type)
                    }
                } else {
                    setDefaultIconColor(transaction.type)
                }

                root.setOnClickListener {
                    onItemClick(transaction)
                }
            }
        }

        private fun setDefaultIconColor(type: TransactionType) {
            val defaultColor = when (type) {
                TransactionType.INCOME -> ContextCompat.getColor(binding.root.context, R.color.success_500)
                TransactionType.EXPENSE -> ContextCompat.getColor(binding.root.context, R.color.error_500)
            }
            binding.ivCategoryIcon.setColorFilter(defaultColor)
        }

        private fun formatCurrency(amount: java.math.BigDecimal): String {
            return "¥${String.format("%,.0f", amount.toFloat())}"
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}