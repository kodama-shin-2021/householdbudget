package com.example.householdbudget.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.householdbudget.BuildConfig
import com.example.householdbudget.databinding.FragmentSettingsBinding
import com.example.householdbudget.util.BackupResult
import com.example.householdbudget.util.BiometricAvailability
import com.example.householdbudget.util.BiometricAuthManager
import com.example.householdbudget.util.CsvExportResult
import com.example.householdbudget.util.CsvImportResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    private val backupFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val filePath = getPathFromUri(uri)
                if (filePath != null) {
                    viewModel.restoreFromBackup(filePath)
                } else {
                    showError("ファイルパスを取得できませんでした")
                }
            }
        }
    }

    private val csvFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val filePath = getPathFromUri(uri)
                if (filePath != null) {
                    viewModel.importDataFromCsv(filePath)
                } else {
                    showError("ファイルパスを取得できませんでした")
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
        setupClickListeners()
    }

    private fun setupViews() {
        binding.tvAppVersion.text = BuildConfig.VERSION_NAME
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SettingsUiState.Loading -> {
                    // Show loading state
                }
                is SettingsUiState.Success -> {
                    // Hide loading state
                }
                is SettingsUiState.Error -> {
                    showError(state.message)
                }
            }
        }

        viewModel.currentTheme.observe(viewLifecycleOwner) { theme ->
            binding.tvThemeValue.text = viewModel.getThemeDisplayName(theme)
        }

        viewModel.currentCurrency.observe(viewLifecycleOwner) { currency ->
            binding.tvCurrencyValue.text = viewModel.getCurrencyDisplayName(currency)
        }

        viewModel.regularTransactionNotifications.observe(viewLifecycleOwner) { enabled ->
            binding.switchRegularTransactionNotifications.isChecked = enabled
        }

        viewModel.budgetAlertNotifications.observe(viewLifecycleOwner) { enabled ->
            binding.switchBudgetAlertNotifications.isChecked = enabled
        }

        viewModel.goalNotifications.observe(viewLifecycleOwner) { enabled ->
            binding.switchGoalNotifications.isChecked = enabled
        }

        viewModel.appLockEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchAppLock.isChecked = enabled
            binding.layoutBiometricAuth.visibility = if (enabled) View.VISIBLE else View.GONE
        }

        viewModel.biometricAuthEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchBiometricAuth.isChecked = enabled
        }

        viewModel.biometricAvailability.observe(viewLifecycleOwner) { availability ->
            binding.switchBiometricAuth.isEnabled = availability == BiometricAvailability.AVAILABLE
        }

        viewModel.autoBackupEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchAutoBackup.isChecked = enabled
        }

        viewModel.backupResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                when (it) {
                    is BackupResult.Success -> showSuccess(it.message)
                    is BackupResult.Error -> showError(it.message)
                }
                viewModel.clearBackupResult()
            }
        }

        viewModel.exportResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                when (it) {
                    is CsvExportResult.Success -> showSuccess("${it.recordCount}件のデータをエクスポートしました")
                    is CsvExportResult.Error -> showError(it.message)
                }
                viewModel.clearExportResult()
            }
        }

        viewModel.importResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                when (it) {
                    is CsvImportResult.Success -> showSuccess("${it.recordCount}件のデータをインポートしました")
                    is CsvImportResult.Error -> showError(it.message)
                }
                viewModel.clearImportResult()
            }
        }
    }

    private fun setupClickListeners() {
        binding.layoutThemeSetting.setOnClickListener {
            showThemeSelectionDialog()
        }

        binding.layoutCurrencySetting.setOnClickListener {
            showCurrencySelectionDialog()
        }

        binding.switchRegularTransactionNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setRegularTransactionNotifications(isChecked)
        }

        binding.switchBudgetAlertNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBudgetAlertNotifications(isChecked)
        }

        binding.switchGoalNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setGoalNotifications(isChecked)
        }

        binding.switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showPinSetupDialog()
            } else {
                showAppLockDisableConfirmation()
            }
        }

        binding.switchBiometricAuth.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && biometricAuthManager.isBiometricAvailable() == BiometricAvailability.AVAILABLE) {
                authenticateWithBiometric {
                    viewModel.setBiometricAuthEnabled(true)
                }
            } else {
                viewModel.setBiometricAuthEnabled(false)
            }
        }

        binding.switchAutoBackup.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoBackupEnabled(isChecked)
        }

        binding.layoutCreateBackup.setOnClickListener {
            viewModel.createBackup()
        }

        binding.layoutRestoreBackup.setOnClickListener {
            showBackupFileDialog()
        }

        binding.layoutExportData.setOnClickListener {
            viewModel.exportDataToCsv()
        }

        binding.layoutPrivacyPolicy.setOnClickListener {
            // Open privacy policy URL
        }
    }

    private fun showThemeSelectionDialog() {
        val themes = AppTheme.values()
        val themeNames = themes.map { viewModel.getThemeDisplayName(it) }.toTypedArray()
        val currentTheme = viewModel.currentTheme.value ?: AppTheme.SYSTEM
        val currentIndex = themes.indexOf(currentTheme)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("テーマを選択")
            .setSingleChoiceItems(themeNames, currentIndex) { dialog, which ->
                viewModel.setTheme(themes[which])
                dialog.dismiss()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun showCurrencySelectionDialog() {
        val currencies = Currency.values()
        val currencyNames = currencies.map { viewModel.getCurrencyDisplayName(it) }.toTypedArray()
        val currentCurrency = viewModel.currentCurrency.value ?: Currency.JPY
        val currentIndex = currencies.indexOf(currentCurrency)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("通貨を選択")
            .setSingleChoiceItems(currencyNames, currentIndex) { dialog, which ->
                viewModel.setCurrency(currencies[which])
                dialog.dismiss()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun showPinSetupDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(com.example.householdbudget.R.layout.dialog_pin_setup, null)
        
        val pinInput = dialogView.findViewById<TextInputEditText>(com.example.householdbudget.R.id.etPin)
        val confirmPinInput = dialogView.findViewById<TextInputEditText>(com.example.householdbudget.R.id.etConfirmPin)
        val pinLayout = dialogView.findViewById<TextInputLayout>(com.example.householdbudget.R.id.layoutPin)
        val confirmPinLayout = dialogView.findViewById<TextInputLayout>(com.example.householdbudget.R.id.layoutConfirmPin)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("PINを設定")
            .setView(dialogView)
            .setPositiveButton("設定") { _, _ ->
                val pin = pinInput.text.toString()
                val confirmPin = confirmPinInput.text.toString()

                when {
                    pin.length < 4 -> {
                        pinLayout.error = "PINは4桁以上で入力してください"
                        binding.switchAppLock.isChecked = false
                        return@setPositiveButton
                    }
                    pin != confirmPin -> {
                        confirmPinLayout.error = "PINが一致しません"
                        binding.switchAppLock.isChecked = false
                        return@setPositiveButton
                    }
                    else -> {
                        viewModel.enableAppLock(pin)
                    }
                }
            }
            .setNegativeButton("キャンセル") { _, _ ->
                binding.switchAppLock.isChecked = false
            }
            .setOnCancelListener {
                binding.switchAppLock.isChecked = false
            }
            .show()
    }

    private fun showAppLockDisableConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("アプリロックを無効化")
            .setMessage("アプリロックを無効化しますか？生体認証も同時に無効化されます。")
            .setPositiveButton("無効化") { _, _ ->
                viewModel.disableAppLock()
            }
            .setNegativeButton("キャンセル") { _, _ ->
                binding.switchAppLock.isChecked = true
            }
            .show()
    }

    private fun authenticateWithBiometric(onSuccess: () -> Unit) {
        biometricAuthManager.authenticate(
            fragment = this,
            title = "生体認証を有効化",
            subtitle = "生体認証を設定するために認証してください",
            onSuccess = onSuccess,
            onError = { error ->
                binding.switchBiometricAuth.isChecked = false
                showError("生体認証に失敗しました: $error")
            },
            onCancel = {
                binding.switchBiometricAuth.isChecked = false
            }
        )
    }

    private fun showBackupFileDialog() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/json"))
        }
        backupFilePickerLauncher.launch(intent)
    }

    private fun getPathFromUri(uri: Uri): String? {
        // For simplicity, return the URI string
        // In a real app, you might want to copy the file to internal storage
        return uri.toString()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(com.example.householdbudget.R.color.success_500, null))
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(com.example.householdbudget.R.color.error_500, null))
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}