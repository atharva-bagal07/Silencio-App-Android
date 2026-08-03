package com.silencio.app.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silencio.app.data.model.ReplyContact
import com.silencio.app.data.repository.SilencioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val watchedCalendarNames: String = "",
    val availableCalendars: List<Pair<Long, String>> = emptyList(),
    val watchedCalendarIds: Set<Long> = emptySet(),
    val isPremium: Boolean = false,
    val customReplyMessage: String = "",
    val replyContacts: List<ReplyContact> = emptyList(),
    val selectedReplyContactNames: Set<String> = emptySet()
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
                _availableCalendars,
                repository.isPremium,
                repository.customReplyMessage,
                repository.vipContacts
            ) { args ->
                val calendarIds = args[0] as Set<Long>
                val calendars = args[1] as List<Pair<Long, String>>
                val premium = args[2] as Boolean
                val replyMessage = args[3] as String
                val replyContactNames = args[4] as Set<String>

                SettingsUiState(
                    watchedCalendarIds = calendarIds,
                    watchedCalendarNames = calendars
                        .filter { it.first in calendarIds }
                        .joinToString(", ") { it.second }
                        .ifEmpty { "" },
                    availableCalendars = calendars,
                    isPremium = premium,
                    customReplyMessage = replyMessage,
                    selectedReplyContactNames = replyContactNames
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


    fun setPremium(value: Boolean) {
        viewModelScope.launch { repository.setPremium(value) }
    }

    fun setWatchedCalendarIds(ids: Set<Long>) {
        viewModelScope.launch {
            Log.d("Settings", "Saving calendar ids: $ids")
            repository.setWatchedCalendarIds(ids)
        }
    }
}