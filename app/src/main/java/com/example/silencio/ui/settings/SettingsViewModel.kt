package com.example.silencio.ui.settings

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
    val autoSilenceEnabled: Boolean = true,
    val vibrateInstead: Boolean = false,
    val preMeetingAlert: Boolean = true,
    val vipContactCount: Int = 0,
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

    init {
        observePreferences()
        loadCalendars()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                repository.autoSilenceEnabled,
                repository.vibrateInstead,
                repository.preMeetingAlert,
                repository.vipContactIds,
                repository.watchedCalendarIds
            ) { autoSilence, vibrate, preAlert, vipIds, calendarIds ->
                SettingsUiState(
                    autoSilenceEnabled = autoSilence,
                    vibrateInstead = vibrate,
                    preMeetingAlert = preAlert,
                    vipContactCount = vipIds.size,
                    watchedCalendarIds = calendarIds,
                    watchedCalendarNames = buildCalendarNames(calendarIds),
                    availableCalendars = _uiState.value.availableCalendars
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun loadCalendars() {
        viewModelScope.launch {
            val calendars = repository.getAvailableCalendars()
            _uiState.value = _uiState.value.copy(
                availableCalendars = calendars
            )
        }
    }

    private fun buildCalendarNames(ids: Set<Long>): String {
        if (ids.isEmpty()) return ""
        val available = _uiState.value.availableCalendars
        return available
            .filter { it.first in ids }
            .joinToString(", ") { it.second }
            .ifEmpty { "" }
    }

    fun setAutoSilenceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSilenceEnabled(enabled)
        }
    }

    fun setVibrateInstead(vibrate: Boolean) {
        viewModelScope.launch {
            repository.setVibrateInstead(vibrate)
        }
    }

    fun setPreMeetingAlert(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPreMeetingAlert(enabled)
        }
    }

    fun setWatchedCalendarIds(ids: Set<Long>) {
        viewModelScope.launch {
            repository.setWatchedCalendarIds(ids)
            _uiState.value = _uiState.value.copy(
                watchedCalendarIds = ids,
                watchedCalendarNames = buildCalendarNames(ids)
            )
        }
    }

    fun setVipContactIds(ids: Set<Long>) {
        viewModelScope.launch {
            repository.setVipContactIds(ids)
        }
    }
}