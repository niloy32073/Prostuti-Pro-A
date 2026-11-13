package com.prostuti.student.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.domain.FeatureAccess
import com.prostuti.student.domain.GetMeUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn

/** [FeatureAccess] from the cached profile, for screens that don't already hold it. A failed lookup keeps the last answer. */
class FeatureAccessViewModel(getMe: GetMeUseCase) : ViewModel() {
    val access: StateFlow<FeatureAccess> = getMe(refresh = false)
        .mapNotNull { (it as? ApiResponse.Success)?.data?.let(FeatureAccess::of) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeatureAccess())
}
