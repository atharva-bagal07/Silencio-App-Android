package com.example.silencio.ui.onboarding


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.silencio.data.model.VipContact
import com.example.silencio.data.repository.SilencioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val contacts: List<VipContact> = emptyList(),
    val selectedContactIds: Set<Long> = emptySet(),
    val isLoadingContacts: Boolean = false,
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

    /**
     * Loads device contacts for VIP selection screen.
     * Called when VIP contact screen becomes visible.
     */
    fun loadContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingContacts = true)

            val contacts = repository.getDeviceContacts()

            // Pre-select any previously saved VIP contacts
            val savedIds = repository.vipContactIds
            savedIds.collect { ids ->
                _uiState.value = _uiState.value.copy(
                    contacts = contacts,
                    selectedContactIds = ids,
                    isLoadingContacts = false
                )
            }
        }
    }

    fun loadCalendars() {
        viewModelScope.launch {
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

    /**
     * Toggles a contact in/out of the VIP selection.
     */
    fun toggleContact(contactId: Long) {
        val current = _uiState.value.selectedContactIds.toMutableSet()
        if (current.contains(contactId)) {
            current.remove(contactId)
        } else {
            current.add(contactId)
        }
        _uiState.value = _uiState.value.copy(selectedContactIds = current)
    }

    fun onDndGranted() {
        viewModelScope.launch {
            repository.setOnboarded(true)
        }
    }

    /**
     * Saves selected VIP contacts and completes onboarding.
     */
    fun saveVipContacts() {
        viewModelScope.launch {
            repository.setVipContactIds(
                _uiState.value.selectedContactIds
            )
        }
    }
}