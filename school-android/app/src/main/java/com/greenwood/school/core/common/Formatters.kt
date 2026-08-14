package com.greenwood.school.core.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Date/number formatting that matches what the web app renders, so a user moving
 * between the two clients sees identical strings.
 *
 * The backend serialises `LocalDate` as `yyyy-MM-dd`, `LocalDateTime` as ISO-8601
 * without a zone, and `LocalTime` as `HH:mm:ss`. All parsing here is lenient:
 * a malformed value renders as [PLACEHOLDER] rather than crashing a list row.
 */
object Formatters {

    const val PLACEHOLDER = "—"

    /** Wire format for every `LocalDate` query param and request field. */
    val API_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val DISPLAY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    private val DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
    private val DISPLAY_TIME = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
    private val DISPLAY_MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    fun date(raw: String?): String = parseDate(raw)?.format(DISPLAY_DATE) ?: PLACEHOLDER

    fun dateTime(raw: String?): String = parseDateTime(raw)?.format(DISPLAY_DATE_TIME) ?: PLACEHOLDER

    fun time(raw: String?): String {
        if (raw.isNullOrBlank()) return PLACEHOLDER
        return runCatching { LocalTime.parse(raw).format(DISPLAY_TIME) }
            .getOrElse { parseDateTime(raw)?.format(DISPLAY_TIME) ?: PLACEHOLDER }
    }

    fun monthYear(month: Int, year: Int): String = runCatching {
        YearMonth.of(year, month).format(DISPLAY_MONTH)
    }.getOrElse { "$month/$year" }

    fun parseDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        return try {
            LocalDate.parse(raw)
        } catch (_: DateTimeParseException) {
            parseDateTime(raw)?.toLocalDate()
        }
    }

    fun parseDateTime(raw: String?): LocalDateTime? {
        if (raw.isNullOrBlank()) return null
        return runCatching { LocalDateTime.parse(raw) }.getOrNull()
    }

    fun apiDate(date: LocalDate): String = date.format(API_DATE)

    /**
     * Indian rupee formatting with lakh/crore grouping — matches the web app's
     * `formatCurrencyINR` (`Intl.NumberFormat('en-IN', { currency: 'INR' })`).
     */
    fun currency(amount: Double?): String {
        if (amount == null) return PLACEHOLDER
        return "₹" + groupIndian(amount)
    }

    fun number(value: Number?): String {
        if (value == null) return PLACEHOLDER
        return groupIndian(value.toDouble(), decimals = 0)
    }

    fun percent(value: Double?, decimals: Int = 1): String {
        if (value == null) return PLACEHOLDER
        return String.format(Locale.ENGLISH, "%.${decimals}f%%", value)
    }

    /**
     * 12,34,567.89 — Indian digit grouping (last three, then pairs) which
     * `java.text.NumberFormat` cannot produce without an ICU locale on every API level.
     */
    private fun groupIndian(value: Double, decimals: Int = 2): String {
        val negative = value < 0
        val absolute = kotlin.math.abs(value)
        val fixed = String.format(Locale.ENGLISH, "%.${decimals}f", absolute)
        val parts = fixed.split('.')
        val whole = parts[0]
        val fraction = parts.getOrNull(1)

        val grouped = if (whole.length <= 3) {
            whole
        } else {
            val lastThree = whole.takeLast(3)
            val rest = whole.dropLast(3)
            val chunked = rest.reversed().chunked(2).joinToString(",").reversed()
            "$chunked,$lastThree"
        }

        return buildString {
            if (negative) append('-')
            append(grouped)
            if (!fraction.isNullOrEmpty()) append('.').append(fraction)
        }
    }

    /** "R. Sharma" style display name, tolerating the nullable name columns. */
    fun personName(firstName: String?, lastName: String?, fallback: String? = null): String =
        listOfNotNull(firstName?.takeIf { it.isNotBlank() }, lastName?.takeIf { it.isNotBlank() })
            .joinToString(" ")
            .ifBlank { fallback ?: PLACEHOLDER }

    fun initials(name: String): String = name.split(' ', '.', '_')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

    /** `SUPER_ADMIN` -> `Super Admin`; used for role chips and enum labels. */
    fun humanizeEnum(raw: String?): String {
        if (raw.isNullOrBlank()) return PLACEHOLDER
        return raw.split('_').joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}
