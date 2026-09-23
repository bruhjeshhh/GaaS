package com.brajesh.gaas.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brajesh.gaas.data.MealEntry
import kotlin.math.roundToInt

/**
 * Shared meal row. Used on Home with a delete affordance and in the read-only
 * per-day history detail (onDelete = null hides the button).
 */
@Composable
fun MealRow(meal: MealEntry, modifier: Modifier = Modifier, onDelete: (() -> Unit)? = null) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(meal.rawText, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "${meal.calories.roundToInt()} kcal · P ${meal.proteinG.roundToInt()}g · " +
                    "C ${meal.carbsG.roundToInt()}g · F ${meal.fatG.roundToInt()}g",
                style = MaterialTheme.typography.bodySmall
            )
            meal.estimateNote?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.End)) {
                    Text("Remove")
                }
            }
        }
    }
}