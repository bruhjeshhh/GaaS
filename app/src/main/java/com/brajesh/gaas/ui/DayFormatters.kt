package com.brajesh.gaas.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Parsed from the ISO "yyyy-MM-dd" dayKey directly, so no timezone arithmetic is needed.
private val dayLabelFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
private val weekdayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.US)

/** "2026-09-23" -> "Sep 23"; falls back to the raw key if it won't parse. */
internal fun formatDayLabel(dayKey: String): String =
    runCatching { LocalDate.parse(dayKey).format(dayLabelFormatter) }.getOrDefault(dayKey)

/** "2026-09-23" -> "Wed"; falls back to the raw key if it won't parse. */
internal fun formatWeekday(dayKey: String): String =
    runCatching { LocalDate.parse(dayKey).format(weekdayFormatter) }.getOrDefault(dayKey)