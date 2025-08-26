package com.example.householdbudget.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdbudget.domain.repository.SettingsRepository
import com.example.householdbudget.util.AppLockManager
import com.example.householdbudget.util.BackupManager
import com.example.householdbudget.util.BackupResult
import com.example.householdbudget.util.BiometricAuthManager
import com.example.householdbudget.util.BiometricAvailability
import com.example.householdbudget.util.CsvExportManager
import com.example.householdbudget.util.CsvExportResult
import com.example.householdbudget.util.CsvImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    private val csvExportManager: CsvExportManager,
    private val appLockManager: AppLockManager,
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    private val _uiState = MutableLiveData<SettingsUiState>(SettingsUiState.Loading)
    val uiState: LiveData<SettingsUiState> = _uiState

    // Theme settings
    private val _currentTheme = MutableLiveData<AppTheme>()
    val currentTheme: LiveData<AppTheme> = _currentTheme

    // Currency settings
    private val _currentCurrency = MutableLiveData<Currency>()
    val currentCurrency: LiveData<Currency> = _currentCurrency

    // Notification settings
    private val _regularTransactionNotifications = MutableLiveData<Boolean>()
    val regularTransactionNotifications: LiveData<Boolean> = _regularTransactionNotifications

    private val _budgetAlertNotifications = MutableLiveData<Boolean>()
    val budgetAlertNotifications: LiveData<Boolean> = _budgetAlertNotifications

    private val _goalNotifications = MutableLiveData<Boolean>()
    val goalNotifications: LiveData<Boolean> = _goalNotifications

    // Security settings
    private val _appLockEnabled = MutableLiveData<Boolean>()
    val appLockEnabled: LiveData<Boolean> = _appLockEnabled

    private val _biometricAuthEnabled = MutableLiveData<Boolean>()
    val biometricAuthEnabled: LiveData<Boolean> = _biometricAuthEnabled

    private val _biometricAvailability = MutableLiveData<BiometricAvailability>()
    val biometricAvailability: LiveData<BiometricAvailability> = _biometricAvailability

    // Backup settings
    private val _autoBackupEnabled = MutableLiveData<Boolean>()
    val autoBackupEnabled: LiveData<Boolean> = _autoBackupEnabled

    private val _lastBackupDate = MutableLiveData<Date?>()
    val lastBackupDate: LiveData<Date?> = _lastBackupDate

    // Operation results
    private val _backupResult = MutableLiveData<BackupResult?>()
    val backupResult: LiveData<BackupResult?> = _backupResult

    private val _exportResult = MutableLiveData<CsvExportResult?>()
    val exportResult: LiveData<CsvExportResult?> = _exportResult

    private val _importResult = MutableLiveData<CsvImportResult?>()
    val importResult: LiveData<CsvImportResult?> = _importResult

    init {
        loadSettings()
        checkBiometricAvailability()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // Load theme setting
                val themeValue = settingsRepository.getSetting("theme", AppTheme.SYSTEM.name)
                _currentTheme.value = AppTheme.valueOf(themeValue)

                // Load currency setting
                val currencyValue = settingsRepository.getSetting("currency", Currency.JPY.name)
                _currentCurrency.value = Currency.valueOf(currencyValue)

                // Load notification settings
                _regularTransactionNotifications.value = settingsRepository.getBooleanSetting("notifications_regular_transactions", true)
                _budgetAlertNotifications.value = settingsRepository.getBooleanSetting("notifications_budget_alerts", true)
                _goalNotifications.value = settingsRepository.getBooleanSetting("notifications_goals", true)

                // Load security settings
                _appLockEnabled.value = appLockManager.isAppLockEnabled()
                _biometricAuthEnabled.value = appLockManager.isBiometricAuthEnabled()

                // Load backup settings
                _autoBackupEnabled.value = settingsRepository.getBooleanSetting("auto_backup_enabled", true)
                val lastBackupTime = settingsRepository.getLongSetting("last_backup_time", 0L)
                _lastBackupDate.value = if (lastBackupTime > 0) Date(lastBackupTime) else null

                _uiState.value = SettingsUiState.Success
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("設定の読み込みに失敗しました")
            }
        }
    }

    private fun checkBiometricAvailability() {
        _biometricAvailability.value = biometricAuthManager.isBiometricAvailable()
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            try {
                settingsRepository.updateSetting("theme", theme.name)
                _currentTheme.value = theme
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("テーマの設定に失敗しました")
            }
        }
    }

    fun setCurrency(currency: Currency) {
        viewModelScope.launch {
            try {
                settingsRepository.updateSetting("currency", currency.name)
                _currentCurrency.value = currency
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("通貨の設定に失敗しました")
            }
        }
    }

    fun setRegularTransactionNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateBooleanSetting("notifications_regular_transactions", enabled)
                _regularTransactionNotifications.value = enabled
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("通知設定の更新に失敗しました")
            }
        }
    }

    fun setBudgetAlertNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateBooleanSetting("notifications_budget_alerts", enabled)
                _budgetAlertNotifications.value = enabled
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("通知設定の更新に失敗しました")
            }
        }
    }

    fun setGoalNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateBooleanSetting("notifications_goals", enabled)
                _goalNotifications.value = enabled
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("通知設定の更新に失敗しました")
            }
        }
    }

    fun enableAppLock(pin: String) {
        viewModelScope.launch {
            try {
                appLockManager.enableAppLock(pin)
                _appLockEnabled.value = true
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("アプリロックの設定に失敗しました")
            }
        }
    }

    fun disableAppLock() {
        viewModelScope.launch {
            try {
                appLockManager.disableAppLock()
                _appLockEnabled.value = false
                _biometricAuthEnabled.value = false
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("アプリロックの無効化に失敗しました")
            }
        }
    }

    fun setBiometricAuthEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                appLockManager.setBiometricAuthEnabled(enabled)
                _biometricAuthEnabled.value = enabled
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("生体認証の設定に失敗しました")
            }
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateBooleanSetting("auto_backup_enabled", enabled)
                _autoBackupEnabled.value = enabled
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("自動バックアップの設定に失敗しました")
            }
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            try {
                _uiState.value = SettingsUiState.Loading
                val result = backupManager.createBackup()
                _backupResult.value = result
                
                if (result is BackupResult.Success) {
                    // Update last backup time
                    settingsRepository.updateLongSetting("last_backup_time", System.currentTimeMillis())
                    _lastBackupDate.value = Date()
                }
                
                _uiState.value = SettingsUiState.Success
            } catch (e: Exception) {
                _backupResult.value = BackupResult.Error("バックアップの作成に失敗しました")
                _uiState.value = SettingsUiState.Success
            }
        }
    }

    fun restoreFromBackup(filePath: String) {
        viewModelScope.launch {
            try {
                _uiState.value = SettingsUiState.Loading
                val result = backupManager.restoreFromBackup(filePath)
                _backupResult.value = result
                _uiState.value = SettingsUiState.Success
                
                if (result is BackupResult.Success) {
                    // Reload settings after restore
                    loadSettings()
                }
            } catch (e: Exception) {
                _backupResult.value = BackupResult.Error("バックアップの復元に失敗しました")
                _uiState.value = SettingsUiState.Success
            }
        }
    }

    fun exportDataToCsv(startDate: Date? = null, endDate: Date? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = SettingsUiState.Loading
                val result = csvExportManager.exportTransactionsToCsv(startDate, endDate)
                _exportResult.value = result
                _uiState.value = SettingsUiState.Success
            } catch (e: Exception) {
                _exportResult.value = CsvExportResult.Error("CSVエクスポートに失敗しました")
                _uiState.value = SettingsUiState.Success
            }
        }
    }

    fun importDataFromCsv(filePath: String) {
        viewModelScope.launch {
            try {
                _uiState.value = SettingsUiState.Loading
                val result = csvExportManager.importTransactionsFromCsv(filePath)
                _importResult.value = result
                _uiState.value = SettingsUiState.Success
            } catch (e: Exception) {
                _importResult.value = CsvImportResult.Error("CSVインポートに失敗しました")
                _uiState.value = SettingsUiState.Success
            }
        }
    }

    fun clearBackupResult() {
        _backupResult.value = null
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    fun clearImportResult() {
        _importResult.value = null
    }

    fun getThemeDisplayName(theme: AppTheme): String {
        return when (theme) {
            AppTheme.LIGHT -> "ライトテーマ"
            AppTheme.DARK -> "ダークテーマ"
            AppTheme.SYSTEM -> "システムのデフォルト"
        }
    }

    fun getCurrencyDisplayName(currency: Currency): String {
        return when (currency) {
            Currency.JPY -> "日本円 (¥)"
            Currency.USD -> "米ドル ($)"
            Currency.EUR -> "ユーロ (€)"
        }
    }
}

sealed class SettingsUiState {
    object Loading : SettingsUiState()
    object Success : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

enum class Currency {
    JPY,
    USD,
    EUR
}