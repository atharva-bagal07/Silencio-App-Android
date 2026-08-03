package io.silencio.app.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.silencio.app.data.model.CalendarEvent
import io.silencio.app.data.repository.SilencioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val currentEvent: CalendarEvent? = null,
    val nextEvent: CalendarEvent? = null,
    val upcomingEvents: List<CalendarEvent> = emptyList(),
    val isActive: Boolean = false,
    val silenceStartTime: Long? = null,
    val notificationsHeld: Long = 0L,
    val hasDndPermission: Boolean = false,
    val isLoading: Boolean = true,
    val dndEverGranted: Boolean = false,
    val hasCalendarPermission: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SilencioRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            hasDndPermission = repository.hasDndPermission(),
            isLoading = true
        )
    )

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val isOnboarded: StateFlow<Boolean?> = repository.isOnboarded
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    init {
        viewModelScope.launch {
            val everGranted = repository.dndPermissionGranted.first()
            _uiState.value = _uiState.value.copy(
                hasDndPermission = repository.hasDndPermission() || everGranted,
                dndEverGranted = everGranted,
                isLoading = true
            )
            observeSessionState()
            refreshEvents()
        }
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
        val hasDndPermission = repository.hasDndPermission()
        val hasCalendarPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        _uiState.value = _uiState.value.copy(
            hasCalendarPermission = hasCalendarPermission
        )

        viewModelScope.launch {
            if (hasDndPermission) {
                repository.setDndPermissionGranted(true)
            }

            val everGranted = repository.dndPermissionGranted.first()

            _uiState.value = _uiState.value.copy(
                hasDndPermission = hasDndPermission || everGranted,
                dndEverGranted = everGranted || hasDndPermission
            )

            val currentEvent = repository.getCurrentEvent()
            val nextEvent = repository.getNextEvent()
            val upcoming = repository.getUpcomingMeetings()
                .filter { it.startTime > System.currentTimeMillis() }
                .take(3)

            _uiState.value = _uiState.value.copy(
                currentEvent = currentEvent,
                nextEvent = nextEvent,
                upcomingEvents = upcoming,
                isLoading = false
            )
        }
    }

    fun onResume() {
        Log.d("HomeViewModel", "onResume called")
        _uiState.value = _uiState.value.copy(isLoading = true)
        _uiState.value = _uiState.value.copy(
            hasDndPermission = repository.hasDndPermission()
        )
        refreshEvents()
    }
}