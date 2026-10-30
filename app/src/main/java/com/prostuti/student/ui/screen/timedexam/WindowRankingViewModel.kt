package com.prostuti.student.ui.screen.timedexam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.WindowRanking
import com.prostuti.student.domain.FetchWindowRankingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ERR_LOAD = "LOAD"

data class WindowRankingUiState(
    val isLoading: Boolean = true,
    val kind: TimedExamKind = TimedExamKind.LIVE,
    val ranking: WindowRanking? = null,
    val errors: Map<String, String> = emptyMap()
)

class WindowRankingViewModel(
    private val examId: String,
    kind: TimedExamKind,
    private val fetchWindowRankingUseCase: FetchWindowRankingUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(WindowRankingUiState(kind = kind))
    val state: StateFlow<WindowRankingUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val r = fetchWindowRankingUseCase(examId)) {
                is ApiResponse.Success -> _state.update { it.copy(isLoading = false, ranking = r.data) }
                is ApiResponse.Failure -> _state.update { it.copy(isLoading = false, errors = it.errors + (ERR_LOAD to r.message)) }
            }
        }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
