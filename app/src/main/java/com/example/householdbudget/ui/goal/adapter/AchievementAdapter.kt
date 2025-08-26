package com.example.householdbudget.ui.goal.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.householdbudget.databinding.ItemAchievementBinding
import com.example.householdbudget.ui.goal.Achievement
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

class AchievementAdapter(
    private val onItemClick: (Achievement) -> Unit
) : ListAdapter<Achievement, AchievementAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAchievementBinding.inflate(
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
        private val binding: ItemAchievementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Achievement) {
            binding.apply {
                tvAchievementGoalName.text = item.goalName
                tvAchievementAmount.text = formatCurrency(item.targetAmount)
                
                val dateFormat = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())
                tvAchievementDate.text = "${dateFormat.format(item.achievedDate)}に達成"

                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }

        private fun formatCurrency(amount: BigDecimal): String {
            return "¥${String.format("%,.0f", amount.toFloat())}"
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Achievement>() {
        override fun areItemsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
            return oldItem.goalName == newItem.goalName && oldItem.achievedDate == newItem.achievedDate
        }

        override fun areContentsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
            return oldItem == newItem
        }
    }
}