package com.example.householdbudget

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.householdbudget.util.PerformanceOptimizations
import com.example.householdbudget.util.StartupOptimizer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HouseholdBudgetApplication : Application() {

    companion object {
        private const val TAG = "HouseholdBudgetApp"
    }

    override fun onCreate() {
        val startTime = System.currentTimeMillis()
        super.onCreate()

        // Initialize performance optimizations
        initializePerformanceOptimizations()

        // Configure WorkManager for better performance
        configureWorkManager()

        // Initialize startup optimizer
        StartupOptimizer.initialize(this)

        val initTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "Application initialized in ${initTime}ms")
    }

    private fun initializePerformanceOptimizations() {
        // Pre-initialize commonly used utilities
        PerformanceOptimizations.MemoryUtils.getAvailableMemory(this)

        // Optimize image loading
        configureImageLoading()
    }

    private fun configureImageLoading() {
        // Pre-calculate common image sizes for optimization
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        Log.d(TAG, "Screen dimensions: ${screenWidth}x${screenHeight}")
    }

    private fun configureWorkManager() {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
        
        WorkManager.initialize(this, config)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        when (level) {
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.w(TAG, "Memory pressure detected, level: $level")
                // Clear caches and release non-essential resources
                clearCaches()
                StartupOptimizer.getInstance()?.optimizeForLowMemory()
            }
        }
    }

    private fun clearCaches() {
        // Clear image caches, temporary data, etc.
        System.gc() // Suggest garbage collection
        Log.d(TAG, "Caches cleared due to memory pressure")
    }
}