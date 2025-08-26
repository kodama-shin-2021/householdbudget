package com.example.householdbudget.ui.transaction

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.householdbudget.R
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.data.entity.Transaction
import com.example.householdbudget.data.entity.TransactionType
import com.example.householdbudget.databinding.ItemTransactionBinding
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit,
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit,
    private val categoryProvider: (Long) -> Category?,
    private val dateFormatter: (Date) -> String
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale.JAPAN)
    private val expandedItems = mutableSetOf<Long>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            val category = categoryProvider(transaction.categoryId)
            
            binding.apply {
                // Set category info
                tvCategoryName.text = category?.name ?: "不明なカテゴリ"
                
                // Set category icon and color
                category?.let { cat ->
                    ivCategoryIcon.setImageResource(cat.iconResId)
                    try {
                        val color = Color.parseColor(cat.color)
                        ivCategoryIcon.backgroundTintList = ColorStateList.valueOf(color)
                    } catch (e: IllegalArgumentException) {
                        // Use default color if invalid
                    }
                }

                // Set transaction date
                tvTransactionDate.text = dateFormatter(transaction.date)

                // Set subcategory if available (placeholder for now)
                tvSubcategoryName.visibility = View.GONE

                // Set description
                if (!transaction.description.isNullOrBlank()) {
                    tvDescription.text = transaction.description
                    tvDescription.visibility = View.VISIBLE
                } else {
                    tvDescription.visibility = View.GONE
                }

                // Set amount with appropriate color
                val formattedAmount = numberFormat.format(transaction.amount.toDouble())
                tvAmount.text = formattedAmount
                
                val amountColor = when (transaction.type) {
                    TransactionType.INCOME -> itemView.context.getColor(R.color.success_500)
                    TransactionType.EXPENSE -> itemView.context.getColor(R.color.error_500)
                }
                tvAmount.setTextColor(amountColor)

                // Show regular transaction indicator
                if (transaction.regularTransactionId != null) {
                    layoutRegularIndicator.visibility = View.VISIBLE
                } else {
                    layoutRegularIndicator.visibility = View.GONE
                }

                // Handle expand/collapse
                val isExpanded = expandedItems.contains(transaction.id)
                layoutActions.visibility = if (isExpanded) View.VISIBLE else View.GONE

                // Set click listeners
                root.setOnClickListener {
                    onTransactionClick(transaction)
                    toggleExpansion(transaction.id)
                }

                btnEdit.setOnClickListener {
                    onEditClick(transaction)
                }

                btnDelete.setOnClickListener {
                    onDeleteClick(transaction)
                }
            }
        }

        private fun toggleExpansion(transactionId: Long) {
            if (expandedItems.contains(transactionId)) {
                expandedItems.remove(transactionId)
            } else {
                expandedItems.add(transactionId)
            }
            notifyItemChanged(adapterPosition)
        }
    }
}

class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
    override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem == newItem
    }
}