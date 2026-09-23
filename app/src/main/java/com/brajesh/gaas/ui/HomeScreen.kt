package com.brajesh.gaas.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    onDeleteMeal: (MealEntry) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Today") }) },
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
            item { RemainingCard(totals) }
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

@Composable
private fun RemainingCard(totals: DayTotals) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (totals.goal == null) {
                Text("No goal set yet.")
                return@Column
            }
            Text("Remaining today", style = MaterialTheme.typography.titleMedium)
            MacroRow("Calories", totals.remainingCalories, totals.goal.calories)
            MacroRow("Protein", totals.remainingProteinG, totals.goal.proteinG, unit = "g")
            MacroRow("Carbs", totals.remainingCarbsG, totals.goal.carbsG, unit = "g")
            MacroRow("Fat", totals.remainingFatG, totals.goal.fatG, unit = "g")
        }
    }
}

@Composable
private fun MacroRow(label: String, remaining: Double, goal: Double, unit: String = "") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        val over = remaining < 0
        Text(
            "${remaining.roundToInt()}$unit / ${goal.roundToInt()}$unit${if (over) " (over)" else " left"}",
            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
