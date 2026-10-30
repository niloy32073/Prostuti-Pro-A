package com.prostuti.student.ui.screen.examengine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.domain.GetExamAttemptUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExamResultUiState(
    val isLoading: Boolean = true,
    val result: ExamAttemptResultResponse? = null,
    val errors: Map<String, String> = emptyMap()
)

class ExamResultViewModel(
    private val attemptId: String,
    private val getExamAttemptUseCase: GetExamAttemptUseCase
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
                    it.copy(isLoading = false, errors = it.errors + ("LOAD" to r.message))
                }
            }
        }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
