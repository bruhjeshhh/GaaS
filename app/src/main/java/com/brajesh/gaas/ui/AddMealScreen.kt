package com.brajesh.gaas.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            Text(
                "Describe what you ate, as loosely as you like — quantities, homemade " +
                    "stuff, \"a little of this and that\" is fine.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                Text(if (pending is PendingEstimate.Loading) "Estimating…" else "Estimate macros")
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
                is PendingEstimate.Ready -> ReviewCard(pending, onConfirm, onDiscard)
                PendingEstimate.Idle -> {}
            }
        }
    }
}

@Composable
private fun ReviewCard(ready: PendingEstimate.Ready, onConfirm: () -> Unit, onDiscard: () -> Unit) {
    val meal = ready.parsed
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Review before saving", style = MaterialTheme.typography.titleMedium)

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${meal.totalCalories.roundToInt()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "P ${meal.totalProteinG.roundToInt()}g · " +
                        "C ${meal.totalCarbsG.roundToInt()}g · " +
                        "F ${meal.totalFatG.roundToInt()}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                meal.assumptionsNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) { Text("Discard") }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text("Log it") }
            }
        }
    }
}
