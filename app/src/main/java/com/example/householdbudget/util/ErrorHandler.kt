package com.example.householdbudget.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import android.view.View
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.sql.SQLException

object ErrorHandler {

    private const val TAG = "ErrorHandler"

    fun handleError(
        context: Context,
        throwable: Throwable,
        userMessage: String? = null,
        showToUser: Boolean = true
    ) {
        Log.e(TAG, "Error occurred: ${throwable.message}", throwable)

        val message = userMessage ?: getErrorMessage(throwable)
        
        if (showToUser) {
            showErrorToUser(context, message)
        }
    }

    fun handleErrorWithSnackbar(
        view: View,
        throwable: Throwable,
        userMessage: String? = null,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        Log.e(TAG, "Error occurred: ${throwable.message}", throwable)

        val message = userMessage ?: getErrorMessage(throwable)
        
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        
        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action.invoke() }
        }
        
        snackbar.show()
    }

    fun getErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is IOException -> "ネットワークエラーが発生しました"
            is SocketTimeoutException -> "接続がタイムアウトしました"
            is UnknownHostException -> "サーバーに接続できません"
            is SQLException -> "データベースエラーが発生しました"
            is IllegalArgumentException -> "無効な入力データです"
            is SecurityException -> "アクセス権限がありません"
            is OutOfMemoryError -> "メモリ不足です"
            is NumberFormatException -> "数値形式が正しくありません"
            else -> "予期しないエラーが発生しました: ${throwable.message}"
        }
    }

    fun showErrorToUser(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    fun logWarning(tag: String, message: String) {
        Log.w(tag, message)
    }

    fun logInfo(tag: String, message: String) {
        Log.i(tag, message)
    }

    fun isNetworkError(throwable: Throwable): Boolean {
        return throwable is IOException ||
               throwable is SocketTimeoutException ||
               throwable is UnknownHostException
    }

    fun isDatabaseError(throwable: Throwable): Boolean {
        return throwable is SQLException
    }

    fun isValidationError(throwable: Throwable): Boolean {
        return throwable is IllegalArgumentException ||
               throwable is NumberFormatException
    }

    fun handleNetworkError(context: Context, throwable: Throwable) {
        val message = when (throwable) {
            is SocketTimeoutException -> "接続がタイムアウトしました。しばらくしてからもう一度お試しください。"
            is UnknownHostException -> "インターネット接続を確認してください。"
            else -> "ネットワークエラーが発生しました。接続を確認してください。"
        }
        
        showErrorToUser(context, message)
        logError(TAG, "Network error", throwable)
    }

    fun handleDatabaseError(context: Context, throwable: Throwable, operation: String) {
        val message = "${operation}中にエラーが発生しました。アプリを再起動してください。"
        showErrorToUser(context, message)
        logError(TAG, "Database error during $operation", throwable)
    }

    fun handleValidationError(context: Context, errors: List<String>) {
        val message = if (errors.size == 1) {
            errors.first()
        } else {
            "入力に以下の問題があります:\n${errors.joinToString("\n• ", "• ")}"
        }
        showErrorToUser(context, message)
        logWarning(TAG, "Validation errors: $errors")
    }

    fun createRetryAction(retryFunction: () -> Unit): (() -> Unit) {
        return {
            try {
                retryFunction.invoke()
            } catch (e: Exception) {
                logError(TAG, "Retry action failed", e)
            }
        }
    }

    inline fun <T> safeCall(
        context: Context? = null,
        showError: Boolean = true,
        defaultValue: T? = null,
        action: () -> T
    ): T? {
        return try {
            action.invoke()
        } catch (e: Exception) {
            if (context != null && showError) {
                handleError(context, e, showToUser = true)
            } else {
                logError(TAG, "Safe call failed", e)
            }
            defaultValue
        }
    }

    inline fun <T> safeCallAsync(
        context: Context? = null,
        showError: Boolean = true,
        crossinline onError: (Throwable) -> Unit = {},
        crossinline action: suspend () -> T
    ): suspend () -> T? {
        return {
            try {
                action.invoke()
            } catch (e: Exception) {
                if (context != null && showError) {
                    handleError(context, e, showToUser = true)
                } else {
                    logError(TAG, "Async safe call failed", e)
                }
                onError.invoke(e)
                null
            }
        }
    }
}