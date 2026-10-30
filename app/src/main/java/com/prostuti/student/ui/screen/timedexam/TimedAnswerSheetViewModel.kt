package com.prostuti.student.ui.screen.timedexam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.TimedExamAnswerSheet
import com.prostuti.student.domain.FetchMyAnswerSheetUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ERR_LOAD = "LOAD"

data class TimedAnswerSheetUiState(
    val isLoading: Boolean = true,
    val sheet: TimedExamAnswerSheet? = null,
    val errors: Map<String, String> = emptyMap()
)

/** Always my own sheet — see the no-peeking-at-others'-answers decision. */
class TimedAnswerSheetViewModel(
    private val examId: String,
    private val fetchMyAnswerSheetUseCase: FetchMyAnswerSheetUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(TimedAnswerSheetUiState())
    val state: StateFlow<TimedAnswerSheetUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val r = fetchMyAnswerSheetUseCase(examId)) {
                is ApiResponse.Success -> _state.update { it.copy(isLoading = false, sheet = r.data) }
                is ApiResponse.Failure -> _state.update { it.copy(isLoading = false, errors = it.errors + (ERR_LOAD to r.message)) }
            }
        }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
