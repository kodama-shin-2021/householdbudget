package com.example.householdbudget.ui.regular

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.householdbudget.R
import com.example.householdbudget.data.entity.CategoryType
import com.example.householdbudget.data.entity.RecurrenceFrequency
import com.example.householdbudget.data.entity.TransactionType
import com.example.householdbudget.databinding.FragmentRegularTransactionAddBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class RegularTransactionAddFragment : Fragment() {

    private var _binding: FragmentRegularTransactionAddBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegularTransactionViewModel by viewModels()
    
    private var selectedStartDate: Date = Date()
    private var selectedEndDate: Date? = null
    private var selectedCategoryId: Long? = null
    private var selectedSubcategoryId: Long? = null
    
    private val dateFormat = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegularTransactionAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupObservers()
    }

    private fun setupViews() {
        // Setup toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Setup transaction type toggle
        binding.toggleGroupTransactionType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnExpense -> {
                        loadCategories(CategoryType.EXPENSE)
                    }
                    R.id.btnIncome -> {
                        loadCategories(CategoryType.INCOME)
                    }
                }
            }
        }

        // Setup frequency chips
        binding.chipGroupFrequency.setOnCheckedStateChangeListener { _, checkedIds ->
            updateIntervalUnit()
        }

        // Setup date pickers
        binding.etStartDate.setText(dateFormat.format(selectedStartDate))
        binding.etStartDate.setOnClickListener {
            showDatePicker(true)
        }

        binding.etEndDate.setOnClickListener {
            showDatePicker(false)
        }

        // Setup end date toggle
        binding.switchEndDate.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutEndDate.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                selectedEndDate = null
                binding.etEndDate.text?.clear()
            }
        }

        // Setup notification toggle
        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutNotificationDays.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Setup save button
        binding.btnSave.setOnClickListener {
            saveRegularTransaction()
        }

        // Load initial categories (default to expense)
        loadCategories(CategoryType.EXPENSE)
        updateIntervalUnit()
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RegularTransactionUiState.Loading -> {
                    binding.btnSave.isEnabled = false
                }
                is RegularTransactionUiState.Success -> {
                    binding.btnSave.isEnabled = true
                    // Navigate back on successful save
                    findNavController().navigateUp()
                }
                is RegularTransactionUiState.Error -> {
                    binding.btnSave.isEnabled = true
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> {
                    binding.btnSave.isEnabled = true
                }
            }
        }
    }

    private fun loadCategories(type: CategoryType) {
        viewModel.getCategoriesByType(type).observe(viewLifecycleOwner) { categories ->
            val categoryNames = categories.map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categoryNames
            )
            binding.actvCategory.setAdapter(adapter)
            
            binding.actvCategory.setOnItemClickListener { _, _, position, _ ->
                selectedCategoryId = categories[position].id
                selectedSubcategoryId = null
                // Load subcategories if available
            }
        }
    }

    private fun updateIntervalUnit() {
        val unit = when {
            binding.chipWeekly.isChecked -> "週"
            binding.chipMonthly.isChecked -> "ヶ月"
            binding.chipYearly.isChecked -> "年"
            else -> "ヶ月"
        }
        binding.tvIntervalUnit.text = "${unit}ごと"
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val initialDate = if (isStartDate) selectedStartDate else selectedEndDate ?: Date()
        val calendar = Calendar.getInstance()
        calendar.time = initialDate
        
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDate = calendar.time
                
                if (isStartDate) {
                    selectedStartDate = selectedDate
                    binding.etStartDate.setText(dateFormat.format(selectedDate))
                } else {
                    selectedEndDate = selectedDate
                    binding.etEndDate.setText(dateFormat.format(selectedDate))
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveRegularTransaction() {
        // Validate input
        val name = binding.etName.text.toString()
        if (name.isBlank()) {
            binding.etName.error = "定期取引名を入力してください"
            return
        }

        val amountText = binding.etAmount.text.toString()
        if (amountText.isBlank()) {
            binding.etAmount.error = "金額を入力してください"
            return
        }

        val amount = try {
            BigDecimal(amountText)
        } catch (e: NumberFormatException) {
            binding.etAmount.error = "有効な金額を入力してください"
            return
        }

        if (amount <= BigDecimal.ZERO) {
            binding.etAmount.error = "金額は0より大きい値を入力してください"
            return
        }

        if (selectedCategoryId == null) {
            Snackbar.make(binding.root, "カテゴリを選択してください", Snackbar.LENGTH_SHORT).show()
            return
        }

        val intervalText = binding.etInterval.text.toString()
        val interval = try {
            intervalText.toInt()
        } catch (e: NumberFormatException) {
            binding.etInterval.error = "有効な間隔を入力してください"
            return
        }

        if (interval <= 0) {
            binding.etInterval.error = "間隔は1以上の値を入力してください"
            return
        }

        // Determine transaction type
        val transactionType = if (binding.btnExpense.isChecked) {
            TransactionType.EXPENSE
        } else {
            TransactionType.INCOME
        }

        // Get frequency
        val frequency = when {
            binding.chipWeekly.isChecked -> RecurrenceFrequency.WEEKLY
            binding.chipMonthly.isChecked -> RecurrenceFrequency.MONTHLY
            binding.chipYearly.isChecked -> RecurrenceFrequency.YEARLY
            else -> RecurrenceFrequency.MONTHLY
        }

        // Get description
        val description = binding.etDescription.text.toString().takeIf { it.isNotBlank() }

        // Get notification settings
        val notifyBeforeDays = if (binding.switchNotification.isChecked) {
            try {
                binding.etNotificationDays.text.toString().toInt().coerceAtLeast(0)
            } catch (e: NumberFormatException) {
                1
            }
        } else {
            0
        }

        // Get auto execute setting
        val autoExecute = binding.switchAutoExecute.isChecked

        // Save regular transaction
        viewModel.saveRegularTransaction(
            name = name,
            amount = amount,
            type = transactionType,
            categoryId = selectedCategoryId!!,
            subcategoryId = selectedSubcategoryId,
            description = description,
            frequency = frequency,
            interval = interval,
            startDate = selectedStartDate,
            endDate = selectedEndDate,
            autoExecute = autoExecute,
            notifyBeforeDays = notifyBeforeDays
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}