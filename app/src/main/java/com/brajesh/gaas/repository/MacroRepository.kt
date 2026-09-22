package com.brajesh.gaas.repository

import com.brajesh.gaas.data.MacroGoal
import com.brajesh.gaas.data.MealDao
import com.brajesh.gaas.data.MealEntry
import com.brajesh.gaas.data.SettingsStore
import com.brajesh.gaas.network.GeminiClient
import com.brajesh.gaas.network.GeminiResult
import com.brajesh.gaas.network.ParsedMeal
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DayTotals(
    val goal: MacroGoal?,
    val consumedCalories: Double,
    val consumedProteinG: Double,
    val consumedCarbsG: Double,
    val consumedFatG: Double
) {
    val remainingCalories get() = (goal?.calories ?: 0.0) - consumedCalories
    val remainingProteinG get() = (goal?.proteinG ?: 0.0) - consumedProteinG
    val remainingCarbsG get() = (goal?.carbsG ?: 0.0) - consumedCarbsG
    val remainingFatG get() = (goal?.fatG ?: 0.0) - consumedFatG
}

class MacroRepository(
    private val dao: MealDao,
    private val settings: SettingsStore
) {
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val json = Json { encodeDefaults = true }

    fun todayKey(): String = dayFormat.format(Date())

    fun goal(): MacroGoal? = settings.goal
    fun setGoal(goal: MacroGoal) { settings.goal = goal }
    fun apiKey(): String? = settings.geminiApiKey
    fun setApiKey(key: String) { settings.geminiApiKey = key.trim() }
    val isOnboarded: Boolean get() = settings.isOnboarded

    fun mealsForDay(dayKey: String): Flow<List<MealEntry>> = dao.mealsForDay(dayKey)

    suspend fun estimateMeal(description: String): GeminiResult {
        val key = settings.geminiApiKey
            ?: return GeminiResult.ApiError("No Gemini API key set — add one in Settings.")
        return GeminiClient(key).estimateMacros(description)
    }

    suspend fun logMeal(rawText: String, parsed: ParsedMeal) {
        val entry = MealEntry(
            rawText = rawText,
            loggedAtEpochMillis = System.currentTimeMillis(),
            dayKey = todayKey(),
            calories = parsed.totalCalories,
            proteinG = parsed.totalProteinG,
            carbsG = parsed.totalCarbsG,
            fatG = parsed.totalFatG,
            parsedItemsJson = json.encodeToString(parsed.items),
            estimateNote = parsed.assumptionsNote
        )
        dao.insert(entry)
    }

    suspend fun deleteMeal(entry: MealEntry) = dao.delete(entry)

    fun computeTotals(meals: List<MealEntry>): DayTotals = DayTotals(
        goal = goal(),
        consumedCalories = meals.sumOf { it.calories },
        consumedProteinG = meals.sumOf { it.proteinG },
        consumedCarbsG = meals.sumOf { it.carbsG },
        consumedFatG = meals.sumOf { it.fatG }
    )
}
