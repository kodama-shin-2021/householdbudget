package com.example.householdbudget.ui.goal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.householdbudget.R
import com.example.householdbudget.data.entity.GoalStatus
import com.example.householdbudget.data.entity.GoalType
import com.example.householdbudget.databinding.ItemGoalBinding
import com.example.householdbudget.ui.goal.GoalItem
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

class GoalAdapter(
    private val onItemClick: (GoalItem) -> Unit,
    private val onMenuClick: (GoalItem) -> Unit
) : ListAdapter<GoalItem, GoalAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGoalBinding.inflate(
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
        private val binding: ItemGoalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GoalItem) {
            binding.apply {
                // Set goal basic info
                tvGoalName.text = item.goal.name
                tvGoalType.text = when (item.goal.type) {
                    GoalType.SAVING -> "貯金目標"
                    GoalType.EXPENSE_REDUCTION -> "支出削減目標"
                }

                // Set goal icon based on type
                val iconRes = when (item.goal.type) {
                    GoalType.SAVING -> R.drawable.ic_savings
                    GoalType.EXPENSE_REDUCTION -> R.drawable.ic_trending_down
                }
                ivGoalIcon.setImageResource(iconRes)

                // Set icon color based on status
                val iconColor = when {
                    item.isAchieved -> ContextCompat.getColor(root.context, R.color.success_500)
                    item.goal.status == GoalStatus.PAUSED -> ContextCompat.getColor(root.context, R.color.warning_500)
                    else -> ContextCompat.getColor(root.context, R.color.primary_500)
                }
                ivGoalIcon.setColorFilter(iconColor)

                // Set amounts
                tvCurrentAmount.text = formatCurrency(item.currentAmount)
                tvTargetAmount.text = formatCurrency(item.goal.targetAmount)
                tvRemainingAmount.text = formatCurrency(item.goal.targetAmount.subtract(item.currentAmount))

                // Set remaining amount color
                val remainingAmount = item.goal.targetAmount.subtract(item.currentAmount)
                if (remainingAmount <= BigDecimal.ZERO) {
                    tvRemainingAmount.setTextColor(ContextCompat.getColor(root.context, R.color.success_500))
                } else {
                    tvRemainingAmount.setTextColor(ContextCompat.getColor(root.context, R.color.on_surface))
                }

                // Set progress
                progressGoal.progress = item.progress
                tvProgressPercentage.text = "${item.progress}%"

                // Set progress bar color
                val progressColor = when {
                    item.isAchieved -> ContextCompat.getColor(root.context, R.color.success_500)
                    item.progress >= 75 -> ContextCompat.getColor(root.context, R.color.success_500)
                    item.progress >= 50 -> ContextCompat.getColor(root.context, R.color.primary_500)
                    item.progress >= 25 -> ContextCompat.getColor(root.context, R.color.warning_500)
                    else -> ContextCompat.getColor(root.context, R.color.error_500)
                }
                progressGoal.setIndicatorColor(progressColor)

                // Set goal status
                when {
                    item.isAchieved -> {
                        tvGoalStatus.text = "達成済み"
                        tvGoalStatus.backgroundTintList = ContextCompat.getColorStateList(root.context, R.color.success_500)
                    }
                    item.goal.status == GoalStatus.PAUSED -> {
                        tvGoalStatus.text = "一時停止"
                        tvGoalStatus.backgroundTintList = ContextCompat.getColorStateList(root.context, R.color.warning_500)
                    }
                    item.isOverdue -> {
                        tvGoalStatus.text = "期限超過"
                        tvGoalStatus.backgroundTintList = ContextCompat.getColorStateList(root.context, R.color.error_500)
                    }
                    else -> {
                        tvGoalStatus.text = "進行中"
                        tvGoalStatus.backgroundTintList = ContextCompat.getColorStateList(root.context, R.color.primary_500)
                    }
                }

                // Set target date
                val dateFormat = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())
                tvGoalDeadline.text = "${dateFormat.format(item.goal.targetDate)}まで"

                // Show achievement badge if achieved
                if (item.isAchieved) {
                    layoutAchievementBadge.visibility = View.VISIBLE
                    tvAchievementMessage.text = "目標達成！おめでとうございます🎉"
                    layoutTimeRemaining.visibility = View.GONE
                } else {
                    layoutAchievementBadge.visibility = View.GONE
                    
                    // Show time remaining for active goals
                    if (item.goal.status == GoalStatus.ACTIVE) {
                        layoutTimeRemaining.visibility = View.VISIBLE
                        tvTimeRemaining.text = when {
                            item.isOverdue -> "${-item.daysRemaining}日経過"
                            item.daysRemaining == 0 -> "今日まで"
                            item.daysRemaining == 1 -> "残り1日"
                            else -> "残り${item.daysRemaining}日"
                        }
                        
                        // Set color based on urgency
                        val timeColor = when {
                            item.isOverdue -> ContextCompat.getColor(root.context, R.color.error_500)
                            item.daysRemaining <= 7 -> ContextCompat.getColor(root.context, R.color.warning_500)
                            else -> ContextCompat.getColor(root.context, R.color.on_surface_variant)
                        }
                        tvTimeRemaining.setTextColor(timeColor)
                    } else {
                        layoutTimeRemaining.visibility = View.GONE
                    }
                }

                // Set click listeners
                root.setOnClickListener {
                    onItemClick(item)
                }

                btnGoalMenu.setOnClickListener {
                    onMenuClick(item)
                }

                // Apply inactive state if goal is paused
                root.alpha = if (item.goal.status == GoalStatus.ACTIVE || item.isAchieved) 1.0f else 0.7f
            }
        }

        private fun formatCurrency(amount: BigDecimal): String {
            return "¥${String.format("%,.0f", amount.toFloat())}"
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<GoalItem>() {
        override fun areItemsTheSame(oldItem: GoalItem, newItem: GoalItem): Boolean {
            return oldItem.goal.id == newItem.goal.id
        }

        override fun areContentsTheSame(oldItem: GoalItem, newItem: GoalItem): Boolean {
            return oldItem == newItem
        }
    }
}