package com.brajesh.gaas.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Parsed from the ISO "yyyy-MM-dd" dayKey directly, so no timezone arithmetic is needed.
private val dayLabelFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
private val longDayFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.US)
private val weekdayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.US)

/** "2026-09-23" -> "Sep 23"; falls back to the raw key if it won't parse. */
internal fun formatDayLabel(dayKey: String): String =
    runCatching { LocalDate.parse(dayKey).format(dayLabelFormatter) }.getOrDefault(dayKey)

/** "2026-09-23" -> "Wednesday, Sep 23"; falls back to the raw key if it won't parse. */
internal fun formatLongDayLabel(dayKey: String): String =
    runCatching { LocalDate.parse(dayKey).format(longDayFormatter) }.getOrDefault(dayKey)

/** "2026-09-23" -> "Wed"; falls back to the raw key if it won't parse. */
internal fun formatWeekday(dayKey: String): String =
    runCatching { LocalDate.parse(dayKey).format(weekdayFormatter) }.getOrDefault(dayKey)

/**
 * Friendly relative label for the history list: "Today", "Yesterday", else the
 * weekday ("Wed"). Falls back to an empty string if the key won't parse.
 */
internal fun formatRelativeDay(dayKey: String): String {
    val date = runCatching { LocalDate.parse(dayKey) }.getOrNull() ?: return ""
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(weekdayFormatter)
    }
}