package com.example.householdbudget.data.repository

import androidx.lifecycle.LiveData
import com.example.householdbudget.data.dao.SettingsDao
import com.example.householdbudget.data.database.DefaultData
import com.example.householdbudget.data.entity.Settings
import com.example.householdbudget.data.entity.SettingType
import com.example.householdbudget.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao
) : SettingsRepository {

    override fun getAllSettings(): LiveData<List<Settings>> {
        return settingsDao.getAllSettings()
    }

    override suspend fun getSettingByKey(key: String): Settings? {
        return withContext(Dispatchers.IO) {
            settingsDao.getSettingByKey(key)
        }
    }

    override suspend fun getSettingValue(key: String): String? {
        return withContext(Dispatchers.IO) {
            settingsDao.getSettingValue(key)
        }
    }

    override fun getSettingsByType(type: SettingType): LiveData<List<Settings>> {
        return settingsDao.getSettingsByType(type)
    }

    override suspend fun insertOrUpdateSetting(setting: Settings) {
        withContext(Dispatchers.IO) {
            settingsDao.insertOrUpdateSetting(setting.copy(updatedAt = Date()))
        }
    }

    override suspend fun insertOrUpdateSettings(settings: List<Settings>) {
        withContext(Dispatchers.IO) {
            val updatedSettings = settings.map { it.copy(updatedAt = Date()) }
            settingsDao.insertOrUpdateSettings(updatedSettings)
        }
    }

    override suspend fun updateSetting(setting: Settings) {
        withContext(Dispatchers.IO) {
            settingsDao.updateSetting(setting.copy(updatedAt = Date()))
        }
    }

    override suspend fun deleteSetting(setting: Settings) {
        withContext(Dispatchers.IO) {
            settingsDao.deleteSetting(setting)
        }
    }

    override suspend fun deleteSettingByKey(key: String) {
        withContext(Dispatchers.IO) {
            settingsDao.deleteSettingByKey(key)
        }
    }

    override suspend fun deleteAllSettings() {
        withContext(Dispatchers.IO) {
            settingsDao.deleteAllSettings()
        }
    }

    override suspend fun initializeDefaultSettings() {
        withContext(Dispatchers.IO) {
            val defaultSettings = DefaultData.getDefaultSettings()
            settingsDao.insertOrUpdateSettings(defaultSettings)
        }
    }

    override suspend fun getDefaultCurrency(): String {
        return getSettingValue("default_currency") ?: "JPY"
    }

    override suspend fun setDefaultCurrency(currency: String) {
        insertOrUpdateSetting(
            Settings(
                key = "default_currency",
                value = currency,
                type = SettingType.STRING
            )
        )
    }

    override suspend fun getBudgetAlertThreshold(): Float {
        return getSettingValue("budget_alert_threshold")?.toFloatOrNull() ?: 0.8f
    }

    override suspend fun setBudgetAlertThreshold(threshold: Float) {
        insertOrUpdateSetting(
            Settings(
                key = "budget_alert_threshold",
                value = threshold.toString(),
                type = SettingType.FLOAT
            )
        )
    }

    override suspend fun isNotificationEnabled(): Boolean {
        return getSettingValue("enable_notifications")?.toBooleanStrictOrNull() ?: true
    }

    override suspend fun setNotificationEnabled(enabled: Boolean) {
        insertOrUpdateSetting(
            Settings(
                key = "enable_notifications",
                value = enabled.toString(),
                type = SettingType.BOOLEAN
            )
        )
    }

    override suspend fun getNotificationTime(): String {
        return getSettingValue("notification_time") ?: "20:00"
    }

    override suspend fun setNotificationTime(time: String) {
        insertOrUpdateSetting(
            Settings(
                key = "notification_time",
                value = time,
                type = SettingType.STRING
            )
        )
    }

    override suspend fun getFirstDayOfWeek(): Int {
        return getSettingValue("first_day_of_week")?.toIntOrNull() ?: 1 // Monday
    }

    override suspend fun setFirstDayOfWeek(day: Int) {
        insertOrUpdateSetting(
            Settings(
                key = "first_day_of_week",
                value = day.toString(),
                type = SettingType.INT
            )
        )
    }

    override suspend fun isDarkModeEnabled(): Boolean {
        return getSettingValue("enable_dark_mode")?.toBooleanStrictOrNull() ?: false
    }

    override suspend fun setDarkModeEnabled(enabled: Boolean) {
        insertOrUpdateSetting(
            Settings(
                key = "enable_dark_mode",
                value = enabled.toString(),
                type = SettingType.BOOLEAN
            )
        )
    }
}