package com.example.householdbudget.ui.category

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.householdbudget.R
import com.example.householdbudget.data.entity.Category
import com.example.householdbudget.data.entity.CategoryType
import com.example.householdbudget.databinding.DialogCategoryEditBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryEditDialogFragment : DialogFragment() {

    private var _binding: DialogCategoryEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryViewModel by activityViewModels()
    
    private lateinit var iconAdapter: IconSelectionAdapter
    private lateinit var colorAdapter: ColorSelectionAdapter
    
    private var selectedIconResId: Int = R.drawable.ic_more_horiz
    private var selectedColor: String = "#FF9800"
    
    private var dialogState: CategoryEditDialogState? = null

    companion object {
        private const val ARG_DIALOG_STATE = "dialog_state"

        fun newInstance(dialogState: CategoryEditDialogState): CategoryEditDialogFragment {
            return CategoryEditDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_DIALOG_STATE, dialogState)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialogState = arguments?.getSerializable(ARG_DIALOG_STATE) as? CategoryEditDialogState
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCategoryEditBinding.inflate(layoutInflater)
        
        setupViews()
        setupAdapters()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun setupViews() {
        val state = dialogState ?: return
        
        when (state) {
            is CategoryEditDialogState.Add -> {
                binding.tvDialogTitle.text = "新しいカテゴリを追加"
                binding.tvCategoryTypeLabel.visibility = View.VISIBLE
                binding.radioGroupCategoryType.visibility = View.VISIBLE
                
                when (state.type) {
                    CategoryType.EXPENSE -> binding.rbExpense.isChecked = true
                    CategoryType.INCOME -> binding.rbIncome.isChecked = true
                }
            }
            is CategoryEditDialogState.Edit -> {
                binding.tvDialogTitle.text = "カテゴリを編集"
                binding.tvCategoryTypeLabel.visibility = View.GONE
                binding.radioGroupCategoryType.visibility = View.GONE
                
                // Pre-fill existing values
                binding.etCategoryName.setText(state.category.name)
                selectedIconResId = state.category.iconResId
                selectedColor = state.category.color
            }
        }

        binding.btnCancel.setOnClickListener {
            viewModel.dismissEditDialog()
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveCategory()
        }
    }

    private fun setupAdapters() {
        // Icon selection adapter
        iconAdapter = IconSelectionAdapter(getAvailableIcons()) { iconResId ->
            selectedIconResId = iconResId
        }
        binding.recyclerViewIcons.apply {
            layoutManager = GridLayoutManager(context, 6)
            adapter = iconAdapter
        }

        // Color selection adapter
        colorAdapter = ColorSelectionAdapter(getAvailableColors()) { color ->
            selectedColor = color
        }
        binding.recyclerViewColors.apply {
            layoutManager = GridLayoutManager(context, 8)
            adapter = colorAdapter
        }

        // Set initial selections
        iconAdapter.setSelectedIcon(selectedIconResId)
        colorAdapter.setSelectedColor(selectedColor)
    }

    private fun saveCategory() {
        val name = binding.etCategoryName.text.toString()
        if (name.isBlank()) {
            binding.etCategoryName.error = "カテゴリ名を入力してください"
            return
        }

        val categoryType = when (dialogState) {
            is CategoryEditDialogState.Add -> {
                if (binding.rbExpense.isChecked) CategoryType.EXPENSE else CategoryType.INCOME
            }
            is CategoryEditDialogState.Edit -> {
                dialogState.category.type
            }
            null -> CategoryType.EXPENSE
        }

        val categoryId = when (dialogState) {
            is CategoryEditDialogState.Edit -> dialogState.category.id
            else -> null
        }

        viewModel.saveCategory(
            categoryId = categoryId,
            name = name,
            type = categoryType,
            iconResId = selectedIconResId,
            color = selectedColor
        )

        dismiss()
    }

    private fun getAvailableIcons(): List<Int> {
        return listOf(
            R.drawable.ic_restaurant,
            R.drawable.ic_home,
            R.drawable.ic_directions_car,
            R.drawable.ic_flash_on,
            R.drawable.ic_phone,
            R.drawable.ic_local_hospital,
            R.drawable.ic_movie,
            R.drawable.ic_shopping_bag,
            R.drawable.ic_school,
            R.drawable.ic_work,
            R.drawable.ic_business,
            R.drawable.ic_trending_up,
            R.drawable.ic_attach_money,
            R.drawable.ic_more_horiz
        )
    }

    private fun getAvailableColors(): List<String> {
        return listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#03DAC5", "#4CAF50",
            "#8BC34A", "#CDDC39", "#FFC107", "#FF9800",
            "#FF5722", "#795548", "#9E9E9E", "#607D8B"
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}