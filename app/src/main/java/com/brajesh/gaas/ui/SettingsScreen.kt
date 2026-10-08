package com.brajesh.gaas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.brajesh.gaas.data.MacroGoal
import com.brajesh.gaas.data.ThemeMode
import com.brajesh.gaas.network.UpdateState

/**
 * Post-onboarding settings: appearance, the Gemini API key, and the daily goal
 * without going through first-launch setup again. Key/goal values pre-fill from
 * the current saved settings and are committed by the button; the theme picker
 * applies and persists the moment you tap it. A blank calorie goal (or empty
 * key) disables saving, same rule as onboarding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    apiKey: String,
    goal: MacroGoal?,
    themeMode: ThemeMode,
    updateState: UpdateState,
    appVersion: String,
    onThemeChange: (ThemeMode) -> Unit,
    onCheckForUpdate: () -> Unit,
    onOpenRelease: (String) -> Unit,
    onSave: (apiKey: String, goal: MacroGoal) -> Unit,
    onBack: () -> Unit
) {
    var key by remember { mutableStateOf(apiKey) }
    var calories by remember { mutableStateOf((goal?.calories ?: 0.0).toString()) }
    var protein by remember { mutableStateOf((goal?.proteinG ?: 0.0).toString()) }
    var carbs by remember { mutableStateOf((goal?.carbsG ?: 0.0).toString()) }
    var fat by remember { mutableStateOf((goal?.fatG ?: 0.0).toString()) }

    val canSave = key.isNotBlank() && calories.toDoubleOrNull() != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { BackArrow(onBack) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                // Keeps the goal fields and the save button above the keyboard.
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsCard(title = "Appearance") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = mode == themeMode,
                            onClick = { onThemeChange(mode) },
                            label = { Text(mode.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    "System follows your phone's dark-mode setting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            SettingsCard(title = "Gemini API key") {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Gemini API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Stored on this device only. Get a free key at aistudio.google.com/apikey.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            SettingsCard(title = "Daily macro goal") {
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
                Text(
                    "Blank macros stay 0g — the rings show \"no goal\" for them. " +
                        "History and the running total re-read from here once saved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Button(
                onClick = {
                    onSave(
                        key,
                        MacroGoal(
                            calories = calories.toDoubleOrNull() ?: 0.0,
                            proteinG = protein.toDoubleOrNull() ?: 0.0,
                            carbsG = carbs.toDoubleOrNull() ?: 0.0,
                            fatG = fat.toDoubleOrNull() ?: 0.0
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save changes")
            }

            SettingsCard(title = "Updates") {
                Text(
                    updateSummary(updateState, appVersion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCheckForUpdate,
                        enabled = updateState !is UpdateState.Checking,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (updateState is UpdateState.Checking) "Checking…" else "Check for updates")
                    }
                    val available = updateState as? UpdateState.Available
                    if (available != null) {
                        OutlinedButton(
                            onClick = { onOpenRelease(available.url) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Get ${available.version}")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Card wrapper used for every settings section, for a consistent grouping. */
@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

/** One line under the Updates heading describing the last check's outcome. */
private fun updateSummary(state: UpdateState, appVersion: String): String = when (state) {
    is UpdateState.Idle -> "You're on $appVersion. Checks GitHub for newer releases."
    is UpdateState.Checking -> "Asking GitHub for newer releases…"
    is UpdateState.UpToDate -> "You're on $appVersion — the latest release."
    is UpdateState.Available -> "${state.version} is available (you're on $appVersion)."
    is UpdateState.Failed -> state.reason
}
