package com.prostuti.student.ui.screen.examengine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.domain.GetExamAttemptUseCase
import com.prostuti.student.domain.StartOrResumeExamAttemptUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ERR_LOAD = "LOAD"
private const val ERR_RETAKE = "RETAKE"

data class ExamResultUiState(
    val isLoading: Boolean = true,
    val result: ExamAttemptResultResponse? = null,
    val isStartingRetake: Boolean = false,
    /** One-shot: a new attempt was started on the same exam — open it, then consume. */
    val retakeAttempt: ExamAttemptData? = null,
    val errors: Map<String, String> = emptyMap()
)

class ExamResultViewModel(
    private val attemptId: String,
    private val getExamAttemptUseCase: GetExamAttemptUseCase,
    private val startOrResumeExamAttemptUseCase: StartOrResumeExamAttemptUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(ExamResultUiState())
    val state: StateFlow<ExamResultUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val r = getExamAttemptUseCase(attemptId)) {
                is ApiResponse.Success -> _state.update { it.copy(isLoading = false, result = r.data) }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isLoading = false, errors = it.errors + (ERR_LOAD to r.message))
                }
            }
        }
    }

    /** Starts a new attempt on this result's exam. The old attempt stays in the history. */
    fun retake() {
        val examId = _state.value.result?.examId ?: return
        if (_state.value.isStartingRetake) return
        viewModelScope.launch {
            _state.update { it.copy(isStartingRetake = true) }
            when (val r = startOrResumeExamAttemptUseCase(examId)) {
                is ApiResponse.Success -> _state.update { it.copy(isStartingRetake = false, retakeAttempt = r.data) }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isStartingRetake = false, errors = it.errors + (ERR_RETAKE to r.message))
                }
            }
        }
    }

    fun onRetakeConsumed() { _state.update { it.copy(retakeAttempt = null) } }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
