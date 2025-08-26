package com.example.householdbudget.ui.transaction

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
import com.example.householdbudget.databinding.FragmentTransactionAddBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class TransactionAddFragment : Fragment() {

    private var _binding: FragmentTransactionAddBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by viewModels()
    
    private var selectedDate: Date = Date()
    private var selectedCategoryId: Long? = null
    private var selectedSubcategoryId: Long? = null
    
    private val dateFormat = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionAddBinding.inflate(inflater, container, false)
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

        // Setup date picker
        binding.etDate.setText(dateFormat.format(selectedDate))
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // Setup regular transaction toggle
        binding.switchRegularTransaction.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutRegularSettings.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Setup save button
        binding.btnSave.setOnClickListener {
            saveTransaction()
        }

        // Load initial categories (default to expense)
        loadCategories(CategoryType.EXPENSE)
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TransactionUiState.Loading -> {
                    binding.btnSave.isEnabled = false
                }
                is TransactionUiState.Success -> {
                    binding.btnSave.isEnabled = true
                    // Navigate back on successful save
                    findNavController().navigateUp()
                }
                is TransactionUiState.Error -> {
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
                binding.actvSubcategory.text.clear()
                binding.layoutSubcategory.visibility = View.GONE
                
                // Load subcategories if available
                // This would need to be implemented in the repository
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.time
                binding.etDate.setText(dateFormat.format(selectedDate))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveTransaction() {
        // Validate input
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

        // Determine transaction type
        val transactionType = if (binding.btnExpense.isChecked) {
            TransactionType.EXPENSE
        } else {
            TransactionType.INCOME
        }

        // Get description
        val description = binding.etDescription.text.toString().takeIf { it.isNotBlank() }

        // Check if regular transaction
        val isRegular = binding.switchRegularTransaction.isChecked
        val frequency = if (isRegular) {
            when {
                binding.chipWeekly.isChecked -> RecurrenceFrequency.WEEKLY
                binding.chipMonthly.isChecked -> RecurrenceFrequency.MONTHLY
                binding.chipYearly.isChecked -> RecurrenceFrequency.YEARLY
                else -> RecurrenceFrequency.MONTHLY
            }
        } else {
            null
        }

        // Save transaction
        viewModel.saveTransaction(
            amount = amount,
            type = transactionType,
            categoryId = selectedCategoryId!!,
            subcategoryId = selectedSubcategoryId,
            description = description,
            date = selectedDate,
            isRegular = isRegular,
            frequency = frequency
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}