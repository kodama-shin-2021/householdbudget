package com.example.householdbudget.util

import android.content.Context
import android.net.Uri
import com.example.householdbudget.data.entity.*
import com.example.householdbudget.domain.repository.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.reflect.Type
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val regularTransactionRepository: RegularTransactionRepository,
    private val settingsRepository: SettingsRepository
) {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, DateSerializer())
        .registerTypeAdapter(Date::class.java, DateDeserializer())
        .registerTypeAdapter(BigDecimal::class.java, BigDecimalSerializer())
        .registerTypeAdapter(BigDecimal::class.java, BigDecimalDeserializer())
        .setPrettyPrinting()
        .create()

    suspend fun createBackup(): BackupResult {
        return withContext(Dispatchers.IO) {
            try {
                val backupData = collectAllData()
                val backupFile = createBackupFile()
                writeBackupToFile(backupData, backupFile)
                BackupResult.Success(backupFile.absolutePath)
            } catch (e: Exception) {
                BackupResult.Error(e.message ?: "バックアップの作成に失敗しました")
            }
        }
    }

    suspend fun restoreFromBackup(backupFilePath: String): BackupResult {
        return withContext(Dispatchers.IO) {
            try {
                val backupFile = File(backupFilePath)
                if (!backupFile.exists()) {
                    return@withContext BackupResult.Error("バックアップファイルが見つかりません")
                }

                val backupData = readBackupFromFile(backupFile)
                restoreAllData(backupData)
                BackupResult.Success("データの復元が完了しました")
            } catch (e: Exception) {
                BackupResult.Error(e.message ?: "バックアップの復元に失敗しました")
            }
        }
    }

    suspend fun restoreFromUri(uri: Uri): BackupResult {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext BackupResult.Error("ファイルを開けませんでした")

                val backupData = if (uri.toString().endsWith(".zip")) {
                    readBackupFromZip(inputStream)
                } else {
                    val reader = InputStreamReader(inputStream)
                    gson.fromJson(reader, BackupData::class.java)
                }

                restoreAllData(backupData)
                BackupResult.Success("データの復元が完了しました")
            } catch (e: Exception) {
                BackupResult.Error(e.message ?: "バックアップの復元に失敗しました")
            }
        }
    }

    private suspend fun collectAllData(): BackupData {
        return BackupData(
            version = BACKUP_VERSION,
            timestamp = Date(),
            transactions = transactionRepository.getAllTransactionsDirect(),
            categories = categoryRepository.getAllCategoriesDirect(),
            budgets = budgetRepository.getAllBudgetsDirect(),
            goals = goalRepository.getAllGoalsDirect(),
            regularTransactions = regularTransactionRepository.getAllRegularTransactionsDirect(),
            settings = settingsRepository.getAllSettingsDirect()
        )
    }

    private suspend fun restoreAllData(backupData: BackupData) {
        // Clear existing data
        transactionRepository.deleteAllTransactions()
        categoryRepository.deleteAllCategories()
        budgetRepository.deleteAllBudgets()
        goalRepository.deleteAllGoals()
        regularTransactionRepository.deleteAllRegularTransactions()
        settingsRepository.deleteAllSettings()

        // Restore data
        backupData.categories.forEach { categoryRepository.insertCategory(it) }
        backupData.transactions.forEach { transactionRepository.insertTransaction(it) }
        backupData.budgets.forEach { budgetRepository.insertBudget(it) }
        backupData.goals.forEach { goalRepository.insertGoal(it) }
        backupData.regularTransactions.forEach { regularTransactionRepository.insertRegularTransaction(it) }
        backupData.settings.forEach { settingsRepository.updateSetting(it.key, it.value) }
    }

    private fun createBackupFile(): File {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return File(backupDir, "household_budget_backup_$timestamp.zip")
    }

    private fun writeBackupToFile(backupData: BackupData, file: File) {
        ZipOutputStream(FileOutputStream(file)).use { zipOut ->
            // Write main backup data
            val backupEntry = ZipEntry("backup.json")
            zipOut.putNextEntry(backupEntry)
            val writer = OutputStreamWriter(zipOut)
            gson.toJson(backupData, writer)
            writer.flush()
            zipOut.closeEntry()

            // Add metadata
            val metadataEntry = ZipEntry("metadata.json")
            zipOut.putNextEntry(metadataEntry)
            val metadata = BackupMetadata(
                version = BACKUP_VERSION,
                createdAt = Date(),
                deviceInfo = getDeviceInfo(),
                dataSize = backupData.calculateSize()
            )
            val metadataWriter = OutputStreamWriter(zipOut)
            gson.toJson(metadata, metadataWriter)
            metadataWriter.flush()
            zipOut.closeEntry()
        }
    }

    private fun readBackupFromFile(file: File): BackupData {
        return if (file.name.endsWith(".zip")) {
            readBackupFromZip(FileInputStream(file))
        } else {
            // Legacy JSON format
            val reader = InputStreamReader(FileInputStream(file))
            gson.fromJson(reader, BackupData::class.java)
        }
    }

    private fun readBackupFromZip(inputStream: java.io.InputStream): BackupData {
        ZipInputStream(inputStream).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                if (entry.name == "backup.json") {
                    val reader = InputStreamReader(zipIn)
                    return gson.fromJson(reader, BackupData::class.java)
                }
                entry = zipIn.nextEntry
            }
            throw IllegalArgumentException("バックアップデータが見つかりません")
        }
    }

    private fun getDeviceInfo(): String {
        return "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
    }

    // Type adapters for Gson
    private class DateSerializer : JsonSerializer<Date> {
        override fun serialize(date: Date?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(date?.time ?: 0)
        }
    }

    private class DateDeserializer : JsonDeserializer<Date> {
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Date {
            return Date(json?.asLong ?: 0)
        }
    }

    private class BigDecimalSerializer : JsonSerializer<BigDecimal> {
        override fun serialize(decimal: BigDecimal?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(decimal?.toString() ?: "0")
        }
    }

    private class BigDecimalDeserializer : JsonDeserializer<BigDecimal> {
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): BigDecimal {
            return BigDecimal(json?.asString ?: "0")
        }
    }

    companion object {
        private const val BACKUP_VERSION = 1
    }
}

data class BackupData(
    val version: Int,
    val timestamp: Date,
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val budgets: List<Budget>,
    val goals: List<Goal>,
    val regularTransactions: List<RegularTransaction>,
    val settings: List<Settings>
) {
    fun calculateSize(): Long {
        return transactions.size + categories.size + budgets.size + goals.size + regularTransactions.size + settings.size
    }
}

data class BackupMetadata(
    val version: Int,
    val createdAt: Date,
    val deviceInfo: String,
    val dataSize: Long
)

sealed class BackupResult {
    data class Success(val message: String) : BackupResult()
    data class Error(val message: String) : BackupResult()
}