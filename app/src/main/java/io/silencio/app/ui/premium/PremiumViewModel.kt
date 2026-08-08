package io.silencio.app.ui.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.silencio.app.data.model.ReplyContact
import io.silencio.app.data.repository.SilencioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.silencio.app.billing.PurchaseManager
import io.silencio.app.billing.PurchaseResult
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

sealed class PurchaseState {
    object Idle : PurchaseState()
    object Loading : PurchaseState()
    object Success : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val repository: SilencioRepository,
    private val purchaseManager: PurchaseManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState(isLoadingContacts = true))
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState

    private var vipContactsInitialized = false

    init {
        viewModelScope.launch {
            // Verify entitlement with RevenueCat on every launch
            val active = purchaseManager.checkEntitlement()
            if (active) repository.setPremium(true)

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

    fun purchasePremium(activity: Activity) {
        viewModelScope.launch {
            _purchaseState.value = PurchaseState.Loading
            when (val result = purchaseManager.purchaseLifetime(activity)) {
                is PurchaseResult.Success -> {
                    repository.setPremium(true)
                    _purchaseState.value = PurchaseState.Success
                }

                is PurchaseResult.Cancelled -> {
                    _purchaseState.value = PurchaseState.Idle
                }

                is PurchaseResult.Error -> {
                    _purchaseState.value = PurchaseState.Error(result.message)
                }
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _purchaseState.value = PurchaseState.Loading
            when (val result = purchaseManager.restorePurchases()) {
                is PurchaseResult.Success -> {
                    repository.setPremium(true)
                    _purchaseState.value = PurchaseState.Success
                }

                is PurchaseResult.Cancelled -> {
                    _purchaseState.value = PurchaseState.Idle
                }

                is PurchaseResult.Error -> {
                    _purchaseState.value = PurchaseState.Error(result.message)
                }
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