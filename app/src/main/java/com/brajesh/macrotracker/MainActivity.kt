package com.brajesh.macrotracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brajesh.macrotracker.data.AppDatabase
import com.brajesh.macrotracker.data.SettingsStore
import com.brajesh.macrotracker.repository.MacroRepository
import com.brajesh.macrotracker.ui.AddMealScreen
import com.brajesh.macrotracker.ui.HomeScreen
import com.brajesh.macrotracker.ui.OnboardingScreen
import com.brajesh.macrotracker.viewmodel.MacroViewModel

private enum class Screen { HOME, ADD_MEAL }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = SettingsStore(applicationContext)
        val dao = AppDatabase.get(applicationContext).mealDao()
        val repo = MacroRepository(dao, settings)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier) {
                    val viewModel: MacroViewModel = viewModel(factory = MacroViewModel.Factory(repo))
                    var onboarded by remember { mutableStateOf(repo.isOnboarded) }
                    var screen by remember { mutableStateOf(Screen.HOME) }

                    if (!onboarded) {
                        OnboardingScreen(onComplete = { key, goal ->
                            viewModel.completeOnboarding(key, goal)
                            onboarded = true
                        })
                    } else {
                        when (screen) {
                            Screen.HOME -> {
                                val totals by viewModel.todayTotals.collectAsState()
                                val meals by viewModel.todaysMeals.collectAsState()
                                HomeScreen(
                                    totals = totals,
                                    meals = meals,
                                    onAddMeal = { screen = Screen.ADD_MEAL },
                                    onDeleteMeal = { viewModel.deleteMeal(it) }
                                )
                            }
                            Screen.ADD_MEAL -> {
                                val pending by viewModel.pendingEstimate.collectAsState()
                                AddMealScreen(
                                    pending = pending,
                                    onEstimate = { viewModel.estimateMeal(it) },
                                    onConfirm = {
                                        viewModel.confirmPendingMeal()
                                        screen = Screen.HOME
                                    },
                                    onDiscard = { viewModel.discardPendingMeal() },
                                    onBack = {
                                        viewModel.discardPendingMeal()
                                        screen = Screen.HOME
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
