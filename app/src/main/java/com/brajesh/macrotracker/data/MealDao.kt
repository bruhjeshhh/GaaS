package com.brajesh.macrotracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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
}
