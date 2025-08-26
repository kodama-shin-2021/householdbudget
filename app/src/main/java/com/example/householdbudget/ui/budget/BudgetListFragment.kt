package com.example.householdbudget.ui.budget

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.householdbudget.R
import com.example.householdbudget.databinding.FragmentBudgetListBinding
import com.example.householdbudget.ui.budget.adapter.CategoryBudgetAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class BudgetListFragment : Fragment() {

    private var _binding: FragmentBudgetListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by viewModels()

    private lateinit var categoryBudgetAdapter: CategoryBudgetAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
        setupClickListeners()
    }

    private fun setupViews() {
        categoryBudgetAdapter = CategoryBudgetAdapter(
            onItemClick = { budgetItem ->
                showBudgetEditDialog(budgetItem)
            },
            onMenuClick = { budgetItem ->
                showBudgetOptionsDialog(budgetItem)
            }
        )

        binding.recyclerViewCategoryBudgets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryBudgetAdapter
        }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BudgetUiState.Loading -> {
                    // Show loading state
                }
                is BudgetUiState.Success -> {
                    // Hide loading state
                }
                is BudgetUiState.Error -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.currentPeriod.observe(viewLifecycleOwner) { period ->
            updatePeriodChips(period)
        }

        viewModel.totalBudget.observe(viewLifecycleOwner) { totalBudget ->
            binding.tvTotalBudget.text = viewModel.formatCurrency(totalBudget)
        }

        viewModel.totalUsed.observe(viewLifecycleOwner) { totalUsed ->
            binding.tvUsedBudget.text = viewModel.formatCurrency(totalUsed)
        }

        viewModel.totalRemaining.observe(viewLifecycleOwner) { totalRemaining ->
            binding.tvRemainingBudget.text = viewModel.formatCurrency(totalRemaining)
            
            // Update progress bar
            val totalBudget = viewModel.totalBudget.value ?: BigDecimal.ZERO
            val progress = if (totalBudget > BigDecimal.ZERO) {
                val totalUsed = viewModel.totalUsed.value ?: BigDecimal.ZERO
                ((totalUsed.toDouble() / totalBudget.toDouble()) * 100).toInt()
            } else {
                0
            }
            binding.progressTotalBudget.progress = progress.coerceAtMost(100)
        }

        viewModel.categoryBudgets.observe(viewLifecycleOwner) { budgets ->
            categoryBudgetAdapter.submitList(budgets)
            binding.layoutEmptyCategoryBudgets.visibility = 
                if (budgets.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.budgetAlert.observe(viewLifecycleOwner) { alert ->
            if (alert != null) {
                binding.cardBudgetAlert.visibility = View.VISIBLE
                binding.tvBudgetAlertMessage.text = alert.message
            } else {
                binding.cardBudgetAlert.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        binding.chipGroupBudgetPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val period = when (checkedIds.first()) {
                    R.id.chipWeeklyBudget -> BudgetPeriod.WEEKLY
                    R.id.chipMonthlyBudget -> BudgetPeriod.MONTHLY
                    R.id.chipYearlyBudget -> BudgetPeriod.YEARLY
                    else -> BudgetPeriod.MONTHLY
                }
                viewModel.setPeriod(period)
            }
        }

        binding.btnEditTotalBudget.setOnClickListener {
            showTotalBudgetEditDialog()
        }

        binding.btnAddCategoryBudget.setOnClickListener {
            showBudgetSetupDialog()
        }

        binding.fabAddBudget.setOnClickListener {
            showBudgetSetupDialog()
        }
    }

    private fun updatePeriodChips(period: BudgetPeriod) {
        val chipId = when (period) {
            BudgetPeriod.WEEKLY -> R.id.chipWeeklyBudget
            BudgetPeriod.MONTHLY -> R.id.chipMonthlyBudget
            BudgetPeriod.YEARLY -> R.id.chipYearlyBudget
        }
        binding.chipGroupBudgetPeriod.check(chipId)
    }

    private fun showTotalBudgetEditDialog() {
        // Implementation for editing total budget
        // This would show a simple input dialog for total budget amount
    }

    private fun showBudgetSetupDialog(budgetItem: CategoryBudgetItem? = null) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_budget_setup, null)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        // Setup dialog components
        setupBudgetDialog(dialogView, budgetItem, dialog)
        
        dialog.show()
    }

    private fun setupBudgetDialog(
        dialogView: View, 
        budgetItem: CategoryBudgetItem?, 
        dialog: androidx.appcompat.app.AlertDialog
    ) {
        val binding = com.example.householdbudget.databinding.DialogBudgetSetupBinding.bind(dialogView)
        
        // Setup category dropdown
        val categories = viewModel.categories.value ?: emptyList()
        val categoryNames = categories.map { it.name }
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
        binding.actvCategory.setAdapter(categoryAdapter)
        
        // Setup period dropdown
        val periods = listOf("週間", "月間", "年間")
        val periodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, periods)
        binding.actvBudgetPeriod.setAdapter(periodAdapter)
        
        // Fill existing data if editing
        if (budgetItem != null) {
            binding.tvDialogTitle.text = "予算を編集"
            binding.layoutCategory.visibility = View.GONE
            binding.etBudgetAmount.setText(budgetItem.budget.budgetAmount.toString())
            binding.actvBudgetPeriod.setText(
                when (budgetItem.budget.period) {
                    "WEEKLY" -> "週間"
                    "MONTHLY" -> "月間"
                    "YEARLY" -> "年間"
                    else -> "月間"
                }
            )
            binding.sliderAlertThreshold.value = budgetItem.budget.alertThreshold.toFloat()
            binding.etNotes.setText(budgetItem.budget.notes ?: "")
        } else {
            binding.tvDialogTitle.text = "予算を追加"
            binding.layoutCategory.visibility = View.VISIBLE
            binding.actvBudgetPeriod.setText("月間")
            binding.sliderAlertThreshold.value = 80f
        }

        // Setup date picker
        var selectedDate = budgetItem?.budget?.startDate ?: Date()
        val dateFormat = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())
        binding.etStartDate.setText(dateFormat.format(selectedDate))
        
        binding.etStartDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.time
                    binding.etStartDate.setText(dateFormat.format(selectedDate))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Setup slider
        binding.sliderAlertThreshold.addOnChangeListener { _, value, _ ->
            binding.tvAlertThreshold.text = "${value.toInt()}%"
        }
        binding.tvAlertThreshold.text = "${binding.sliderAlertThreshold.value.toInt()}%"

        // Setup buttons
        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveBudget(binding, budgetItem, selectedDate, dialog)
        }
    }

    private fun saveBudget(
        binding: com.example.householdbudget.databinding.DialogBudgetSetupBinding,
        budgetItem: CategoryBudgetItem?,
        selectedDate: Date,
        dialog: androidx.appcompat.app.AlertDialog
    ) {
        val amountText = binding.etBudgetAmount.text.toString()
        val periodText = binding.actvBudgetPeriod.text.toString()
        val alertThreshold = binding.sliderAlertThreshold.value.toInt()
        val notes = binding.etNotes.text.toString().takeIf { it.isNotBlank() }

        if (amountText.isBlank()) {
            binding.layoutBudgetAmount.error = "予算額を入力してください"
            return
        }

        val amount = try {
            BigDecimal(amountText)
        } catch (e: NumberFormatException) {
            binding.layoutBudgetAmount.error = "正しい金額を入力してください"
            return
        }

        val period = when (periodText) {
            "週間" -> BudgetPeriod.WEEKLY
            "月間" -> BudgetPeriod.MONTHLY
            "年間" -> BudgetPeriod.YEARLY
            else -> BudgetPeriod.MONTHLY
        }

        if (budgetItem != null) {
            // Update existing budget
            viewModel.updateBudget(
                budgetItem.budget.id,
                amount,
                alertThreshold,
                notes
            )
        } else {
            // Create new budget
            val categoryName = binding.actvCategory.text.toString()
            val category = viewModel.categories.value?.find { it.name == categoryName }
            
            if (category == null) {
                binding.layoutCategory.error = "カテゴリを選択してください"
                return
            }

            viewModel.saveBudget(
                category.id,
                amount,
                period,
                selectedDate,
                alertThreshold,
                notes
            )
        }

        dialog.dismiss()
    }

    private fun showBudgetEditDialog(budgetItem: CategoryBudgetItem) {
        showBudgetSetupDialog(budgetItem)
    }

    private fun showBudgetOptionsDialog(budgetItem: CategoryBudgetItem) {
        val options = arrayOf("編集", "削除", if (budgetItem.budget.isActive) "無効化" else "有効化")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(budgetItem.category.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showBudgetEditDialog(budgetItem)
                    1 -> showDeleteConfirmation(budgetItem)
                    2 -> viewModel.toggleBudgetStatus(budgetItem.budget.id)
                }
            }
            .show()
    }

    private fun showDeleteConfirmation(budgetItem: CategoryBudgetItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("予算を削除")
            .setMessage("${budgetItem.category.name}の予算を削除しますか？")
            .setPositiveButton("削除") { _, _ ->
                viewModel.deleteBudget(budgetItem.budget.id)
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}