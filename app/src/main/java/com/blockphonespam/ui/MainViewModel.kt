package com.blockphonespam.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blockphonespam.data.local.BlockedCallEntity
import com.blockphonespam.data.local.PrefixRuleEntity
import com.blockphonespam.data.repository.SpamBlockRepository
import com.blockphonespam.domain.PhoneNumberMatcher
import com.blockphonespam.service.KeepAliveForegroundService
import com.blockphonespam.util.PermissionHelper
import com.blockphonespam.util.ProtectionPermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val blockedCalls: List<BlockedCallEntity> = emptyList(),
    val prefixes: List<PrefixRuleEntity> = emptyList(),
    val newPrefixInput: String = "",
    val prefixError: String? = null,
    val permissionState: ProtectionPermissionState = ProtectionPermissionState(),
    val isServiceRunning: Boolean = false
)

class MainViewModel(
    private val repository: SpamBlockRepository
) : ViewModel() {

    private val newPrefixInput = MutableStateFlow("")
    private val prefixError = MutableStateFlow<String?>(null)
    private val permissionState = MutableStateFlow(ProtectionPermissionState())
    private val isServiceRunning = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repository.ensureDefaultPrefixesSeeded()
        }
    }

    val uiState: StateFlow<MainUiState> = combine(
        repository.observeBlockedCalls(),
        repository.observePrefixes(),
        newPrefixInput,
        prefixError,
        permissionState,
        isServiceRunning
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        MainUiState(
            blockedCalls = values[0] as List<BlockedCallEntity>,
            prefixes = values[1] as List<PrefixRuleEntity>,
            newPrefixInput = values[2] as String,
            prefixError = values[3] as String?,
            permissionState = values[4] as ProtectionPermissionState,
            isServiceRunning = values[5] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    fun refreshSystemState(context: Context) {
        permissionState.value = PermissionHelper.checkAll(context)
        isServiceRunning.value = KeepAliveForegroundService.isRunning.get()
    }

    fun onTogglePrefix(prefix: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.togglePrefix(prefix, enabled)
        }
    }

    fun onNewPrefixInputChange(input: String) {
        val sanitized = input.filter { it.isDigit() || it == '+' || it == ' ' }.take(14)
        newPrefixInput.value = sanitized
        if (prefixError.value != null) {
            prefixError.value = null
        }
    }

    fun onAddPrefix() {
        val candidate = newPrefixInput.value.trim()
        val normalized = PhoneNumberMatcher.normalizePrefix(candidate)
        if (normalized.length < 2) {
            prefixError.value = "Nhập tối thiểu 2 chữ số (VD: 024, 028, 1900)"
            return
        }
        viewModelScope.launch {
            val added = repository.addPrefix(candidate)
            if (added) {
                newPrefixInput.value = ""
                prefixError.value = null
            } else {
                prefixError.value = "Đầu số không hợp lệ"
            }
        }
    }

    fun onDeletePrefix(prefix: String) {
        viewModelScope.launch {
            repository.deletePrefix(prefix)
        }
    }

    fun onClearBlockedCalls() {
        viewModelScope.launch {
            repository.clearBlockedCalls()
        }
    }

    class Factory(private val repository: SpamBlockRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
