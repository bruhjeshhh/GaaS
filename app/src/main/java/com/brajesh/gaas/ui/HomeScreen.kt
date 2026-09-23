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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brajesh.gaas.data.MealEntry
import com.brajesh.gaas.repository.DayTotals
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    totals: DayTotals,
    meals: List<MealEntry>,
    onAddMeal: () -> Unit,
    onDeleteMeal: (MealEntry) -> Unit,
    onOpenHistory: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    TextButton(onClick = onOpenHistory) { Text("History") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMeal) { Icon(Icons.Default.Add, contentDescription = "Log meal") }
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
            item { ProgressCard(totals) }
            item {
                Text(
                    "Logged today",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            if (meals.isEmpty()) {
                item { Text("Nothing logged yet — tap + to add a meal.", style = MaterialTheme.typography.bodyMedium) }
            }
            items(meals.reversed()) { meal ->
                MealRow(meal, onDelete = { onDeleteMeal(meal) })
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

/**
 * Today's goal completion. Replaces the old text-only RemainingCard: a row of
 * progress rings (calories + macros) that keep the "N left" info while adding
 * the at-a-glance fill fraction and an explicit over-goal state.
 */
@Composable
private fun ProgressCard(totals: DayTotals) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Today's progress", style = MaterialTheme.typography.titleMedium)
                if (totals.goal != null) {
                    Text(
                        "${totals.consumedCalories.roundToInt()} / ${totals.goal.calories.roundToInt()} kcal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            if (totals.goal == null) {
                Text("No goal set yet.")
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MacroRing("Calories", totals.consumedCalories, totals.goal.calories, "kcal", Modifier.weight(1f))
                    MacroRing("Protein", totals.consumedProteinG, totals.goal.proteinG, "g", Modifier.weight(1f))
                    MacroRing("Carbs", totals.consumedCarbsG, totals.goal.carbsG, "g", Modifier.weight(1f))
                    MacroRing("Fat", totals.consumedFatG, totals.goal.fatG, "g", Modifier.weight(1f))
                }
            }
        }
    }
}