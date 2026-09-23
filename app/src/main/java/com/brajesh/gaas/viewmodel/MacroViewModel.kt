package com.brajesh.gaas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brajesh.gaas.data.DaySummary
import com.brajesh.gaas.data.MacroGoal
import com.brajesh.gaas.data.MealEntry
import com.brajesh.gaas.network.GeminiResult
import com.brajesh.gaas.network.ParsedMeal
import com.brajesh.gaas.repository.DaySlice
import com.brajesh.gaas.repository.DayTotals
import com.brajesh.gaas.repository.MacroRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class PendingEstimate {
    object Idle : PendingEstimate()
    object Loading : PendingEstimate()
    data class Ready(val rawText: String, val parsed: ParsedMeal) : PendingEstimate()
    data class Failed(val message: String) : PendingEstimate()
}

@OptIn(ExperimentalCoroutinesApi::class)
class MacroViewModel(private val repo: MacroRepository) : ViewModel() {

    private val dayKey = repo.todayKey()

    val todaysMeals: StateFlow<List<MealEntry>> =
        repo.mealsForDay(dayKey).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTotals: StateFlow<DayTotals> =
        todaysMeals.map { meals -> repo.computeTotals(meals) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repo.computeTotals(emptyList()))

    /** The user's current goals, kept in sync as onboarding/updates write them. */
    private val _goal = MutableStateFlow(repo.goal())
    val goal: StateFlow<MacroGoal?> = _goal

    /** Per-day totals for the history list, newest first — powered by the GROUP BY query. */
    val historyDays: StateFlow<List<DaySummary>> =
        repo.daySummaries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Last 7 days (oldest → newest) padded with hasData=false for days without meals. */
    val weekly: StateFlow<List<DaySlice>> =
        historyDays.map(repo::recentDailySlices)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Meals for whatever day is opened in the history detail screen, or empty when none selected. */
    private val _selectedDayKey = MutableStateFlow<String?>(null)
    val selectedDayMeals: StateFlow<List<MealEntry>> =
        _selectedDayKey.flatMapLatest { key ->
            if (key == null) flowOf(emptyList()) else repo.mealsForDay(key)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDay(dayKey: String?) {
        _selectedDayKey.value = dayKey
    }

    private val _pendingEstimate = MutableStateFlow<PendingEstimate>(PendingEstimate.Idle)
    val pendingEstimate: StateFlow<PendingEstimate> = _pendingEstimate

    val isOnboarded: Boolean get() = repo.isOnboarded

    fun completeOnboarding(apiKey: String, goal: MacroGoal) {
        repo.setApiKey(apiKey)
        repo.setGoal(goal)
        _goal.value = goal
    }

    fun updateGoal(goal: MacroGoal) {
        repo.setGoal(goal)
        _goal.value = goal
    }

    /** Sends the verbose meal description to Gemini and stages the result for user confirmation. */
    fun estimateMeal(description: String) {
        if (description.isBlank()) return
        _pendingEstimate.value = PendingEstimate.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repo.estimateMeal(description) }
            when (result) {
                is GeminiResult.Success ->
                    _pendingEstimate.value = PendingEstimate.Ready(description, result.meal)
                is GeminiResult.ApiError ->
                    _pendingEstimate.value = PendingEstimate.Failed(result.message)
                is GeminiResult.ParseError ->
                    _pendingEstimate.value = PendingEstimate.Failed(result.message)
                is GeminiResult.NetworkError ->
                    _pendingEstimate.value = PendingEstimate.Failed("Network error: ${result.cause.message}")
            }
        }
    }

    /** User confirmed the parsed estimate on the review screen — persist it. */
    fun confirmPendingMeal() {
        val current = _pendingEstimate.value
        if (current is PendingEstimate.Ready) {
            viewModelScope.launch {
                repo.logMeal(current.rawText, current.parsed)
                _pendingEstimate.value = PendingEstimate.Idle
            }
        }
    }

    fun discardPendingMeal() {
        _pendingEstimate.value = PendingEstimate.Idle
    }

    fun deleteMeal(entry: MealEntry) = viewModelScope.launch { repo.deleteMeal(entry) }

    class Factory(private val repo: MacroRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MacroViewModel(repo) as T
    }
}
