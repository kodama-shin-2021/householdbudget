package com.example.householdbudget.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.householdbudget.data.entity.CategoryType
import com.example.householdbudget.databinding.FragmentCategoryManagementBinding
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryManagementFragment : Fragment() {

    private var _binding: FragmentCategoryManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryViewModel by viewModels()
    private lateinit var categoryPagerAdapter: CategoryPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewPager()
        setupFab()
        observeViewModel()
    }

    private fun setupViewPager() {
        categoryPagerAdapter = CategoryPagerAdapter(this)
        binding.viewPager.adapter = categoryPagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "支出"
                1 -> "収入"
                else -> ""
            }
        }.attach()

        // Set default tab to expense categories
        binding.viewPager.currentItem = 0
        viewModel.setCategoryType(CategoryType.EXPENSE)

        binding.tabLayout.addOnTabSelectedListener(object : TabLayoutMediator.TabConfigurationStrategy {
            override fun onConfigureTab(tab: com.google.android.material.tabs.TabLayout.Tab, position: Int) {
                when (position) {
                    0 -> {
                        tab.text = "支出"
                        viewModel.setCategoryType(CategoryType.EXPENSE)
                    }
                    1 -> {
                        tab.text = "収入"
                        viewModel.setCategoryType(CategoryType.INCOME)
                    }
                }
            }
        })
    }

    private fun setupFab() {
        binding.fabAddCategory.setOnClickListener {
            val currentType = if (binding.viewPager.currentItem == 0) {
                CategoryType.EXPENSE
            } else {
                CategoryType.INCOME
            }
            viewModel.showAddCategoryDialog(currentType)
        }
    }

    private fun observeViewModel() {
        viewModel.showEditDialog.observe(viewLifecycleOwner) { dialogState ->
            if (dialogState != null) {
                showCategoryEditDialog(dialogState)
            }
        }
    }

    private fun showCategoryEditDialog(dialogState: CategoryEditDialogState) {
        val dialog = CategoryEditDialogFragment.newInstance(dialogState)
        dialog.show(childFragmentManager, "CategoryEditDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class CategoryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> CategoryListFragment.newInstance(CategoryType.EXPENSE)
                1 -> CategoryListFragment.newInstance(CategoryType.INCOME)
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}