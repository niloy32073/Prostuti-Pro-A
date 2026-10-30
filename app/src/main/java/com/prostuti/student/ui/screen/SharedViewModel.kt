package com.prostuti.student.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.dataStore.PrefKeys
import com.prostuti.student.domain.GetBooleanFromDataStoreUseCase
import com.prostuti.student.domain.GetStringFromDataStoreUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SharedState(
    val isChecked: Boolean = false,
    val isFirstTime: Boolean = true,
    val isLoggedIn: Boolean = false
)

class SharedViewModel(
    getStringUseCase: GetStringFromDataStoreUseCase,
    getBooleanUseCase: GetBooleanFromDataStoreUseCase
) : ViewModel() {
    val state: StateFlow<SharedState> = combine(
        getStringUseCase(PrefKeys.JWT_TOKEN),
        getBooleanUseCase(PrefKeys.IS_FIRST_TIME)
    ) { token, isFirstTime ->
        SharedState(
            isChecked = true,
            isFirstTime = isFirstTime ?: true,
            isLoggedIn = !token.isNullOrBlank()
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SharedState())
}
