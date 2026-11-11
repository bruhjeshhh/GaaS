package com.brajesh.gaas.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** The four top-level destinations. Detail screens keep their own back arrow. */
enum class BottomTab {
    HOME, ADD, HISTORY, SETTINGS
}

/** Shared bottom bar so every top-level screen feels like the same app. */
@Composable
fun GaaSBottomBar(current: BottomTab, onSelect: (BottomTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        NavigationBarItem(
            selected = current == BottomTab.HOME,
            onClick = { onSelect(BottomTab.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = current == BottomTab.ADD,
            onClick = { onSelect(BottomTab.ADD) },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            label = { Text("Log") }
        )
        NavigationBarItem(
            selected = current == BottomTab.HISTORY,
            onClick = { onSelect(BottomTab.HISTORY) },
            icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            label = { Text("History") }
        )
        NavigationBarItem(
            selected = current == BottomTab.SETTINGS,
            onClick = { onSelect(BottomTab.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") }
        )
    }
}
