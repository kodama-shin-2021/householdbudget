package com.example.householdbudget.util

import android.content.Context
import android.net.Uri
import com.example.householdbudget.data.entity.Transaction
import com.example.householdbudget.data.entity.TransactionType
import com.example.householdbudget.domain.repository.CategoryRepository
import com.example.householdbudget.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {

    suspend fun exportTransactionsToCsv(
        startDate: Date? = null,
        endDate: Date? = null
    ): CsvExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val transactions = if (startDate != null && endDate != null) {
                    transactionRepository.getTransactionsByDateRange(startDate, endDate)
                } else {
                    transactionRepository.getAllTransactionsDirect()
                }

                val categories = categoryRepository.getAllCategoriesDirect()
                val categoryMap = categories.associateBy { it.id }

                val csvFile = createCsvFile("transactions")
                writeCsvFile(csvFile, transactions, categoryMap)
                
                CsvExportResult.Success(csvFile.absolutePath, transactions.size)
            } catch (e: Exception) {
                CsvExportResult.Error(e.message ?: "CSVエクスポートに失敗しました")
            }
        }
    }

    suspend fun exportTransactionsToCsvUri(
        uri: Uri,
        startDate: Date? = null,
        endDate: Date? = null
    ): CsvExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val transactions = if (startDate != null && endDate != null) {
                    transactionRepository.getTransactionsByDateRange(startDate, endDate)
                } else {
                    transactionRepository.getAllTransactionsDirect()
                }

                val categories = categoryRepository.getAllCategoriesDirect()
                val categoryMap = categories.associateBy { it.id }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                        writeCsvContent(writer, transactions, categoryMap)
                    }
                } ?: throw Exception("ファイルに書き込めませんでした")

                CsvExportResult.Success("エクスポート完了", transactions.size)
            } catch (e: Exception) {
                CsvExportResult.Error(e.message ?: "CSVエクスポートに失敗しました")
            }
        }
    }

    suspend fun importTransactionsFromCsv(csvFilePath: String): CsvImportResult {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(csvFilePath)
                if (!file.exists()) {
                    return@withContext CsvImportResult.Error("CSVファイルが見つかりません")
                }

                val lines = file.readLines()
                if (lines.isEmpty()) {
                    return@withContext CsvImportResult.Error("CSVファイルが空です")
                }

                // Skip header line
                val dataLines = lines.drop(1)
                val transactions = mutableListOf<Transaction>()
                val categories = categoryRepository.getAllCategoriesDirect()
                val categoryMap = categories.associateBy { it.name }

                for ((index, line) in dataLines.withIndex()) {
                    try {
                        val transaction = parseCsvLine(line, categoryMap)
                        transactions.add(transaction)
                    } catch (e: Exception) {
                        return@withContext CsvImportResult.Error("行 ${index + 2} の解析に失敗しました: ${e.message}")
                    }
                }

                // Insert transactions
                transactions.forEach { transactionRepository.insertTransaction(it) }

                CsvImportResult.Success(transactions.size)
            } catch (e: Exception) {
                CsvImportResult.Error(e.message ?: "CSVインポートに失敗しました")
            }
        }
    }

    suspend fun importTransactionsFromCsvUri(uri: Uri): CsvImportResult {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext CsvImportResult.Error("ファイルを開けませんでした")

                val lines = inputStream.bufferedReader().readLines()
                if (lines.isEmpty()) {
                    return@withContext CsvImportResult.Error("CSVファイルが空です")
                }

                // Skip header line
                val dataLines = lines.drop(1)
                val transactions = mutableListOf<Transaction>()
                val categories = categoryRepository.getAllCategoriesDirect()
                val categoryMap = categories.associateBy { it.name }

                for ((index, line) in dataLines.withIndex()) {
                    try {
                        val transaction = parseCsvLine(line, categoryMap)
                        transactions.add(transaction)
                    } catch (e: Exception) {
                        return@withContext CsvImportResult.Error("行 ${index + 2} の解析に失敗しました: ${e.message}")
                    }
                }

                // Insert transactions
                transactions.forEach { transactionRepository.insertTransaction(it) }

                CsvImportResult.Success(transactions.size)
            } catch (e: Exception) {
                CsvImportResult.Error(e.message ?: "CSVインポートに失敗しました")
            }
        }
    }

    private fun createCsvFile(prefix: String): File {
        val exportDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return File(exportDir, "${prefix}_$timestamp.csv")
    }

    private fun writeCsvFile(
        file: File,
        transactions: List<Transaction>,
        categoryMap: Map<Long, com.example.householdbudget.data.entity.Category>
    ) {
        FileWriter(file, Charsets.UTF_8).use { writer ->
            writeCsvContent(writer, transactions, categoryMap)
        }
    }

    private fun writeCsvContent(
        writer: java.io.Writer,
        transactions: List<Transaction>,
        categoryMap: Map<Long, com.example.householdbudget.data.entity.Category>
    ) {
        // Write BOM for Excel compatibility
        writer.write('\uFEFF')

        // Write header
        writer.write("日付,タイプ,カテゴリ,金額,説明,メモ\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Write data
        for (transaction in transactions) {
            val category = categoryMap[transaction.categoryId]
            val line = buildString {
                append(dateFormat.format(transaction.date))
                append(",")
                append(when (transaction.type) {
                    TransactionType.INCOME -> "収入"
                    TransactionType.EXPENSE -> "支出"
                })
                append(",")
                append(escapeCsvField(category?.name ?: "不明"))
                append(",")
                append(transaction.amount.toPlainString())
                append(",")
                append(escapeCsvField(transaction.description ?: ""))
                append(",")
                append(escapeCsvField(transaction.memo ?: ""))
                append("\n")
            }
            writer.write(line)
        }
    }

    private fun parseCsvLine(
        line: String,
        categoryMap: Map<String, com.example.householdbudget.data.entity.Category>
    ): Transaction {
        val parts = parseCsvFields(line)
        
        if (parts.size < 5) {
            throw IllegalArgumentException("CSVの列数が不足しています")
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = try {
            dateFormat.parse(parts[0]) ?: Date()
        } catch (e: Exception) {
            throw IllegalArgumentException("日付の形式が正しくありません: ${parts[0]}")
        }

        val type = when (parts[1]) {
            "収入" -> TransactionType.INCOME
            "支出" -> TransactionType.EXPENSE
            else -> throw IllegalArgumentException("タイプが正しくありません: ${parts[1]}")
        }

        val categoryName = parts[2]
        val category = categoryMap[categoryName]
            ?: throw IllegalArgumentException("カテゴリが見つかりません: $categoryName")

        val amount = try {
            java.math.BigDecimal(parts[3])
        } catch (e: Exception) {
            throw IllegalArgumentException("金額の形式が正しくありません: ${parts[3]}")
        }

        val description = if (parts.size > 4) parts[4] else null
        val memo = if (parts.size > 5) parts[5] else null

        return Transaction(
            amount = amount,
            type = type,
            categoryId = category.id,
            date = date,
            description = description.takeIf { it?.isNotBlank() == true },
            memo = memo.takeIf { it?.isNotBlank() == true }
        )
    }

    private fun parseCsvFields(line: String): List<String> {
        val fields = mutableListOf<String>()
        var currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' && !inQuotes -> inQuotes = true
                char == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        currentField.append('"')
                        i++ // Skip next quote
                    } else {
                        inQuotes = false
                    }
                }
                char == ',' && !inQuotes -> {
                    fields.add(currentField.toString())
                    currentField = StringBuilder()
                }
                else -> currentField.append(char)
            }
            i++
        }

        fields.add(currentField.toString())
        return fields
    }

    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }
}

sealed class CsvExportResult {
    data class Success(val filePath: String, val recordCount: Int) : CsvExportResult()
    data class Error(val message: String) : CsvExportResult()
}

sealed class CsvImportResult {
    data class Success(val recordCount: Int) : CsvImportResult()
    data class Error(val message: String) : CsvImportResult()
}