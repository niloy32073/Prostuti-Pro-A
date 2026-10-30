package com.prostuti.student.ui.screen.intro

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.R
import com.prostuti.student.dataStore.PrefKeys
import com.prostuti.student.domain.SaveBooleanToDataStoreUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingCard(
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val containerRole: ContainerRole
)

enum class ContainerRole { PRIMARY, SECONDARY, TERTIARY }

data class OnboardingUiState(
    val isFinished: Boolean = false
)

private val cards = listOf(
    OnboardingCard(R.string.onboarding_card1_title, R.string.onboarding_card1_desc, ContainerRole.PRIMARY),
    OnboardingCard(R.string.onboarding_card2_title, R.string.onboarding_card2_desc, ContainerRole.SECONDARY),
    OnboardingCard(R.string.onboarding_card3_title, R.string.onboarding_card3_desc, ContainerRole.TERTIARY),
    OnboardingCard(R.string.onboarding_card4_title, R.string.onboarding_card4_desc, ContainerRole.PRIMARY),
    OnboardingCard(R.string.onboarding_card5_title, R.string.onboarding_card5_desc, ContainerRole.SECONDARY)
)

class OnboardingViewModel(
    private val saveBooleanToDataStoreUseCase: SaveBooleanToDataStoreUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    val currentCards: List<OnboardingCard> = cards

    fun finish() {
        viewModelScope.launch {
            saveBooleanToDataStoreUseCase(PrefKeys.IS_FIRST_TIME, false)
            _state.update { it.copy(isFinished = true) }
        }
    }
}
