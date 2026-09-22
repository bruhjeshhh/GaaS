package com.brajesh.gaas.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.brajesh.gaas.data.MacroGoal

@Composable
fun OnboardingScreen(onComplete: (apiKey: String, goal: MacroGoal) -> Unit) {
    var apiKey by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    val canSubmit = apiKey.isNotBlank() && calories.toDoubleOrNull() != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Set up GaaS", style = MaterialTheme.typography.headlineSmall)
        Text(
            "This app estimates macros from what you type using your own Gemini API " +
                "key — nothing is sent to any server but Google's. Get a free key at " +
                "aistudio.google.com/apikey.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("Gemini API key") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Divider()
        Text("Daily macro goal", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = calories, onValueChange = { calories = it },
            label = { Text("Calories") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = protein, onValueChange = { protein = it },
            label = { Text("Protein (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = carbs, onValueChange = { carbs = it },
            label = { Text("Carbs (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = fat, onValueChange = { fat = it },
            label = { Text("Fat (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                onComplete(
                    apiKey,
                    MacroGoal(
                        calories = calories.toDoubleOrNull() ?: 0.0,
                        proteinG = protein.toDoubleOrNull() ?: 0.0,
                        carbsG = carbs.toDoubleOrNull() ?: 0.0,
                        fatG = fat.toDoubleOrNull() ?: 0.0
                    )
                )
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start tracking")
        }
    }
}
