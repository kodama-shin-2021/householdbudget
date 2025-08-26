package com.example.householdbudget.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.householdbudget.data.entity.Settings
import com.example.householdbudget.data.entity.SettingType

@Dao
interface SettingsDao {
    
    @Query("SELECT * FROM settings ORDER BY key ASC")
    fun getAllSettings(): LiveData<List<Settings>>
    
    @Query("SELECT * FROM settings WHERE key = :key")
    suspend fun getSettingByKey(key: String): Settings?
    
    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun getSettingValue(key: String): String?
    
    @Query("SELECT * FROM settings WHERE type = :type")
    fun getSettingsByType(type: SettingType): LiveData<List<Settings>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSetting(setting: Settings)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: List<Settings>)
    
    @Update
    suspend fun updateSetting(setting: Settings)
    
    @Delete
    suspend fun deleteSetting(setting: Settings)
    
    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun deleteSettingByKey(key: String)
    
    @Query("DELETE FROM settings")
    suspend fun deleteAllSettings()
}