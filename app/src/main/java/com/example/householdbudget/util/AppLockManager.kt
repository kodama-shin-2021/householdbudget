package com.example.householdbudget.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val securePrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            "app_lock_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val regularPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("app_lock_settings", Context.MODE_PRIVATE)
    }

    fun isAppLockEnabled(): Boolean {
        return regularPrefs.getBoolean(PREF_APP_LOCK_ENABLED, false)
    }

    fun isBiometricAuthEnabled(): Boolean {
        return regularPrefs.getBoolean(PREF_BIOMETRIC_ENABLED, false)
    }

    fun enableAppLock(pin: String) {
        securePrefs.edit()
            .putString(PREF_PIN_HASH, hashPin(pin))
            .apply()
        
        regularPrefs.edit()
            .putBoolean(PREF_APP_LOCK_ENABLED, true)
            .apply()
    }

    fun disableAppLock() {
        securePrefs.edit()
            .remove(PREF_PIN_HASH)
            .apply()
        
        regularPrefs.edit()
            .putBoolean(PREF_APP_LOCK_ENABLED, false)
            .putBoolean(PREF_BIOMETRIC_ENABLED, false)
            .apply()
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!validatePin(oldPin)) {
            return false
        }
        
        securePrefs.edit()
            .putString(PREF_PIN_HASH, hashPin(newPin))
            .apply()
        
        return true
    }

    fun validatePin(pin: String): Boolean {
        val storedHash = securePrefs.getString(PREF_PIN_HASH, null) ?: return false
        return storedHash == hashPin(pin)
    }

    fun setBiometricAuthEnabled(enabled: Boolean) {
        regularPrefs.edit()
            .putBoolean(PREF_BIOMETRIC_ENABLED, enabled)
            .apply()
    }

    fun getFailedAttempts(): Int {
        return regularPrefs.getInt(PREF_FAILED_ATTEMPTS, 0)
    }

    fun incrementFailedAttempts(): Int {
        val attempts = getFailedAttempts() + 1
        regularPrefs.edit()
            .putInt(PREF_FAILED_ATTEMPTS, attempts)
            .apply()
        return attempts
    }

    fun resetFailedAttempts() {
        regularPrefs.edit()
            .putInt(PREF_FAILED_ATTEMPTS, 0)
            .apply()
    }

    fun isTemporarilyLocked(): Boolean {
        val failedAttempts = getFailedAttempts()
        if (failedAttempts < MAX_FAILED_ATTEMPTS) return false
        
        val lockTime = regularPrefs.getLong(PREF_LOCK_TIME, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lockTime) < LOCK_DURATION_MS
    }

    fun setTemporaryLock() {
        regularPrefs.edit()
            .putLong(PREF_LOCK_TIME, System.currentTimeMillis())
            .apply()
    }

    fun getRemainingLockTime(): Long {
        val lockTime = regularPrefs.getLong(PREF_LOCK_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lockTime
        return (LOCK_DURATION_MS - elapsed).coerceAtLeast(0)
    }

    fun onAppLockSuccess() {
        resetFailedAttempts()
    }

    fun isAppInBackground(): Boolean {
        return regularPrefs.getBoolean(PREF_APP_IN_BACKGROUND, false)
    }

    fun setAppInBackground(inBackground: Boolean) {
        regularPrefs.edit()
            .putBoolean(PREF_APP_IN_BACKGROUND, inBackground)
            .apply()
    }

    fun getLastBackgroundTime(): Long {
        return regularPrefs.getLong(PREF_LAST_BACKGROUND_TIME, 0)
    }

    fun setLastBackgroundTime(time: Long) {
        regularPrefs.edit()
            .putLong(PREF_LAST_BACKGROUND_TIME, time)
            .apply()
    }

    fun shouldShowAppLock(): Boolean {
        if (!isAppLockEnabled()) return false
        
        val lastBackgroundTime = getLastBackgroundTime()
        val currentTime = System.currentTimeMillis()
        val timeInBackground = currentTime - lastBackgroundTime
        
        // Show lock screen if app was in background for more than 30 seconds
        return timeInBackground > APP_LOCK_TIMEOUT_MS
    }

    private fun hashPin(pin: String): String {
        return android.util.Base64.encodeToString(
            java.security.MessageDigest.getInstance("SHA-256")
                .digest((pin + SALT).toByteArray()),
            android.util.Base64.NO_WRAP
        )
    }

    companion object {
        private const val PREF_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val PREF_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val PREF_PIN_HASH = "pin_hash"
        private const val PREF_FAILED_ATTEMPTS = "failed_attempts"
        private const val PREF_LOCK_TIME = "lock_time"
        private const val PREF_APP_IN_BACKGROUND = "app_in_background"
        private const val PREF_LAST_BACKGROUND_TIME = "last_background_time"
        
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCK_DURATION_MS = 30 * 60 * 1000L // 30 minutes
        private const val APP_LOCK_TIMEOUT_MS = 30 * 1000L // 30 seconds
        private const val SALT = "HouseholdBudgetAppSalt2024"
    }
}