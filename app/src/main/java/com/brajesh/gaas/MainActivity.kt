package com.brajesh.gaas

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brajesh.gaas.data.AppDatabase
import com.brajesh.gaas.data.SettingsStore
import com.brajesh.gaas.repository.MacroRepository
import com.brajesh.gaas.ui.AddMealScreen
import com.brajesh.gaas.ui.DayDetailScreen
import com.brajesh.gaas.ui.HistoryScreen
import com.brajesh.gaas.ui.HomeScreen
import com.brajesh.gaas.ui.OnboardingScreen
import com.brajesh.gaas.ui.SettingsScreen
import com.brajesh.gaas.ui.theme.GaaSTheme
import com.brajesh.gaas.viewmodel.MacroViewModel

private sealed interface Screen {
    data object Home : Screen
    data object AddMeal : Screen
    data object History : Screen
    data class DayDetail(val dayKey: String) : Screen
    data object Settings : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = SettingsStore(applicationContext)
        val dao = AppDatabase.get(applicationContext).mealDao()
        val repo = MacroRepository(dao, settings)

        // targetSdk 36 enforces edge-to-edge, so ask for it explicitly and let
        // the screens below handle their own insets. Icon contrast is then
        // corrected per-theme inside GaaSTheme.
        enableEdgeToEdge()

        setContent {
            val viewModel: MacroViewModel = viewModel(factory = MacroViewModel.Factory(repo))
            val themeMode by viewModel.themeMode.collectAsState()

            GaaSTheme(themeMode) {
                Surface(modifier = Modifier, color = MaterialTheme.colorScheme.background) {
                    var onboarded by remember { mutableStateOf(repo.isOnboarded) }
                    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

                    if (!onboarded) {
                        OnboardingScreen(onComplete = { key, goal ->
                            viewModel.completeOnboarding(key, goal)
                            onboarded = true
                        })
                    } else {
                        // The nav "stack" is a single state var, so system back has to be
                        // unwound by hand — otherwise it exits the app from a sub-screen.
                        // Mirrors what each screen's own Back button already does.
                        BackHandler(enabled = screen != Screen.Home) {
                            when (screen) {
                                is Screen.AddMeal -> {
                                    viewModel.discardPendingMeal()
                                    screen = Screen.Home
                                }
                                is Screen.DayDetail -> {
                                    viewModel.selectDay(null)
                                    screen = Screen.History
                                }
                                else -> screen = Screen.Home
                            }
                        }

                        when (val current = screen) {
                            Screen.Home -> {
                                val totals by viewModel.todayTotals.collectAsState()
                                val meals by viewModel.todaysMeals.collectAsState()
                                val updateState by viewModel.updateState.collectAsState()
                                // Once per app launch; the ViewModel ignores repeats.
                                LaunchedEffect(Unit) { viewModel.checkForUpdate() }
                                HomeScreen(
                                    totals = totals,
                                    meals = meals,
                                    updateState = updateState,
                                    onGetUpdate = ::openRelease,
                                    onDismissUpdate = viewModel::dismissUpdate,
                                    onAddMeal = { screen = Screen.AddMeal },
                                    onDeleteMeal = { viewModel.deleteMeal(it) },
                                    onOpenHistory = { screen = Screen.History },
                                    onOpenSettings = { screen = Screen.Settings }
                                )
                            }
                            Screen.AddMeal -> {
                                val pending by viewModel.pendingEstimate.collectAsState()
                                AddMealScreen(
                                    pending = pending,
                                    onEstimate = { viewModel.estimateMeal(it) },
                                    onConfirm = {
                                        viewModel.confirmPendingMeal()
                                        screen = Screen.Home
                                    },
                                    onDiscard = { viewModel.discardPendingMeal() },
                                    onBack = {
                                        viewModel.discardPendingMeal()
                                        screen = Screen.Home
                                    }
                                )
                            }
                            Screen.History -> {
                                val days by viewModel.historyDays.collectAsState()
                                val weekly by viewModel.weekly.collectAsState()
                                val goal by viewModel.goal.collectAsState()
                                HistoryScreen(
                                    days = days,
                                    goal = goal,
                                    weekly = weekly,
                                    onDayClick = { key ->
                                        viewModel.selectDay(key)
                                        screen = Screen.DayDetail(key)
                                    },
                                    onBack = { screen = Screen.Home }
                                )
                            }
                            is Screen.DayDetail -> {
                                val meals by viewModel.selectedDayMeals.collectAsState()
                                val goal by viewModel.goal.collectAsState()
                                DayDetailScreen(
                                    dayKey = current.dayKey,
                                    goal = goal,
                                    meals = meals,
                                    onBack = {
                                        viewModel.selectDay(null)
                                        screen = Screen.History
                                    }
                                )
                            }
                            Screen.Settings -> {
                                val apiKey by viewModel.apiKey.collectAsState()
                                val goal by viewModel.goal.collectAsState()
                                val updateState by viewModel.updateState.collectAsState()
                                SettingsScreen(
                                    apiKey = apiKey.orEmpty(),
                                    goal = goal,
                                    themeMode = themeMode,
                                    updateState = updateState,
                                    appVersion = BuildConfig.VERSION_NAME,
                                    onThemeChange = viewModel::setThemeMode,
                                    onCheckForUpdate = { viewModel.checkForUpdate(force = true) },
                                    onOpenRelease = ::openRelease,
                                    onSave = { key, newGoal ->
                                        viewModel.saveSettings(key, newGoal)
                                        screen = Screen.Home
                                    },
                                    onBack = { screen = Screen.Home }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /** Opens a GitHub release page in whatever browser the user has. */
    private fun openRelease(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}
