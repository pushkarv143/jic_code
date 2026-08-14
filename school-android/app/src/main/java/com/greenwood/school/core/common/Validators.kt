package com.greenwood.school.core.common

import android.util.Patterns

/**
 * Client-side validation mirroring the Yup schemas in the web forms and the
 * `jakarta.validation` annotations on the backend request DTOs.
 *
 * These exist to give instant feedback, not to be the source of truth — the
 * server validates again and its per-field errors are surfaced through
 * [com.greenwood.school.core.network.AppError.Validation].
 */
object Validators {

    /** `null` means valid; a non-null string is the message to show under the field. */
    fun required(value: String?, label: String): String? =
        if (value.isNullOrBlank()) "$label is required" else null

    fun email(value: String?, required: Boolean = true): String? {
        if (value.isNullOrBlank()) return if (required) "Email is required" else null
        return if (Patterns.EMAIL_ADDRESS.matcher(value).matches()) null else "Enter a valid email address"
    }

    /**
     * Indian mobile numbers, optionally with a `+91`/`91` prefix and separators —
     * the seed data stores them as `+91-9999900001`.
     */
    fun phone(value: String?, required: Boolean = true): String? {
        if (value.isNullOrBlank()) return if (required) "Phone number is required" else null
        val digits = value.filter(Char::isDigit)
        val national = digits.removePrefix("91").takeIf { digits.length > 10 } ?: digits
        return if (national.length == 10 && national.first() in '6'..'9') {
            null
        } else {
            "Enter a valid 10-digit mobile number"
        }
    }

    /**
     * Matches the backend's password policy used by the seeded accounts
     * (`Admin@123`, `Password@123`): at least 8 characters with an upper, a lower,
     * a digit and a symbol.
     */
    fun password(value: String?): String? = when {
        value.isNullOrBlank() -> "Password is required"
        value.length < 8 -> "Password must be at least 8 characters"
        value.none(Char::isUpperCase) -> "Include at least one uppercase letter"
        value.none(Char::isLowerCase) -> "Include at least one lowercase letter"
        value.none(Char::isDigit) -> "Include at least one number"
        value.all { it.isLetterOrDigit() } -> "Include at least one special character"
        else -> null
    }

    fun confirmPassword(password: String?, confirmation: String?): String? = when {
        confirmation.isNullOrBlank() -> "Confirm your password"
        password != confirmation -> "Passwords do not match"
        else -> null
    }

    fun positiveNumber(value: String?, label: String, allowZero: Boolean = false): String? {
        if (value.isNullOrBlank()) return "$label is required"
        val parsed = value.toDoubleOrNull() ?: return "$label must be a number"
        return when {
            parsed < 0 -> "$label cannot be negative"
            !allowZero && parsed == 0.0 -> "$label must be greater than zero"
            else -> null
        }
    }

    fun selected(value: Long?, label: String): String? =
        if (value == null || value <= 0) "Select a $label" else null

    fun pincode(value: String?, required: Boolean = false): String? {
        if (value.isNullOrBlank()) return if (required) "PIN code is required" else null
        return if (value.length == 6 && value.all(Char::isDigit)) null else "Enter a valid 6-digit PIN code"
    }

    fun maxLength(value: String?, max: Int, label: String): String? =
        if ((value?.length ?: 0) > max) "$label must be at most $max characters" else null

    /** Combines several checks and returns the first failure, or null when all pass. */
    fun firstError(vararg checks: String?): String? = checks.firstOrNull { it != null }
}
