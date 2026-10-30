package com.prostuti.student.ui.screen.timedexam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.response.MyTimedExamSubmission
import com.prostuti.student.domain.FetchMyTimedSubmissionUseCase
import com.prostuti.student.domain.FetchTimedExamWindowUseCase
import com.prostuti.student.util.TimeProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LockedResultUiState(
    val isLoading: Boolean = true,
    val kind: TimedExamKind = TimedExamKind.LIVE,
    val submission: MyTimedExamSubmission? = null,
    val windowEndEpochMillis: Long = 0L,
    val now: Long = 0L
) {
    val isWindowClosed: Boolean get() = now >= windowEndEpochMillis
    val remainingMillis: Long get() = (windowEndEpochMillis - now).coerceAtLeast(0)
}

/**
 * Nothing about the result is shown here, ever — not even the correct-answer count — until the
 * exam window itself closes (the "সবই লকড" decision). While open this screen just confirms the
 * submission and counts down to when results unlock; once closed it hands off to
 * [WindowRankingViewModel] instead of trying to show anything itself.
 */
class LockedResultViewModel(
    private val examId: String,
    kind: TimedExamKind,
    private val fetchMyTimedSubmissionUseCase: FetchMyTimedSubmissionUseCase,
    private val fetchTimedExamWindowUseCase: FetchTimedExamWindowUseCase,
    private val timeProvider: TimeProvider
) : ViewModel() {
    private val _state = MutableStateFlow(LockedResultUiState(kind = kind))
    val state: StateFlow<LockedResultUiState> = _state.asStateFlow()

    init {
        load()
        startTicker()
    }

    private fun load() {
        viewModelScope.launch {
            val submission = fetchMyTimedSubmissionUseCase(examId)
            val (_, windowEnd) = fetchTimedExamWindowUseCase(examId)
            _state.update { it.copy(isLoading = false, submission = submission, windowEndEpochMillis = windowEnd, now = timeProvider.nowEpochMillis()) }
        }
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                _state.update { it.copy(now = timeProvider.nowEpochMillis()) }
            }
        }
    }
}
