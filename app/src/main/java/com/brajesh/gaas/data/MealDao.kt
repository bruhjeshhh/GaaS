package com.brajesh.gaas.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * One day's aggregated totals summed straight out of SQL (GROUP BY dayKey).
 * The cheap path for history/charts — no need to pull full MealEntry rows
 * just to sum them. DaySummary should only contain queries that aggregate
 * existing columns, so it never triggers a schema migration.
 */
data class DaySummary(
    val dayKey: String,
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double
)

@Dao
interface MealDao {

    @Insert
    suspend fun insert(entry: MealEntry): Long

    @Delete
    suspend fun delete(entry: MealEntry)

    @Query("SELECT * FROM meal_entries WHERE dayKey = :dayKey ORDER BY loggedAtEpochMillis ASC")
    fun mealsForDay(dayKey: String): Flow<List<MealEntry>>

    @Query("SELECT * FROM meal_entries ORDER BY loggedAtEpochMillis DESC")
    fun allMeals(): Flow<List<MealEntry>>

    @Query("SELECT DISTINCT dayKey FROM meal_entries ORDER BY dayKey DESC")
    fun allDays(): Flow<List<String>>

    /**
     * Per-day totals without loading full rows. Only days with at least one
     * meal appear — a missing dayKey here means "no data", not "0 consumed".
     * Newest first.
     */
    @Query(
        "SELECT dayKey, SUM(calories) AS calories, SUM(proteinG) AS proteinG, " +
            "SUM(carbsG) AS carbsG, SUM(fatG) AS fatG " +
            "FROM meal_entries GROUP BY dayKey ORDER BY dayKey DESC"
    )
    fun daySummaries(): Flow<List<DaySummary>>
}
