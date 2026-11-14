package com.brajesh.gaas.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.brajesh.gaas.viewmodel.PendingEstimate
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    pending: PendingEstimate,
    onEstimate: (String) -> Unit,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit,
    onBack: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Log a meal") },
            navigationIcon = { BackArrow(onBack) }
        )
    }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                // Edge-to-edge: the keyboard no longer resizes the window, and
                // the review card's "Log it" button has to stay reachable.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Describe what you ate, as loosely as you like — quantities, homemade " +
                        "stuff, \"a little of this and that\" is fine.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("2 rotis with dal", "a bowl of curd rice", "chicken biryani, half plate").forEach { example ->
                    AssistChip(
                        onClick = { text = example },
                        label = { Text(example) }
                    )
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("e.g. 3 rotis with a little ghee, homemade paneer, and a bit of salad") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
                enabled = pending !is PendingEstimate.Loading
            )

            Button(
                onClick = { onEstimate(text) },
                enabled = text.isNotBlank() && pending !is PendingEstimate.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (pending is PendingEstimate.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Estimating…")
                } else {
                    Text("Estimate macros")
                }
            }

            when (pending) {
                is PendingEstimate.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        "Asking Gemini for an estimate…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                is PendingEstimate.Failed -> Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        pending.message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                else -> {}
            }

            // Hoisted out of the `when` so the card also animates *out* when the
            // estimate is discarded or logged, not just in.
            val ready = pending as? PendingEstimate.Ready
            AnimatedVisibility(
                visible = ready != null,
                enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
            ) {
                if (ready != null) ReviewCard(ready, onConfirm, onDiscard)
            }
        }
    }
}

@Composable
private fun ReviewCard(ready: PendingEstimate.Ready, onConfirm: () -> Unit, onDiscard: () -> Unit) {
    val meal = ready.parsed
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Review before saving",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Column {
                meal.items.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(item.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                item.quantity,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${item.calories.roundToInt()} kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                "${meal.totalCalories.roundToInt()} kcal",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroBadge("P", meal.totalProteinG, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                MacroBadge("C", meal.totalCarbsG, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                MacroBadge("F", meal.totalFatG, Color(0xFFF57C00), Modifier.weight(1f))
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    meal.assumptionsNote,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) { Text("Discard") }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text("Log it") }
            }
        }
    }
}

@Composable
private fun MacroBadge(label: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        modifier = modifier
    ) {
        Column(
            Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$label · ${value.roundToInt()}g", style = MaterialTheme.typography.labelLarge)
        }
    }
}
