package com.example.householdbudget.ui.goal

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
import com.example.householdbudget.data.entity.GoalType
import com.example.householdbudget.databinding.FragmentGoalListBinding
import com.example.householdbudget.ui.goal.adapter.AchievementAdapter
import com.example.householdbudget.ui.goal.adapter.GoalAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class GoalListFragment : Fragment() {

    private var _binding: FragmentGoalListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalViewModel by viewModels()

    private lateinit var goalAdapter: GoalAdapter
    private lateinit var achievementAdapter: AchievementAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
        setupClickListeners()
    }

    private fun setupViews() {
        goalAdapter = GoalAdapter(
            onItemClick = { goalItem ->
                showGoalEditDialog(goalItem)
            },
            onMenuClick = { goalItem ->
                showGoalOptionsDialog(goalItem)
            }
        )

        achievementAdapter = AchievementAdapter { achievement ->
            // Handle achievement click if needed
        }

        binding.recyclerViewGoals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = goalAdapter
        }

        binding.recyclerViewAchievements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = achievementAdapter
        }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is GoalUiState.Loading -> {
                    // Show loading state
                }
                is GoalUiState.Success -> {
                    // Hide loading state
                }
                is GoalUiState.Error -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.filteredGoals.observe(viewLifecycleOwner) { goals ->
            goalAdapter.submitList(goals)
            binding.layoutEmptyGoals.visibility = if (goals.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.achievements.observe(viewLifecycleOwner) { achievements ->
            achievementAdapter.submitList(achievements)
            binding.cardAchievementHistory.visibility = 
                if (achievements.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.activeGoalsCount.observe(viewLifecycleOwner) { count ->
            binding.tvActiveGoalsCount.text = count.toString()
        }

        viewModel.achievedGoalsCount.observe(viewLifecycleOwner) { count ->
            binding.tvAchievedGoalsCount.text = count.toString()
        }

        viewModel.currentFilter.observe(viewLifecycleOwner) { filter ->
            updateFilterChips(filter)
        }
    }

    private fun setupClickListeners() {
        binding.chipGroupGoalType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val filter = when (checkedIds.first()) {
                    R.id.chipAllGoals -> GoalFilter.ALL
                    R.id.chipSavingGoals -> GoalFilter.SAVING
                    R.id.chipExpenseReductionGoals -> GoalFilter.EXPENSE_REDUCTION
                    else -> GoalFilter.ALL
                }
                viewModel.setFilter(filter)
            }
        }

        binding.btnSortGoals.setOnClickListener {
            showSortDialog()
        }

        binding.fabAddGoal.setOnClickListener {
            showGoalSetupDialog()
        }

        binding.btnCreateFirstGoal.setOnClickListener {
            showGoalSetupDialog()
        }

        binding.btnViewAllAchievements.setOnClickListener {
            // Navigate to achievements detail screen
        }
    }

    private fun updateFilterChips(filter: GoalFilter) {
        val chipId = when (filter) {
            GoalFilter.ALL -> R.id.chipAllGoals
            GoalFilter.SAVING -> R.id.chipSavingGoals
            GoalFilter.EXPENSE_REDUCTION -> R.id.chipExpenseReductionGoals
            else -> R.id.chipAllGoals
        }
        binding.chipGroupGoalType.check(chipId)
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "進捗率 (高い順)",
            "進捗率 (低い順)",
            "目標日 (近い順)",
            "目標日 (遠い順)",
            "目標金額 (高い順)",
            "目標金額 (低い順)",
            "名前順"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("並び替え")
            .setItems(sortOptions) { _, which ->
                val sortOrder = when (which) {
                    0 -> GoalSortOrder.PROGRESS_DESC
                    1 -> GoalSortOrder.PROGRESS_ASC
                    2 -> GoalSortOrder.TARGET_DATE_ASC
                    3 -> GoalSortOrder.TARGET_DATE_DESC
                    4 -> GoalSortOrder.TARGET_AMOUNT_DESC
                    5 -> GoalSortOrder.TARGET_AMOUNT_ASC
                    6 -> GoalSortOrder.NAME_ASC
                    else -> GoalSortOrder.PROGRESS_DESC
                }
                viewModel.setSortOrder(sortOrder)
            }
            .show()
    }

    private fun showGoalSetupDialog(goalItem: GoalItem? = null) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_goal_setup, null)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        setupGoalDialog(dialogView, goalItem, dialog)
        dialog.show()
    }

    private fun setupGoalDialog(
        dialogView: View,
        goalItem: GoalItem?,
        dialog: androidx.appcompat.app.AlertDialog
    ) {
        val binding = com.example.householdbudget.databinding.DialogGoalSetupBinding.bind(dialogView)

        // Setup goal type dropdown
        val goalTypes = listOf("貯金目標", "支出削減目標")
        val goalTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, goalTypes)
        binding.actvGoalType.setAdapter(goalTypeAdapter)

        // Setup category dropdown
        val categories = viewModel.categories.value ?: emptyList()
        val categoryNames = categories.map { it.name }
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
        binding.actvCategory.setAdapter(categoryAdapter)

        // Fill existing data if editing
        if (goalItem != null) {
            binding.tvDialogTitle.text = "目標を編集"
            binding.etGoalName.setText(goalItem.goal.name)
            binding.actvGoalType.setText(
                when (goalItem.goal.type) {
                    GoalType.SAVING -> "貯金目標"
                    GoalType.EXPENSE_REDUCTION -> "支出削減目標"
                }
            )
            binding.etTargetAmount.setText(goalItem.goal.targetAmount.toString())
            binding.layoutCurrentAmount.visibility = View.VISIBLE
            binding.etCurrentAmount.setText(goalItem.currentAmount.toString())
            binding.etGoalDescription.setText(goalItem.goal.description ?: "")
            
            goalItem.category?.let { category ->
                binding.actvCategory.setText(category.name)
            }

            // Set milestone notifications
            val milestones = goalItem.goal.milestoneNotifications?.split(",")?.mapNotNull { 
                it.toIntOrNull() 
            } ?: emptyList()
            
            binding.chipMilestone25.isChecked = 25 in milestones
            binding.chipMilestone50.isChecked = 50 in milestones
            binding.chipMilestone75.isChecked = 75 in milestones
            binding.chipMilestone90.isChecked = 90 in milestones
        } else {
            binding.tvDialogTitle.text = "目標を追加"
            binding.actvGoalType.setText("貯金目標")
        }

        // Setup date picker
        var selectedDate = goalItem?.goal?.targetDate ?: run {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, 6) // Default to 6 months from now
            calendar.time
        }
        
        val dateFormat = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())
        binding.etTargetDate.setText(dateFormat.format(selectedDate))
        
        binding.etTargetDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.time
                    binding.etTargetDate.setText(dateFormat.format(selectedDate))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Handle goal type change
        binding.actvGoalType.setOnItemClickListener { _, _, position, _ ->
            val isExpenseReduction = position == 1
            binding.layoutCategory.visibility = if (isExpenseReduction) View.VISIBLE else View.GONE
        }

        // Handle milestone notifications toggle
        binding.switchMilestoneNotifications.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutMilestoneOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Setup buttons
        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveGoal(binding, goalItem, selectedDate, dialog)
        }
    }

    private fun saveGoal(
        binding: com.example.householdbudget.databinding.DialogGoalSetupBinding,
        goalItem: GoalItem?,
        selectedDate: Date,
        dialog: androidx.appcompat.app.AlertDialog
    ) {
        val name = binding.etGoalName.text.toString().trim()
        val goalTypeText = binding.actvGoalType.text.toString()
        val targetAmountText = binding.etTargetAmount.text.toString()
        val currentAmountText = binding.etCurrentAmount.text.toString()
        val categoryName = binding.actvCategory.text.toString()
        val description = binding.etGoalDescription.text.toString().takeIf { it.isNotBlank() }

        // Validation
        if (name.isBlank()) {
            binding.layoutGoalName.error = "目標名を入力してください"
            return
        }

        if (targetAmountText.isBlank()) {
            binding.layoutTargetAmount.error = "目標金額を入力してください"
            return
        }

        val targetAmount = try {
            BigDecimal(targetAmountText)
        } catch (e: NumberFormatException) {
            binding.layoutTargetAmount.error = "正しい金額を入力してください"
            return
        }

        val currentAmount = if (currentAmountText.isBlank()) {
            BigDecimal.ZERO
        } else {
            try {
                BigDecimal(currentAmountText)
            } catch (e: NumberFormatException) {
                binding.layoutCurrentAmount.error = "正しい金額を入力してください"
                return
            }
        }

        val goalType = when (goalTypeText) {
            "貯金目標" -> GoalType.SAVING
            "支出削減目標" -> GoalType.EXPENSE_REDUCTION
            else -> GoalType.SAVING
        }

        val category = viewModel.categories.value?.find { it.name == categoryName }
        val categoryId = if (goalType == GoalType.EXPENSE_REDUCTION && category != null) {
            category.id
        } else null

        // Get milestone notifications
        val milestoneNotifications = mutableListOf<Int>()
        if (binding.switchMilestoneNotifications.isChecked) {
            if (binding.chipMilestone25.isChecked) milestoneNotifications.add(25)
            if (binding.chipMilestone50.isChecked) milestoneNotifications.add(50)
            if (binding.chipMilestone75.isChecked) milestoneNotifications.add(75)
            if (binding.chipMilestone90.isChecked) milestoneNotifications.add(90)
        }

        if (goalItem != null) {
            // Update existing goal
            viewModel.updateGoal(
                goalItem.goal.id,
                name,
                targetAmount,
                currentAmount,
                selectedDate,
                categoryId,
                description,
                milestoneNotifications
            )
        } else {
            // Create new goal
            viewModel.saveGoal(
                name,
                goalType,
                targetAmount,
                currentAmount,
                selectedDate,
                categoryId,
                description,
                milestoneNotifications
            )
        }

        dialog.dismiss()
    }

    private fun showGoalEditDialog(goalItem: GoalItem) {
        showGoalSetupDialog(goalItem)
    }

    private fun showGoalOptionsDialog(goalItem: GoalItem) {
        val options = mutableListOf<String>()
        options.add("編集")
        
        if (goalItem.isAchieved) {
            options.add("詳細を表示")
        } else {
            options.add("達成済みにする")
            options.add(if (goalItem.goal.status.name == "ACTIVE") "一時停止" else "再開")
        }
        
        options.add("削除")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(goalItem.goal.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "編集" -> showGoalEditDialog(goalItem)
                    "達成済みにする" -> viewModel.markGoalAsAchieved(goalItem.goal.id)
                    "一時停止", "再開" -> viewModel.toggleGoalStatus(goalItem.goal.id)
                    "削除" -> showDeleteConfirmation(goalItem)
                    "詳細を表示" -> {
                        // Show goal details
                    }
                }
            }
            .show()
    }

    private fun showDeleteConfirmation(goalItem: GoalItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("目標を削除")
            .setMessage("「${goalItem.goal.name}」を削除しますか？この操作は取り消せません。")
            .setPositiveButton("削除") { _, _ ->
                viewModel.deleteGoal(goalItem.goal.id)
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}