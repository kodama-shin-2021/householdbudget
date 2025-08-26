package com.example.householdbudget.ui.category

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.householdbudget.data.entity.Budget
import com.example.householdbudget.data.entity.BudgetPeriod
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.databinding.DialogCategoryBudgetBinding
import com.example.householdbudget.domain.repository.BudgetRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class CategoryBudgetDialogFragment : DialogFragment() {

    private var _binding: DialogCategoryBudgetBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var budgetRepository: BudgetRepository

    private lateinit var category: Category
    private var existingBudget: Budget? = null

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: Category): CategoryBudgetDialogFragment {
            return CategoryBudgetDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CATEGORY, category)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = arguments?.getSerializable(ARG_CATEGORY) as Category
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCategoryBudgetBinding.inflate(layoutInflater)
        
        setupViews()
        loadExistingBudget()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun setupViews() {
        // Display category info
        binding.ivCategoryIcon.setImageResource(category.iconResId)
        try {
            val color = Color.parseColor(category.color)
            binding.ivCategoryIcon.backgroundTintList = ColorStateList.valueOf(color)
        } catch (e: IllegalArgumentException) {
            // Use default color
        }
        binding.tvCategoryName.text = category.name

        // Setup alert threshold slider
        binding.sliderAlertThreshold.addOnChangeListener { _, value, _ ->
            binding.tvAlertThresholdValue.text = "${value.toInt()}%"
        }
        binding.tvAlertThresholdValue.text = "${binding.sliderAlertThreshold.value.toInt()}%"

        // Setup buttons
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveBudget()
        }
    }

    private fun loadExistingBudget() {
        lifecycleScope.launch {
            try {
                existingBudget = budgetRepository.getCurrentBudgetForCategory(
                    category.id,
                    Date()
                )
                
                existingBudget?.let { budget ->
                    // Pre-fill existing budget data
                    binding.etBudgetAmount.setText(budget.budgetAmount.toString())
                    
                    when (budget.period) {
                        BudgetPeriod.WEEKLY -> binding.chipWeekly.isChecked = true
                        BudgetPeriod.MONTHLY -> binding.chipMonthly.isChecked = true
                        BudgetPeriod.YEARLY -> binding.chipYearly.isChecked = true
                        BudgetPeriod.CUSTOM -> binding.chipMonthly.isChecked = true // Default fallback
                    }
                    
                    binding.sliderAlertThreshold.value = (budget.alertThreshold * 100)
                    binding.tvAlertThresholdValue.text = "${(budget.alertThreshold * 100).toInt()}%"
                    
                    // Show current budget usage
                    showCurrentBudgetUsage(budget)
                }
            } catch (e: Exception) {
                // Handle error - maybe show toast
            }
        }
    }

    private fun showCurrentBudgetUsage(budget: Budget) {
        lifecycleScope.launch {
            try {
                val usage = budgetRepository.getBudgetUsage(budget.id)
                val progress = budgetRepository.getBudgetProgress(budget.id)
                
                binding.cardCurrentBudget.visibility = View.VISIBLE
                binding.tvUsedAmount.text = "¥${String.format("%,.0f", usage.toFloat())}"
                binding.progressCurrentBudget.progress = (progress * 100).toInt()
                
                // Change color if over threshold
                if (progress >= budget.alertThreshold) {
                    binding.progressCurrentBudget.setIndicatorColor(
                        requireContext().getColor(android.R.color.holo_red_light)
                    )
                }
            } catch (e: Exception) {
                // Handle error
                binding.cardCurrentBudget.visibility = View.GONE
            }
        }
    }

    private fun saveBudget() {
        val amountText = binding.etBudgetAmount.text.toString()
        if (amountText.isBlank()) {
            binding.etBudgetAmount.error = "金額を入力してください"
            return
        }

        val amount = try {
            BigDecimal(amountText)
        } catch (e: NumberFormatException) {
            binding.etBudgetAmount.error = "有効な金額を入力してください"
            return
        }

        if (amount <= BigDecimal.ZERO) {
            binding.etBudgetAmount.error = "金額は0より大きい値を入力してください"
            return
        }

        val period = when {
            binding.chipWeekly.isChecked -> BudgetPeriod.WEEKLY
            binding.chipMonthly.isChecked -> BudgetPeriod.MONTHLY
            binding.chipYearly.isChecked -> BudgetPeriod.YEARLY
            else -> BudgetPeriod.MONTHLY
        }

        val alertThreshold = binding.sliderAlertThreshold.value / 100f

        lifecycleScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val startDate = calendar.time
                
                // Calculate end date based on period
                when (period) {
                    BudgetPeriod.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    BudgetPeriod.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                    BudgetPeriod.YEARLY -> calendar.add(Calendar.YEAR, 1)
                    BudgetPeriod.CUSTOM -> calendar.add(Calendar.MONTH, 1) // Default fallback
                }
                val endDate = calendar.time

                val budget = existingBudget?.copy(
                    budgetAmount = amount,
                    period = period,
                    alertThreshold = alertThreshold,
                    startDate = startDate,
                    endDate = endDate
                ) ?: Budget(
                    categoryId = category.id,
                    budgetAmount = amount,
                    period = period,
                    startDate = startDate,
                    endDate = endDate,
                    alertThreshold = alertThreshold
                )

                if (existingBudget != null) {
                    budgetRepository.updateBudget(budget)
                } else {
                    budgetRepository.insertBudget(budget)
                }
                
                dismiss()
            } catch (e: Exception) {
                // Show error message
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}