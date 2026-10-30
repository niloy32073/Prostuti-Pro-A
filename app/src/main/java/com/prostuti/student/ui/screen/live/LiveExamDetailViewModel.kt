package com.prostuti.student.ui.screen.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.enums.TimedExamState
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MyTimedExamSubmission
import com.prostuti.student.data.model.response.TimedExam
import com.prostuti.student.domain.AdvanceDemoStateUseCase
import com.prostuti.student.domain.FetchLiveExamDetailUseCase
import com.prostuti.student.domain.FetchMyTimedSubmissionUseCase
import com.prostuti.student.domain.TimedExamStateDeriver
import com.prostuti.student.util.TimeProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveExamDetailUiState(
    val isLoading: Boolean = true,
    val exam: TimedExam? = null,
    val mySubmission: MyTimedExamSubmission? = null,
    val now: Long = 0L,
    val errors: Map<String, String> = emptyMap(),
    val isDemoAdvancing: Boolean = false
) {
    val examState: TimedExamState?
        get() = exam?.let { TimedExamStateDeriver.derive(it.windowStartEpochMillis, it.windowEndEpochMillis, mySubmission?.submittedAtEpochMillis, now) }
}

class LiveExamDetailViewModel(
    private val examId: String,
    private val fetchLiveExamDetailUseCase: FetchLiveExamDetailUseCase,
    private val fetchMyTimedSubmissionUseCase: FetchMyTimedSubmissionUseCase,
    private val advanceDemoStateUseCase: AdvanceDemoStateUseCase,
    private val timeProvider: TimeProvider
) : ViewModel() {
    private val _state = MutableStateFlow(LiveExamDetailUiState())
    val state: StateFlow<LiveExamDetailUiState> = _state.asStateFlow()

    init {
        load()
        startTicker()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val r = fetchLiveExamDetailUseCase(examId)) {
                is ApiResponse.Success -> {
                    val submission = fetchMyTimedSubmissionUseCase(examId)
                    _state.update { it.copy(isLoading = false, exam = r.data, mySubmission = submission, now = timeProvider.nowEpochMillis()) }
                }
                is ApiResponse.Failure -> _state.update { it.copy(isLoading = false, errors = it.errors + ("LOAD" to r.message)) }
            }
        }
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                _state.update { it.copy(now = timeProvider.nowEpochMillis()) }
            }
        }
    }

    fun advanceDemoState() {
        viewModelScope.launch {
            _state.update { it.copy(isDemoAdvancing = true) }
            advanceDemoStateUseCase(examId, TimedExamKind.LIVE)
            load()
            _state.update { it.copy(isDemoAdvancing = false) }
        }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
