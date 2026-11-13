package com.prostuti.student.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.ThemeMode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.domain.ClearCachedMeUseCase
import com.prostuti.student.domain.ClearDataStoreUseCase
import com.prostuti.student.domain.ClearBookmarksUseCase
import com.prostuti.student.domain.ClearReportsUseCase
import com.prostuti.student.domain.GetMeUseCase
import com.prostuti.student.domain.ObserveThemeModeUseCase
import com.prostuti.student.domain.SetThemeModeUseCase
import com.prostuti.student.domain.notification.ClearPushTokenUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoggedOut: Boolean = false,
    val userName: String? = null,
    val profileImage: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Placeholders: shown in the design but not wired to anything yet, and not saved.
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = false
)

class SettingsViewModel(
    private val clearDataStoreUseCase: ClearDataStoreUseCase,
    private val clearCachedMeUseCase: ClearCachedMeUseCase,
    private val clearBookmarksUseCase: ClearBookmarksUseCase,
    private val clearReportsUseCase: ClearReportsUseCase,
    private val getMeUseCase: GetMeUseCase,
    private val observeThemeModeUseCase: ObserveThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val clearPushTokenUseCase: ClearPushTokenUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeThemeModeUseCase().collect { mode -> _state.update { it.copy(themeMode = mode) } }
        }
        viewModelScope.launch {
            // Cached profile for the header card; a failure just leaves the card without a name.
            getMeUseCase(refresh = false).collect { r ->
                if (r is ApiResponse.Success) {
                    _state.update {
                        it.copy(
                            userName = "${r.data.firstName} ${r.data.lastName}".trim().ifBlank { r.data.username },
                            profileImage = r.data.profileImage
                        )
                    }
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) = setThemeModeUseCase(mode)

    fun setHapticsEnabled(enabled: Boolean) = _state.update { it.copy(hapticsEnabled = enabled) }

    fun setSoundEnabled(enabled: Boolean) = _state.update { it.copy(soundEnabled = enabled) }

    fun logout() {
        viewModelScope.launch {
            // While the session token still exists: stop pushes to this device. Offline or failed →
            // log out anyway; the next sign-in on any device overwrites the server's token.
            clearPushTokenUseCase()
            clearDataStoreUseCase()
            clearCachedMeUseCase()
            clearBookmarksUseCase()
            clearReportsUseCase()
            _state.update { it.copy(isLoggedOut = true) }
        }
    }
}
