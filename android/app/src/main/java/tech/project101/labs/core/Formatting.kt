package tech.project101.labs.core

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** 1959.4 -> "1.959" (with the grouping of the current locale) */
val Double.roundedFormatted: String
    get() = NumberFormat.getIntegerInstance().format(roundToLong())

/** 12.5 -> "12,5", 12.0 -> "12" */
val Double.compactFormatted: String
    get() = decimalFormatted(0, 1)

/** for text fields: no grouping, so it parses back, 1000.5 -> "1000,5" */
val Double.inputFormatted: String
    get() = NumberFormat.getNumberInstance().apply {
        isGroupingUsed = false
        maximumFractionDigits = 2
    }.format(this)

/** parses what [inputFormatted] wrote or the user typed, with comma or dot */
fun String.parseDecimal(): Double? = trim().replace(',', '.').toDoubleOrNull()

/** 12.345 -> "12,3" */
fun Double.decimalFormatted(min: Int, max: Int = min): String =
    NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = min
        maximumFractionDigits = max
    }.format(this)

/** "Heute", "Gestern", "Morgen" or the weekday */
fun LocalDate.dayTitle(today: LocalDate = LocalDate.now()): String = when (this) {
    today -> "Heute"
    today.minusDays(1) -> "Gestern"
    today.plusDays(1) -> "Morgen"
    else -> dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
}

/** "Sonntag, 4. Oktober" */
val LocalDate.longDay: String
    get() = format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.getDefault()))

/** seconds -> "45:12" or "1:05:12" */
fun formatDuration(seconds: Long): String {
    val s = abs(seconds)
    val hours = s / 3600
    val minutes = (s % 3600) / 60
    val rest = s % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, rest) else "%d:%02d".format(minutes, rest)
}

/** seconds -> "1 Std. 5 Min." */
fun formatHours(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "$hours Std. $minutes Min." else "$minutes Min."
}

/** seconds -> "7 h 32 min" */
val Double.sleepDuration: String
    get() {
        val total = (this / 60).roundToLong()
        val hours = total / 60
        val minutes = total % 60
        return if (hours > 0) "$hours h ${"%02d".format(minutes)} min" else "$minutes min"
    }
