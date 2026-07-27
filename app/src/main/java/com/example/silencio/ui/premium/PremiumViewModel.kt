package com.example.silencio.ui.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.silencio.data.model.ReplyContact
import com.example.silencio.data.repository.SilencioRepository
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
    val selectedReplyContactNames: Set<String> = emptySet(),
    val isLoadingContacts: Boolean = true
)

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val repository: SilencioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState(isLoadingContacts = true))
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()
    private var contactsInitialized = false

    init {
        viewModelScope.launch {
            val savedNames = repository.replyContactNames.first()
            _uiState.value = _uiState.value.copy(selectedReplyContactNames = savedNames)
            contactsInitialized = true
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

    fun toggleContact(name: String) {
        val current = _uiState.value.selectedReplyContactNames.toMutableSet()
        if (current.contains(name)) current.remove(name) else current.add(name)
        _uiState.value = _uiState.value.copy(selectedReplyContactNames = current)
    }

    fun saveReplyContacts() {
        viewModelScope.launch {
            repository.setReplyContactNames(_uiState.value.selectedReplyContactNames)
        }
    }

    fun resetPendingContacts() {
        viewModelScope.launch {
            val savedNames = repository.replyContactNames.first()
            _uiState.value = _uiState.value.copy(selectedReplyContactNames = savedNames)
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
}