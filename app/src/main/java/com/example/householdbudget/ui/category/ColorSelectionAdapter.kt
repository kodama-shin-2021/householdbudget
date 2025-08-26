package com.example.householdbudget.ui.category

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.householdbudget.databinding.ItemColorSelectionBinding

class ColorSelectionAdapter(
    private val colors: List<String>,
    private val onColorSelected: (String) -> Unit
) : RecyclerView.Adapter<ColorSelectionAdapter.ColorViewHolder>() {

    private var selectedColor: String = colors.firstOrNull() ?: "#FF9800"

    fun setSelectedColor(color: String) {
        val oldPosition = colors.indexOf(selectedColor)
        selectedColor = color
        val newPosition = colors.indexOf(selectedColor)
        
        if (oldPosition != -1) notifyItemChanged(oldPosition)
        if (newPosition != -1) notifyItemChanged(newPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding = ItemColorSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position])
    }

    override fun getItemCount(): Int = colors.size

    inner class ColorViewHolder(
        private val binding: ItemColorSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(colorString: String) {
            try {
                val color = Color.parseColor(colorString)
                binding.viewColor.backgroundTintList = ColorStateList.valueOf(color)
            } catch (e: IllegalArgumentException) {
                // Use default color if invalid
                binding.viewColor.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
            }

            // Update selection state
            val isSelected = colorString == selectedColor
            binding.ivCheckmark.visibility = if (isSelected) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                val oldPosition = colors.indexOf(selectedColor)
                selectedColor = colorString
                
                if (oldPosition != -1) notifyItemChanged(oldPosition)
                notifyItemChanged(adapterPosition)
                
                onColorSelected(colorString)
            }
        }
    }
}