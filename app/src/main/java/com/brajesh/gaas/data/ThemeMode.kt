package com.brajesh.gaas.data

/**
 * Which palette the app should paint itself with. [SYSTEM] follows the OS
 * dark-theme setting; the other two pin it. Persisted by name, so the values
 * are the storage format — rename with care.
 */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}
