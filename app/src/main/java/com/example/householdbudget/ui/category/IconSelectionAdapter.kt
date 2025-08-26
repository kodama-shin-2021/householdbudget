package com.example.householdbudget.ui.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.householdbudget.R
import com.example.householdbudget.databinding.ItemIconSelectionBinding

class IconSelectionAdapter(
    private val icons: List<Int>,
    private val onIconSelected: (Int) -> Unit
) : RecyclerView.Adapter<IconSelectionAdapter.IconViewHolder>() {

    private var selectedIcon: Int = icons.firstOrNull() ?: 0

    fun setSelectedIcon(iconResId: Int) {
        val oldPosition = icons.indexOf(selectedIcon)
        selectedIcon = iconResId
        val newPosition = icons.indexOf(selectedIcon)
        
        if (oldPosition != -1) notifyItemChanged(oldPosition)
        if (newPosition != -1) notifyItemChanged(newPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val binding = ItemIconSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IconViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(icons[position])
    }

    override fun getItemCount(): Int = icons.size

    inner class IconViewHolder(
        private val binding: ItemIconSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(iconResId: Int) {
            binding.ivIcon.setImageResource(iconResId)
            
            // Update selection state
            val isSelected = iconResId == selectedIcon
            binding.root.isSelected = isSelected
            
            if (isSelected) {
                binding.root.setBackgroundResource(R.drawable.selector_icon_selected)
            } else {
                binding.root.setBackgroundResource(R.drawable.selector_icon_background)
            }

            binding.root.setOnClickListener {
                val oldPosition = icons.indexOf(selectedIcon)
                selectedIcon = iconResId
                
                if (oldPosition != -1) notifyItemChanged(oldPosition)
                notifyItemChanged(adapterPosition)
                
                onIconSelected(iconResId)
            }
        }
    }
}