package com.prostuti.student.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.domain.ClearDataStoreUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(val isLoggedOut: Boolean = false)

class SettingsViewModel(private val clearDataStoreUseCase: ClearDataStoreUseCase) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            clearDataStoreUseCase()
            _state.update { it.copy(isLoggedOut = true) }
        }
    }
}
