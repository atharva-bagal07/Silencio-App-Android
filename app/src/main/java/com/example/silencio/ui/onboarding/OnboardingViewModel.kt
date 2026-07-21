package com.example.silencio.ui.onboarding


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.silencio.data.repository.SilencioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.delay

data class OnboardingUiState(
    val availableCalendars: List<Pair<Long, String>> = emptyList(),
    val selectedCalendarIds: Set<Long> = emptySet()
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: SilencioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /**
     * Called when calendar permission is granted.
     * Marks onboarding step 1 complete and
     * schedules the background worker.
     */
    fun onCalendarPermissionGranted() {
        viewModelScope.launch {
            repository.getUpcomingMeetings()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboarded(true)
        }
    }

    fun loadCalendars() {
        viewModelScope.launch {
            delay(300)
            val calendars = repository.getAvailableCalendars()
            _uiState.value = _uiState.value.copy(
                availableCalendars = calendars
            )
        }
    }

    fun toggleCalendar(id: Long) {
        val current = _uiState.value.selectedCalendarIds.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _uiState.value = _uiState.value.copy(selectedCalendarIds = current)
    }

    fun saveCalendars() {
        viewModelScope.launch {
            repository.setWatchedCalendarIds(_uiState.value.selectedCalendarIds)
        }
    }
}