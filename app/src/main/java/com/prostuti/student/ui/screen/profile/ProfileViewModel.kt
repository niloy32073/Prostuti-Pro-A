package com.prostuti.student.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MeData
import com.prostuti.student.domain.GetMeUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: MeData? = null,
    val errors: Map<String, String> = emptyMap()
)

class ProfileViewModel(private val getMeUseCase: GetMeUseCase) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    // Opening this screen reads the cache (Home already filled it); pull-to-refresh / retry
    // go to the network.
    init { load() }

    fun refresh() = load(refresh = true)

    private fun load(refresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getMeUseCase(refresh).collect { r ->
                when (r) {
                    is ApiResponse.Success -> _state.update { it.copy(isLoading = false, profile = r.data) }
                    is ApiResponse.Failure -> _state.update {
                        it.copy(isLoading = false, errors = it.errors + (ERR_LOAD to r.message))
                    }
                }
            }
            // A failure with nothing cached ends the flow without a Success.
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }

    companion object {
        const val ERR_LOAD = "LOAD_PROFILE"
    }
}
