package com.example.silencio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.silencio.core.dnd.DndManager
import com.example.silencio.core.worker.CalendarWorker
import com.example.silencio.data.model.CalendarEvent
import com.example.silencio.data.repository.SilencioRepository
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
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
    @ApplicationContext private val context: Context,
    private val repository: SilencioRepository,
    private val dndManager: DndManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val isOnboarded = repository.isOnboarded
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val autoSilenceEnabled = repository.autoSilenceEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    init {
        observeSessionState()
        refreshEvents()
        startPeriodicRefresh()
    }

    private fun observeSessionState() {
        viewModelScope.launch {
            repository.isCurrentlyActive.collect { isActive ->
                _uiState.value = _uiState.value.copy(isActive = isActive)
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

    private fun refreshEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

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

    /**
     * Refreshes event state every 60 seconds while
     * the home screen is visible.
     * WorkManager handles the background work —
     * this just keeps the UI current.
     */
    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                refreshEvents()
            }
        }
    }

    fun onDndPermissionGranted() {
        CalendarWorker.schedule(context)
        refreshEvents()
    }

    fun onResume() {
        refreshEvents()
    }
}