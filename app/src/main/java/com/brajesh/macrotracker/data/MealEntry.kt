package com.brajesh.macrotracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One logged meal. `rawText` keeps the user's original verbose description
 * ("3 rotis with a little ghee, homemade paneer") so the log stays readable
 * later, not just a row of numbers.
 */
@Entity(tableName = "meal_entries")
data class MealEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawText: String,
    val loggedAtEpochMillis: Long,
    val dayKey: String, // "yyyy-MM-dd" in the device's local zone, used to group meals by day
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val parsedItemsJson: String, // Gemini's per-item breakdown, kept for the "what did it think I ate" view
    val estimateNote: String? = null // e.g. "assumed ~1 tbsp ghee, medium roti" — Gemini's own caveat
)
