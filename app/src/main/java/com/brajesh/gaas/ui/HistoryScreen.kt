package com.brajesh.gaas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brajesh.gaas.data.DaySummary
import com.brajesh.gaas.data.MacroGoal
import com.brajesh.gaas.data.MealEntry
import com.brajesh.gaas.repository.DaySlice
import kotlin.math.roundToInt

/**
 * History: weekly chart up top, then every logged day (newest first) as a row
 * showing that day's calories vs goal. Tapping a day opens its meal list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    days: List<DaySummary>,
    goal: MacroGoal?,
    weekly: List<DaySlice>,
    onDayClick: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            item { WeeklyChart(slices = weekly, goal = goal) }
            item {
                Text(
                    "Past days",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            if (days.isEmpty()) {
                item {
                    Text(
                        "Nothing logged yet — days will show up here as you track meals.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            items(days) { summary ->
                DayRow(summary = summary, goal = goal, onClick = { onDayClick(summary.dayKey) })
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun DayRow(summary: DaySummary, goal: MacroGoal?, onClick: () -> Unit) {
    val hasGoal = goal != null && goal.calories > 0
    val goalCal = goal?.calories ?: 0.0
    val over = hasGoal && summary.calories > goalCal

    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(formatDayLabel(summary.dayKey), style = MaterialTheme.typography.titleMedium)
            val calPart = if (hasGoal) {
                "${summary.calories.roundToInt()} / ${goalCal.roundToInt()} kcal"
            } else {
                "${summary.calories.roundToInt()} kcal"
            }
            val statusPart = when {
                over -> "over by ${(summary.calories - goalCal).roundToInt()} kcal"
                hasGoal -> "${(goalCal - summary.calories).roundToInt()} kcal left"
                else -> ""
            }
            Text(
                if (statusPart.isEmpty()) calPart else "$calPart · $statusPart",
                style = MaterialTheme.typography.bodyMedium,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                "P ${summary.proteinG.roundToInt()}g · C ${summary.carbsG.roundToInt()}g · F ${summary.fatG.roundToInt()}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/** Read-only view of one day's meals, reached from the history list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    dayKey: String,
    goal: MacroGoal?,
    meals: List<MealEntry>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(formatDayLabel(dayKey)) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            item { DayTotalsCard(goal = goal, meals = meals) }
            item {
                Text(
                    "Meals",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            if (meals.isEmpty()) {
                item { Text("No meals logged this day.", style = MaterialTheme.typography.bodyMedium) }
            }
            items(meals) { meal ->
                MealRow(meal)
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun DayTotalsCard(goal: MacroGoal?, meals: List<MealEntry>) {
    val consumedCalories = meals.sumOf { it.calories }
    val consumedProteinG = meals.sumOf { it.proteinG }
    val consumedCarbsG = meals.sumOf { it.carbsG }
    val consumedFatG = meals.sumOf { it.fatG }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (goal == null) {
                Text("No goal set for this day.")
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MacroRing("Calories", consumedCalories, goal.calories, "kcal", Modifier.weight(1f))
                    MacroRing("Protein", consumedProteinG, goal.proteinG, "g", Modifier.weight(1f))
                    MacroRing("Carbs", consumedCarbsG, goal.carbsG, "g", Modifier.weight(1f))
                    MacroRing("Fat", consumedFatG, goal.fatG, "g", Modifier.weight(1f))
                }
            }
        }
    }
}