package com.brajesh.gaas.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        TopAppBar(title = { Text("Log a meal") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("Back") }
        })
    }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Describe what you ate, as loosely as you like — quantities, homemade " +
                    "stuff, \"a little of this and that\" is fine.",
                style = MaterialTheme.typography.bodyMedium
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
                is PendingEstimate.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is PendingEstimate.Failed -> Text(
                    pending.message,
                    color = MaterialTheme.colorScheme.error
                )
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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Estimated", style = MaterialTheme.typography.titleMedium)
            meal.items.forEach { item ->
                Text("• ${item.name} (${item.quantity}) — ${item.calories.roundToInt()} kcal")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Total: ${meal.totalCalories.roundToInt()} kcal · " +
                    "P ${meal.totalProteinG.roundToInt()}g · " +
                    "C ${meal.totalCarbsG.roundToInt()}g · " +
                    "F ${meal.totalFatG.roundToInt()}g",
                style = MaterialTheme.typography.bodyMedium
            )
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
