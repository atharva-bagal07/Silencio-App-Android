package com.silencio.app.ui.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silencio.app.data.model.ReplyContact
import com.silencio.app.data.repository.SilencioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PremiumUiState(
    val isPremium: Boolean = false,
    val customReplyMessage: String = "",
    val replyContacts: List<ReplyContact> = emptyList(),
    val isLoadingContacts: Boolean = true,
    val selectedVipContacts: Map<Long, String> = emptyMap(),
)

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val repository: SilencioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState(isLoadingContacts = true))
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()
    private var vipContactsInitialized = false

    init {
        viewModelScope.launch {
            val savedVipContacts = repository.vipContacts.first()
            _uiState.value = _uiState.value.copy(selectedVipContacts = savedVipContacts)
            vipContactsInitialized = true
            observePreferences()
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                repository.isPremium,
                repository.customReplyMessage
            ) { premium, replyMessage ->
                _uiState.value.copy(
                    isPremium = premium,
                    customReplyMessage = replyMessage
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingContacts = true)
            val contacts = repository.getDeviceContacts()
            _uiState.value = _uiState.value.copy(
                replyContacts = contacts,
                isLoadingContacts = false
            )
        }
    }

    fun setCustomReplyMessage(message: String) {
        viewModelScope.launch {
            repository.setCustomReplyMessage(message)
        }
    }

    fun setPremium(value: Boolean) {
        viewModelScope.launch {
            repository.setPremium(value)
        }
    }

    fun saveVipContacts(onComplete: () -> Unit) {
        viewModelScope.launch {
            val savedContacts = repository.vipContacts.first()
            val newContacts = _uiState.value.selectedVipContacts

            savedContacts.keys.minus(newContacts.keys).forEach { repository.unstarContact(it) }
            newContacts.keys.minus(savedContacts.keys).forEach { repository.starContact(it) }

            repository.setVipContacts(newContacts)
            onComplete()
        }
    }

    fun resetPendingVipContacts() {
        viewModelScope.launch {
            val saved = repository.vipContacts.first()
            _uiState.value = _uiState.value.copy(selectedVipContacts = saved)
        }
    }

    fun toggleVipContact(id: Long, name: String) {
        val current = _uiState.value.selectedVipContacts.toMutableMap()
        if (current.containsKey(id)) current.remove(id) else current[id] = name
        _uiState.value = _uiState.value.copy(selectedVipContacts = current)
    }
}