package com.example.silencio.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.silencio.data.repository.SilencioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preMeetingAlert: Boolean = true,
    val watchedCalendarNames: String = "",
    val availableCalendars: List<Pair<Long, String>> = emptyList(),
    val watchedCalendarIds: Set<Long> = emptySet()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SilencioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _availableCalendars = MutableStateFlow<List<Pair<Long, String>>>(emptyList())

    init {
        observePreferences()
        loadCalendars()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                repository.watchedCalendarIds,
                _availableCalendars
            ) { calendarIds, calendars ->
                SettingsUiState(
                    watchedCalendarIds = calendarIds,
                    watchedCalendarNames = calendars
                        .filter { it.first in calendarIds }
                        .joinToString(", ") { it.second }
                        .ifEmpty { "" },
                    availableCalendars = calendars
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun loadCalendars() {
        viewModelScope.launch {
            _availableCalendars.value = repository.getAvailableCalendars()
        }
    }

    fun setWatchedCalendarIds(ids: Set<Long>) {
        viewModelScope.launch {
            Log.d("Settings", "Saving calendar ids: $ids")
            repository.setWatchedCalendarIds(ids)
        }
    }
}