package com.example.householdbudget.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.example.householdbudget.R
import com.example.householdbudget.data.entity.TransactionType
import com.example.householdbudget.databinding.FragmentTransactionListBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class TransactionListFragment : Fragment(), MenuProvider {

    private var _binding: FragmentTransactionListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        
        setupViews()
        setupRecyclerView()
        setupObservers()
    }

    private fun setupViews() {
        // Setup search
        binding.etSearch.addTextChangedListener { editable ->
            val query = editable?.toString() ?: ""
            viewModel.searchTransactions(query)
        }

        // Setup filter chips
        binding.chipSortDate.setOnClickListener {
            val currentSort = viewModel.sortType.value
            val newSort = if (currentSort == SortType.DATE_DESC) {
                SortType.DATE_ASC
            } else {
                SortType.DATE_DESC
            }
            viewModel.setSortType(newSort)
            updateSortChipText(binding.chipSortDate, newSort)
        }

        binding.chipSortAmount.setOnClickListener {
            val currentSort = viewModel.sortType.value
            val newSort = if (currentSort == SortType.AMOUNT_DESC) {
                SortType.AMOUNT_ASC
            } else {
                SortType.AMOUNT_DESC
            }
            viewModel.setSortType(newSort)
            updateSortChipText(binding.chipSortAmount, newSort)
        }

        binding.chipFilterType.setOnClickListener {
            showTransactionTypeFilterDialog()
        }

        binding.chipFilterCategory.setOnClickListener {
            showCategoryFilterDialog()
        }

        binding.chipFilterDate.setOnClickListener {
            showDateRangeFilterDialog()
        }

        // Setup FAB
        binding.fabAddTransaction.setOnClickListener {
            navigateToAddTransaction()
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onTransactionClick = { transaction ->
                // Handle transaction click - maybe expand/collapse
            },
            onEditClick = { transaction ->
                navigateToEditTransaction(transaction.id)
            },
            onDeleteClick = { transaction ->
                viewModel.deleteTransaction(transaction.id)
            },
            categoryProvider = { categoryId ->
                viewModel.categories.value?.find { it.id == categoryId }
            },
            dateFormatter = { date ->
                viewModel.formatDate(date)
            }
        )

        binding.recyclerViewTransactions.adapter = transactionAdapter
    }

    private fun setupObservers() {
        viewModel.filteredTransactions.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.submitList(transactions)
            
            // Show/hide empty state
            if (transactions.isEmpty()) {
                binding.recyclerViewTransactions.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.VISIBLE
            } else {
                binding.recyclerViewTransactions.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
            }
        }

        viewModel.totalIncome.observe(viewLifecycleOwner) { income ->
            binding.tvTotalIncome.text = formatCurrency(income.toDouble())
        }

        viewModel.totalExpense.observe(viewLifecycleOwner) { expense ->
            binding.tvTotalExpense.text = formatCurrency(expense.toDouble())
        }

        viewModel.balance.observe(viewLifecycleOwner) { balance ->
            binding.tvBalance.text = formatCurrency(balance.toDouble())
            
            // Change color based on balance
            val color = if (balance.toDouble() >= 0) {
                requireContext().getColor(R.color.success_500)
            } else {
                requireContext().getColor(R.color.error_500)
            }
            binding.tvBalance.setTextColor(color)
        }

        viewModel.sortType.observe(viewLifecycleOwner) { sortType ->
            updateSortChips(sortType)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TransactionUiState.Loading -> {
                    // Show loading if needed
                }
                is TransactionUiState.Success -> {
                    // Hide loading
                }
                is TransactionUiState.Empty -> {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.recyclerViewTransactions.visibility = View.GONE
                }
                is TransactionUiState.Error -> {
                    // Show error message
                }
            }
        }
    }

    private fun updateSortChips(sortType: SortType) {
        // Reset all sort chips
        binding.chipSortDate.isChecked = false
        binding.chipSortAmount.isChecked = false

        when (sortType) {
            SortType.DATE_DESC, SortType.DATE_ASC -> {
                binding.chipSortDate.isChecked = true
                updateSortChipText(binding.chipSortDate, sortType)
            }
            SortType.AMOUNT_DESC, SortType.AMOUNT_ASC -> {
                binding.chipSortAmount.isChecked = true
                updateSortChipText(binding.chipSortAmount, sortType)
            }
        }
    }

    private fun updateSortChipText(chip: Chip, sortType: SortType) {
        val text = when (sortType) {
            SortType.DATE_DESC -> "日付順 ↓"
            SortType.DATE_ASC -> "日付順 ↑"
            SortType.AMOUNT_DESC -> "金額順 ↓"
            SortType.AMOUNT_ASC -> "金額順 ↑"
        }
        chip.text = text
    }

    private fun showTransactionTypeFilterDialog() {
        // TODO: Implement transaction type filter dialog
    }

    private fun showCategoryFilterDialog() {
        // TODO: Implement category filter dialog
    }

    private fun showDateRangeFilterDialog() {
        // TODO: Implement date range filter dialog
    }

    private fun navigateToAddTransaction() {
        findNavController().navigate(R.id.action_transactionList_to_transactionAdd)
    }

    private fun navigateToEditTransaction(transactionId: Long) {
        // TODO: Navigate to edit transaction with ID
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.JAPAN)
        return formatter.format(amount)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_transaction_list, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_clear_filters -> {
                viewModel.clearFilters()
                binding.etSearch.text?.clear()
                true
            }
            R.id.action_export -> {
                // TODO: Implement export functionality
                true
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}