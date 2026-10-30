package com.prostuti.student.ui.screen.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.TimedExamState
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.TimedExam
import com.prostuti.student.domain.FetchLiveExamListUseCase
import com.prostuti.student.domain.FetchMyTimedSubmissionUseCase
import com.prostuti.student.domain.TimedExamStateDeriver
import com.prostuti.student.util.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ERR_LOAD = "LOAD"

data class LiveExamRow(val exam: TimedExam, val state: TimedExamState)

data class LiveExamListUiState(
    val isLoading: Boolean = true,
    val rows: List<LiveExamRow> = emptyList(),
    val errors: Map<String, String> = emptyMap()
)

class LiveExamListViewModel(
    private val fetchLiveExamListUseCase: FetchLiveExamListUseCase,
    private val fetchMyTimedSubmissionUseCase: FetchMyTimedSubmissionUseCase,
    private val timeProvider: TimeProvider
) : ViewModel() {
    private val _state = MutableStateFlow(LiveExamListUiState())
    val state: StateFlow<LiveExamListUiState> = _state.asStateFlow()

    init { load() }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val r = fetchLiveExamListUseCase()) {
                is ApiResponse.Success -> {
                    val now = timeProvider.nowEpochMillis()
                    val rows = r.data.map { exam ->
                        val submission = fetchMyTimedSubmissionUseCase(exam.examId)
                        val examState = TimedExamStateDeriver.derive(exam.windowStartEpochMillis, exam.windowEndEpochMillis, submission?.submittedAtEpochMillis, now)
                        LiveExamRow(exam, examState)
                    }
                    _state.update { it.copy(isLoading = false, rows = rows) }
                }
                is ApiResponse.Failure -> _state.update { it.copy(isLoading = false, errors = it.errors + (ERR_LOAD to r.message)) }
            }
        }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
