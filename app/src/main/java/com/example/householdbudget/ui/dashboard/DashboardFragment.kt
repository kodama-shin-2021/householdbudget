package com.example.householdbudget.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.householdbudget.R
import com.example.householdbudget.databinding.FragmentDashboardBinding
import com.example.householdbudget.ui.dashboard.adapter.BudgetProgressAdapter
import com.example.householdbudget.ui.dashboard.adapter.RecentTransactionAdapter
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    private lateinit var budgetProgressAdapter: BudgetProgressAdapter
    private lateinit var recentTransactionAdapter: RecentTransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
        setupClickListeners()
        setupChart()
    }

    private fun setupViews() {
        budgetProgressAdapter = BudgetProgressAdapter { budgetItem ->
            // Navigate to budget details
        }

        recentTransactionAdapter = RecentTransactionAdapter(
            onItemClick = { transaction ->
                // Navigate to transaction details or edit
            },
            getCategoryName = { categoryId ->
                viewModel.getCategoryById(categoryId)?.name
            },
            getCategoryIcon = { categoryId ->
                viewModel.getCategoryById(categoryId)?.iconResId
            },
            getCategoryColor = { categoryId ->
                viewModel.getCategoryById(categoryId)?.color
            }
        )

        binding.recyclerViewBudgetProgress.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = budgetProgressAdapter
        }

        binding.recyclerViewRecentTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentTransactionAdapter
        }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DashboardUiState.Loading -> {
                    // Show loading state
                }
                is DashboardUiState.Success -> {
                    // Hide loading state
                }
                is DashboardUiState.Error -> {
                    // Show error state
                }
            }
        }

        viewModel.currentPeriod.observe(viewLifecycleOwner) { periodType ->
            updatePeriodChips(periodType)
        }

        viewModel.periodDisplayText.observe(viewLifecycleOwner) { displayText ->
            binding.tvCurrentPeriod.text = displayText
            binding.tvPeriodType.text = when (viewModel.currentPeriod.value) {
                PeriodType.DAILY -> "日間表示"
                PeriodType.WEEKLY -> "週間表示"
                PeriodType.MONTHLY -> "月間表示"
                PeriodType.YEARLY -> "年間表示"
                null -> "月間表示"
            }
        }

        viewModel.balance.observe(viewLifecycleOwner) { balance ->
            binding.tvBalance.text = viewModel.formatCurrency(balance)
        }

        viewModel.totalIncome.observe(viewLifecycleOwner) { income ->
            binding.tvTotalIncome.text = viewModel.formatCurrency(income)
        }

        viewModel.totalExpense.observe(viewLifecycleOwner) { expense ->
            binding.tvTotalExpense.text = viewModel.formatCurrency(expense)
        }

        viewModel.budgetProgressList.observe(viewLifecycleOwner) { budgetProgress ->
            budgetProgressAdapter.submitList(budgetProgress)
        }

        viewModel.recentTransactions.observe(viewLifecycleOwner) { transactions ->
            recentTransactionAdapter.submitList(transactions)
            binding.layoutEmptyRecentTransactions.visibility = 
                if (transactions.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.categoryExpenses.observe(viewLifecycleOwner) { categoryExpenses ->
            updatePieChart(categoryExpenses)
        }
    }

    private fun setupClickListeners() {
        binding.btnPreviousPeriod.setOnClickListener {
            viewModel.navigateToPreviousPeriod()
        }

        binding.btnNextPeriod.setOnClickListener {
            viewModel.navigateToNextPeriod()
        }

        binding.chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val periodType = when (checkedIds.first()) {
                    R.id.chipDaily -> PeriodType.DAILY
                    R.id.chipWeekly -> PeriodType.WEEKLY
                    R.id.chipMonthly -> PeriodType.MONTHLY
                    R.id.chipYearly -> PeriodType.YEARLY
                    else -> PeriodType.MONTHLY
                }
                viewModel.setPeriodType(periodType)
            }
        }

        binding.cardQuickAddExpense.setOnClickListener {
            val bundle = Bundle().apply {
                putString("transactionType", "EXPENSE")
            }
            findNavController().navigate(R.id.add_transaction, bundle)
        }

        binding.cardQuickAddIncome.setOnClickListener {
            val bundle = Bundle().apply {
                putString("transactionType", "INCOME")
            }
            findNavController().navigate(R.id.add_transaction, bundle)
        }

        binding.fabQuickAdd.setOnClickListener {
            findNavController().navigate(R.id.add_transaction)
        }

        binding.btnViewAllBudgets.setOnClickListener {
            findNavController().navigate(R.id.budgets)
        }

        binding.btnViewAllTransactions.setOnClickListener {
            findNavController().navigate(R.id.transactions)
        }
    }

    private fun updatePeriodChips(periodType: PeriodType) {
        val chipId = when (periodType) {
            PeriodType.DAILY -> R.id.chipDaily
            PeriodType.WEEKLY -> R.id.chipWeekly
            PeriodType.MONTHLY -> R.id.chipMonthly
            PeriodType.YEARLY -> R.id.chipYearly
        }
        binding.chipGroupPeriod.check(chipId)
    }

    private fun setupChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelTextSize(12f)
            setEntryLabelColor(Color.BLACK)
            centerText = "支出の内訳"
            setCenterTextSize(16f)
            setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface))
            
            // Enable touch gestures
            setTouchEnabled(true)
            setDrawHoleEnabled(true)
            setHoleColor(Color.TRANSPARENT)
            setHoleRadius(40f)
            setTransparentCircleRadius(45f)
            
            // Rotation
            setRotationAngle(0f)
            setRotationEnabled(true)
            setHighlightPerTapEnabled(true)
            
            // Animation
            animateY(1400, Easing.EaseInOutQuad)
            
            // Legend
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                xEntrySpace = 7f
                yEntrySpace = 0f
                yOffset = 0f
                textSize = 12f
            }
        }
    }

    private fun updatePieChart(categoryExpenses: List<CategoryExpenseItem>) {
        if (categoryExpenses.isEmpty()) {
            binding.pieChart.clear()
            binding.pieChart.centerText = "データがありません"
            return
        }

        val entries = categoryExpenses.map { expense ->
            PieEntry(expense.amount.toFloat(), expense.categoryName)
        }

        val colors = categoryExpenses.map { expense ->
            try {
                Color.parseColor(expense.color)
            } catch (e: IllegalArgumentException) {
                ContextCompat.getColor(requireContext(), R.color.primary_500)
            }
        }

        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(binding.pieChart)
            sliceSpace = 3f
            selectionShift = 5f
        }

        val data = PieData(dataSet)
        binding.pieChart.data = data
        binding.pieChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}