package com.example.householdbudget.domain.repository

import androidx.lifecycle.LiveData
import com.example.householdbudget.data.entity.Settings
import com.example.householdbudget.data.entity.SettingType

interface SettingsRepository {
    
    fun getAllSettings(): LiveData<List<Settings>>
    
    suspend fun getSettingByKey(key: String): Settings?
    
    suspend fun getSettingValue(key: String): String?
    
    fun getSettingsByType(type: SettingType): LiveData<List<Settings>>
    
    suspend fun insertOrUpdateSetting(setting: Settings)
    
    suspend fun insertOrUpdateSettings(settings: List<Settings>)
    
    suspend fun updateSetting(setting: Settings)
    
    suspend fun deleteSetting(setting: Settings)
    
    suspend fun deleteSettingByKey(key: String)
    
    suspend fun deleteAllSettings()
    
    suspend fun initializeDefaultSettings()
    
    // Convenience methods for common settings
    suspend fun getDefaultCurrency(): String
    
    suspend fun setDefaultCurrency(currency: String)
    
    suspend fun getBudgetAlertThreshold(): Float
    
    suspend fun setBudgetAlertThreshold(threshold: Float)
    
    suspend fun isNotificationEnabled(): Boolean
    
    suspend fun setNotificationEnabled(enabled: Boolean)
    
    suspend fun getNotificationTime(): String
    
    suspend fun setNotificationTime(time: String)
    
    suspend fun getFirstDayOfWeek(): Int
    
    suspend fun setFirstDayOfWeek(day: Int)
    
    suspend fun isDarkModeEnabled(): Boolean
    
    suspend fun setDarkModeEnabled(enabled: Boolean)
}