package com.brajesh.gaas.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brajesh.gaas.data.MealEntry
import com.brajesh.gaas.network.UpdateState
import com.brajesh.gaas.repository.DayTotals
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    totals: DayTotals,
    meals: List<MealEntry>,
    accountName: String?,
    updateState: UpdateState,
    onGetUpdate: (String) -> Unit,
    onDismissUpdate: () -> Unit,
    onAddMeal: () -> Unit,
    onDeleteMeal: (MealEntry) -> Unit,
    onUndoDelete: (MealEntry) -> Unit,
    onNavigate: (BottomTab) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Delete immediately, then offer an undo. Doing the write first keeps the
    // list honest if the user ignores the snackbar; undo re-inserts the entry.
    fun deleteWithUndo(meal: MealEntry) {
        onDeleteMeal(meal)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Meal removed",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onUndoDelete(meal)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { GaaSBottomBar(BottomTab.HOME, onNavigate) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val update = updateState as? UpdateState.Available
            if (update != null && !update.dismissed) {
                item {
                    Spacer(Modifier.height(4.dp))
                    UpdateBanner(update = update, onGetIt = onGetUpdate, onDismiss = onDismissUpdate)
                }
            }
            item { HeroCard(totals, accountName, onAddMeal) }
            item { SectionHeader("Logged today") }
            if (meals.isEmpty()) {
                item {
                    EmptyState(
                        text = "Nothing logged yet — tap \"Log a meal\" to add your first meal.",
                        icon = Icons.AutoMirrored.Filled.List
                    )
                }
            }
            items(meals.reversed(), key = { it.id }) { meal ->
                MealRow(
                    meal,
                    modifier = Modifier.animateItemPlacement(),
                    onDelete = { deleteWithUndo(meal) }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

/**
 * The home hero is the app's visual anchor: a teal→violet gradient with the
 * remaining calories as a huge number, a progress bar and macro pills. It
 * replaces the old four-ring card, which read as a quiet dashboard widget.
 */
@Composable
private fun HeroCard(totals: DayTotals, accountName: String?, onAddMeal: () -> Unit) {
    val goal = totals.goal
    val hasGoal = goal != null && goal.calories > 0
    val over = hasGoal && totals.remainingCalories < 0
    val remaining = if (over) -totals.remainingCalories else totals.remainingCalories
    val progress = if (goal != null && goal.calories > 0) {
        (totals.consumedCalories / goal.calories).coerceIn(0.0, 1.0).toFloat()
    } else 0f
    val onHero = MaterialTheme.colorScheme.onPrimary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            greeting(accountName),
            style = MaterialTheme.typography.titleMedium,
            color = onHero.copy(alpha = 0.82f)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (hasGoal) "${remaining.roundToInt()}" else "Ready when you are",
            style = if (hasGoal) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = onHero
        )
        Text(
            when {
                !hasGoal -> "Set a daily goal in Settings to see your remaining budget."
                over -> "kcal over your ${goal?.calories?.roundToInt() ?: 0} goal today"
                else -> "kcal left of ${goal?.calories?.roundToInt() ?: 0} today"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = onHero.copy(alpha = 0.86f)
        )
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = onHero,
            trackColor = onHero.copy(alpha = 0.25f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroPill("Protein", totals.consumedProteinG, goal?.proteinG ?: 0.0, "g", Modifier.weight(1f))
            MacroPill("Carbs", totals.consumedCarbsG, goal?.carbsG ?: 0.0, "g", Modifier.weight(1f))
            MacroPill("Fat", totals.consumedFatG, goal?.fatG ?: 0.0, "g", Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onAddMeal,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = onHero, contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Log a meal")
        }
    }
}

@Composable
private fun MacroPill(
    label: String,
    consumed: Double,
    goal: Double,
    unit: String,
    modifier: Modifier = Modifier
) {
    val onHero = MaterialTheme.colorScheme.onPrimary
    Surface(
        color = onHero.copy(alpha = 0.16f),
        contentColor = onHero,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = onHero.copy(alpha = 0.8f))
            Text(
                if (goal > 0) "${consumed.roundToInt()}/${goal.roundToInt()}$unit"
                else "${consumed.roundToInt()}$unit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun greeting(name: String?): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val time = when {
        hour < 12 -> "morning"
        hour < 17 -> "afternoon"
        else -> "evening"
    }
    return if (name.isNullOrBlank()) "Good $time" else "Good $time, ${name.trim()}"
}
