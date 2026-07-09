package com.example.silencio.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.silencio.core.worker.CalendarWorker
import com.example.silencio.data.model.VipContact
import com.example.silencio.data.repository.SilencioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val contacts: List<VipContact> = emptyList(),
    val selectedContactIds: Set<Long> = emptySet(),
    val isLoadingContacts: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
            repository.setOnboarded(true)
            CalendarWorker.schedule(context)
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