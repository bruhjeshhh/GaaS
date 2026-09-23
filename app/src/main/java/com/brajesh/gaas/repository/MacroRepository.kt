package com.brajesh.gaas.repository

import com.brajesh.gaas.data.DaySummary
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
import java.time.LocalDate
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

/**
 * A single day's slot in the weekly chart. `hasData` distinguishes "no meals
 * were ever logged that day" (missing from the DB entirely, kept separate so
 * sparse history doesn't get faked as a scary zero bar) from a genuinely
 * logged 0 kcal day.
 */
data class DaySlice(
    val dayKey: String,
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val hasData: Boolean
)

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

    /** Per-day totals for history/charts. Only days with meals are present. */
    fun daySummaries(): Flow<List<DaySummary>> = dao.daySummaries()

    /**
     * Builds the last 7 days as [DaySlice]s, oldest → newest, padding missing
     * days with hasData=false. Uses LocalDate (API 26+, ISO formatting) so the
     * keys line up with the SimpleDateFormat("yyyy-MM-dd") used by [todayKey].
     */
    fun recentDailySlices(summaries: List<DaySummary>): List<DaySlice> {
        val byKey = summaries.associateBy { it.dayKey }
        return (6L downTo 0L).map { back ->
            val key = LocalDate.now().minusDays(back).toString()
            val summary = byKey[key]
            if (summary == null) DaySlice(key, 0.0, 0.0, 0.0, 0.0, hasData = false)
            else DaySlice(key, summary.calories, summary.proteinG, summary.carbsG, summary.fatG, hasData = true)
        }
    }

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
