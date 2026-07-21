package com.example.silencio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.silencio.data.model.CalendarEvent
import com.example.silencio.data.repository.SilencioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val currentEvent: CalendarEvent? = null,
    val nextEvent: CalendarEvent? = null,
    val isActive: Boolean = false,
    val silenceStartTime: Long? = null,
    val notificationsHeld: Long = 0L,
    val hasDndPermission: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SilencioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val isOnboarded: StateFlow<Boolean?> = repository.isOnboarded
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )
    init {
        _uiState.value = _uiState.value.copy(isLoading = true)
        observeSessionState()
        refreshEvents()
    }

    private fun observeSessionState() {
        viewModelScope.launch {
            repository.isCurrentlyActive.collect { isActive ->
                _uiState.value = _uiState.value.copy(isActive = isActive)
                if (isActive) refreshEvents()
            }
        }

        viewModelScope.launch {
            repository.silenceStartTime.collect { startTime ->
                _uiState.value = _uiState.value.copy(
                    silenceStartTime = startTime
                )
            }
        }

        viewModelScope.launch {
            repository.notificationsHeldCount.collect { count ->
                _uiState.value = _uiState.value.copy(
                    notificationsHeld = count
                )
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboarded(true)
        }
    }

    private fun refreshEvents() {
        viewModelScope.launch {
            val currentEvent = repository.getCurrentEvent()
            val nextEvent = repository.getNextEvent()
            val hasDndPermission = repository.hasDndPermission()

            _uiState.value = _uiState.value.copy(
                currentEvent = currentEvent,
                nextEvent = nextEvent,
                hasDndPermission = hasDndPermission,
                isLoading = false
            )
        }
    }

    fun onDndPermissionGranted() {
        viewModelScope.launch {
            repository.getUpcomingMeetings()
        }
        refreshEvents()
    }

    fun onResume() {
        refreshEvents()
    }
}