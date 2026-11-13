package com.prostuti.student.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.ThemeMode
import com.prostuti.student.dataStore.PrefKeys
import com.prostuti.student.domain.GetBooleanFromDataStoreUseCase
import com.prostuti.student.domain.GetStringFromDataStoreUseCase
import com.prostuti.student.domain.ObserveThemeModeUseCase
import com.prostuti.student.domain.SyncBookmarksIfNeededUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SharedState(
    val isChecked: Boolean = false,
    val isFirstTime: Boolean = true,
    val isLoggedIn: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

class SharedViewModel(
    getStringUseCase: GetStringFromDataStoreUseCase,
    getBooleanUseCase: GetBooleanFromDataStoreUseCase,
    observeThemeModeUseCase: ObserveThemeModeUseCase,
    syncBookmarksIfNeededUseCase: SyncBookmarksIfNeededUseCase
) : ViewModel() {
    val state: StateFlow<SharedState> = combine(
        getStringUseCase(PrefKeys.JWT_TOKEN),
        getBooleanUseCase(PrefKeys.IS_FIRST_TIME),
        observeThemeModeUseCase()
    ) { token, isFirstTime, themeMode ->
        SharedState(
            isChecked = true,
            isFirstTime = isFirstTime ?: true,
            isLoggedIn = !token.isNullOrBlank(),
            themeMode = themeMode
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SharedState())

    init {
        // App-open check: on each login (and on every cold start while logged in) make sure the
        // bookmark cache is filled. It only calls the server if this user was never synced; a
        // failure (offline) is retried the next time a screen reads bookmarks.
        viewModelScope.launch {
            state.map { it.isLoggedIn }.distinctUntilChanged().filter { it }.collect { syncBookmarksIfNeededUseCase() }
        }
    }
}
