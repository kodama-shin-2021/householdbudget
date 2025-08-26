package com.example.householdbudget.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 * Performance optimization utilities for the household budget app
 */

/**
 * Generic DiffUtil.ItemCallback for items with IDs
 */
abstract class BaseItemCallback<T : Any> : DiffUtil.ItemCallback<T>() {
    abstract fun getId(item: T): Long
    abstract fun areContentsEqual(oldItem: T, newItem: T): Boolean

    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return getId(oldItem) == getId(newItem)
    }

    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        return areContentsEqual(oldItem, newItem)
    }
}

/**
 * Optimized RecyclerView item decoration for spacing
 */
class OptimizedSpaceItemDecoration(
    private val space: Int,
    private val includeEdge: Boolean = false
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = state.itemCount

        if (includeEdge) {
            outRect.left = space
            outRect.right = space
            if (position == 0) {
                outRect.top = space
            }
            outRect.bottom = space
        } else {
            outRect.left = space / 2
            outRect.right = space / 2
            if (position == 0) {
                outRect.top = space
            }
            outRect.bottom = space
        }
    }
}

/**
 * Memory-efficient ViewHolder base class
 */
abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    
    private var currentItem: T? = null
    
    fun bind(item: T) {
        if (currentItem != item) {
            currentItem = item
            onBind(item)
        }
    }
    
    protected abstract fun onBind(item: T)
    
    fun unbind() {
        currentItem = null
        onUnbind()
    }
    
    protected open fun onUnbind() {
        // Override if needed to clean up resources
    }
}

/**
 * Performance-optimized adapter base class
 */
abstract class BaseOptimizedAdapter<T : Any, VH : BaseViewHolder<T>>(
    diffCallback: DiffUtil.ItemCallback<T>
) : androidx.recyclerview.widget.ListAdapter<T, VH>(diffCallback) {

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    override fun onFailedToRecycleView(holder: VH): Boolean {
        holder.unbind()
        return super.onFailedToRecycleView(holder)
    }
}

/**
 * Smooth scrolling utilities
 */
object SmoothScrollUtils {
    
    fun smoothScrollToTop(recyclerView: RecyclerView) {
        recyclerView.smoothScrollToPosition(0)
    }
    
    fun smoothScrollToPosition(recyclerView: RecyclerView, position: Int) {
        val layoutManager = recyclerView.layoutManager
        val firstVisiblePosition = when (layoutManager) {
            is androidx.recyclerview.widget.LinearLayoutManager -> 
                layoutManager.findFirstVisibleItemPosition()
            else -> 0
        }
        
        // If the target position is far away, jump closer first then smooth scroll
        if (abs(position - firstVisiblePosition) > 10) {
            val jumpPosition = if (position > firstVisiblePosition) {
                (position - 5).coerceAtLeast(0)
            } else {
                position + 5
            }
            recyclerView.scrollToPosition(jumpPosition)
        }
        
        recyclerView.post {
            recyclerView.smoothScrollToPosition(position)
        }
    }
}

/**
 * Image loading optimizations
 */
object ImageLoadingUtils {
    
    fun calculateInSampleSize(options: android.graphics.BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
    
    fun decodeSampledBitmapFromResource(
        res: android.content.res.Resources,
        resId: Int,
        reqWidth: Int,
        reqHeight: Int
    ): android.graphics.Bitmap {
        return android.graphics.BitmapFactory.Options().run {
            inJustDecodeBounds = true
            android.graphics.BitmapFactory.decodeResource(res, resId, this)
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
            inJustDecodeBounds = false
            android.graphics.BitmapFactory.decodeResource(res, resId, this)
        }
    }
}

/**
 * View performance optimizations
 */
object ViewOptimizations {
    
    /**
     * Enables hardware acceleration for a view if beneficial
     */
    fun enableHardwareAcceleration(view: View) {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }
    
    /**
     * Disables hardware acceleration for a view
     */
    fun disableHardwareAcceleration(view: View) {
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }
    
    /**
     * Optimizes view for text rendering
     */
    fun optimizeTextView(textView: android.widget.TextView) {
        textView.includeFontPadding = false
        textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }
    
    /**
     * Pre-loads view measurements to avoid layout passes
     */
    fun preloadViewMeasurement(view: View, widthMeasureSpec: Int, heightMeasureSpec: Int) {
        view.measure(widthMeasureSpec, heightMeasureSpec)
    }
}

/**
 * Memory management utilities
 */
object MemoryUtils {
    
    /**
     * Suggests garbage collection (use sparingly)
     */
    fun suggestGC() {
        System.gc()
    }
    
    /**
     * Gets available memory info
     */
    fun getAvailableMemory(context: android.content.Context): android.app.ActivityManager.MemoryInfo {
        val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo
    }
    
    /**
     * Checks if system is in low memory state
     */
    fun isLowMemory(context: android.content.Context): Boolean {
        return getAvailableMemory(context).lowMemory
    }
}

/**
 * Layout optimization utilities
 */
object LayoutOptimizations {
    
    /**
     * Optimizes RecyclerView for performance
     */
    fun optimizeRecyclerView(recyclerView: RecyclerView) {
        // Enable item animator optimizations
        recyclerView.itemAnimator?.changeDuration = 0
        recyclerView.itemAnimator?.moveDuration = 200
        recyclerView.itemAnimator?.addDuration = 200
        recyclerView.itemAnimator?.removeDuration = 200
        
        // Optimize drawing cache
        recyclerView.setItemViewCacheSize(20)
        recyclerView.setDrawingCacheEnabled(true)
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH)
        
        // Optimize nested scrolling
        recyclerView.isNestedScrollingEnabled = false
        
        // Set stable IDs if adapter supports it
        if (recyclerView.adapter?.hasStableIds() == true) {
            recyclerView.setHasFixedSize(true)
        }
    }
    
    /**
     * Reduces over-drawing in view hierarchies
     */
    fun reduceOverdraw(view: View) {
        view.background = null
    }
    
    /**
     * Optimizes constraint layout performance
     */
    fun optimizeConstraintLayout(constraintLayout: androidx.constraintlayout.widget.ConstraintLayout) {
        constraintLayout.setOptimizationLevel(
            androidx.constraintlayout.widget.Constraints.CHAIN_OPTIMIZATIONS or
            androidx.constraintlayout.widget.Constraints.DIMENSIONS_OPTIMIZATIONS
        )
    }
}

/**
 * Animation performance utilities
 */
object AnimationOptimizations {
    
    /**
     * Creates optimized property animator
     */
    fun createOptimizedAnimator(view: View, property: String, vararg values: Float): android.animation.ObjectAnimator {
        return android.animation.ObjectAnimator.ofFloat(view, property, *values).apply {
            duration = 200L
            interpolator = android.view.animation.DecelerateInterpolator()
        }
    }
    
    /**
     * Optimizes view for animation
     */
    fun prepareForAnimation(view: View) {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }
    
    /**
     * Cleans up after animation
     */
    fun cleanupAfterAnimation(view: View) {
        view.setLayerType(View.LAYER_TYPE_NONE, null)
    }
}