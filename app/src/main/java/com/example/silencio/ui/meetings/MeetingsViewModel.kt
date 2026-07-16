package com.example.silencio.ui.meetings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.silencio.data.model.CalendarEvent
import com.example.silencio.data.repository.SilencioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class MeetingsUiState(
    val meetings: List<CalendarEvent> = emptyList(),
    val todayLabel: String = "",
    val now: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MeetingsViewModel @Inject constructor(
    private val repository: SilencioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeetingsUiState())
    val uiState: StateFlow<MeetingsUiState> = _uiState.asStateFlow()

    init {
        loadMeetings()
        startClock()
    }

    private fun loadMeetings() {
        viewModelScope.launch {
            val meetings = repository.getUpcomingMeetings()
            val todayLabel = SimpleDateFormat(
                "EEEE, d MMM",
                Locale.getDefault()
            ).format(Date())

            _uiState.value = _uiState.value.copy(
                meetings = meetings,
                todayLabel = "Today, $todayLabel",
                isLoading = false
            )
        }
    }

    // Updates "now" every 30 seconds so
    // "NOW" and "In X min" labels stay current
    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                _uiState.value = _uiState.value.copy(
                    now = System.currentTimeMillis()
                )
                loadMeetings()
            }
        }
    }
}