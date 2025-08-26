package com.example.householdbudget.util

import java.math.BigDecimal
import java.util.regex.Pattern

object ValidationUtils {

    fun validateAmount(amount: BigDecimal?): ValidationResult {
        return when {
            amount == null -> ValidationResult.Error("金額を入力してください")
            amount <= BigDecimal.ZERO -> ValidationResult.Error("金額は0より大きい値を入力してください")
            amount > BigDecimal("999999999.99") -> ValidationResult.Error("金額が上限を超えています")
            else -> ValidationResult.Success
        }
    }

    fun validateDescription(description: String?): ValidationResult {
        return when {
            description.isNullOrBlank() -> ValidationResult.Error("説明を入力してください")
            description.length > 200 -> ValidationResult.Error("説明は200文字以内で入力してください")
            else -> ValidationResult.Success
        }
    }

    fun validateCategoryId(categoryId: Long?): ValidationResult {
        return when {
            categoryId == null || categoryId <= 0 -> ValidationResult.Error("カテゴリを選択してください")
            else -> ValidationResult.Success
        }
    }

    fun validateBudgetAmount(amount: BigDecimal?): ValidationResult {
        return when {
            amount == null -> ValidationResult.Error("予算金額を入力してください")
            amount <= BigDecimal.ZERO -> ValidationResult.Error("予算金額は0より大きい値を入力してください")
            amount > BigDecimal("9999999.99") -> ValidationResult.Error("予算金額が上限を超えています")
            else -> ValidationResult.Success
        }
    }

    fun validateGoalAmount(amount: BigDecimal?): ValidationResult {
        return when {
            amount == null -> ValidationResult.Error("目標金額を入力してください")
            amount <= BigDecimal.ZERO -> ValidationResult.Error("目標金額は0より大きい値を入力してください")
            amount > BigDecimal("99999999.99") -> ValidationResult.Error("目標金額が上限を超えています")
            else -> ValidationResult.Success
        }
    }

    fun validateGoalName(name: String?): ValidationResult {
        return when {
            name.isNullOrBlank() -> ValidationResult.Error("目標名を入力してください")
            name.length < 2 -> ValidationResult.Error("目標名は2文字以上で入力してください")
            name.length > 50 -> ValidationResult.Error("目標名は50文字以内で入力してください")
            else -> ValidationResult.Success
        }
    }

    fun validateCategoryName(name: String?): ValidationResult {
        return when {
            name.isNullOrBlank() -> ValidationResult.Error("カテゴリ名を入力してください")
            name.length < 1 -> ValidationResult.Error("カテゴリ名を入力してください")
            name.length > 30 -> ValidationResult.Error("カテゴリ名は30文字以内で入力してください")
            else -> ValidationResult.Success
        }
    }

    fun validatePin(pin: String?): ValidationResult {
        return when {
            pin.isNullOrBlank() -> ValidationResult.Error("PINを入力してください")
            pin.length != 4 -> ValidationResult.Error("PINは4桁で入力してください")
            !pin.all { it.isDigit() } -> ValidationResult.Error("PINは数字のみで入力してください")
            else -> ValidationResult.Success
        }
    }

    fun validateEmail(email: String?): ValidationResult {
        if (email.isNullOrBlank()) {
            return ValidationResult.Error("メールアドレスを入力してください")
        }

        val emailPattern = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
            "\\@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+"
        )

        return if (emailPattern.matcher(email).matches()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error("有効なメールアドレスを入力してください")
        }
    }

    fun validateBackupPassword(password: String?): ValidationResult {
        return when {
            password.isNullOrBlank() -> ValidationResult.Error("パスワードを入力してください")
            password.length < 8 -> ValidationResult.Error("パスワードは8文字以上で入力してください")
            password.length > 50 -> ValidationResult.Error("パスワードは50文字以内で入力してください")
            !password.any { it.isDigit() } -> ValidationResult.Error("パスワードには数字を含めてください")
            !password.any { it.isLetter() } -> ValidationResult.Error("パスワードには英字を含めてください")
            else -> ValidationResult.Success
        }
    }

    fun validatePositiveNumber(value: String?, fieldName: String): ValidationResult {
        if (value.isNullOrBlank()) {
            return ValidationResult.Error("${fieldName}を入力してください")
        }

        return try {
            val number = value.toDouble()
            if (number <= 0) {
                ValidationResult.Error("${fieldName}は0より大きい値を入力してください")
            } else {
                ValidationResult.Success
            }
        } catch (e: NumberFormatException) {
            ValidationResult.Error("${fieldName}には有効な数値を入力してください")
        }
    }

    fun validateDateRange(startDate: Long?, endDate: Long?): ValidationResult {
        return when {
            startDate == null -> ValidationResult.Error("開始日を選択してください")
            endDate == null -> ValidationResult.Error("終了日を選択してください")
            startDate > endDate -> ValidationResult.Error("開始日は終了日より前に設定してください")
            else -> ValidationResult.Success
        }
    }

    fun validateTransactionData(
        amount: BigDecimal?,
        categoryId: Long?,
        description: String?
    ): List<String> {
        val errors = mutableListOf<String>()

        val amountValidation = validateAmount(amount)
        if (amountValidation is ValidationResult.Error) {
            errors.add(amountValidation.message)
        }

        val categoryValidation = validateCategoryId(categoryId)
        if (categoryValidation is ValidationResult.Error) {
            errors.add(categoryValidation.message)
        }

        val descriptionValidation = validateDescription(description)
        if (descriptionValidation is ValidationResult.Error) {
            errors.add(descriptionValidation.message)
        }

        return errors
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}