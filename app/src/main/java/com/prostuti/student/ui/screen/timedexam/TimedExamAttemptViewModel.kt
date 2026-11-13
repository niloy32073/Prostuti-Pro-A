package com.prostuti.student.ui.screen.timedexam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Question
import com.prostuti.student.domain.AutoSubmitAbandonedAttemptUseCase
import com.prostuti.student.domain.FetchInProgressAnswersUseCase
import com.prostuti.student.domain.FetchTimedExamDurationUseCase
import com.prostuti.student.domain.FetchTimedExamQuestionsUseCase
import com.prostuti.student.domain.FetchTimedExamWindowUseCase
import com.prostuti.student.domain.SaveTimedExamAnswerUseCase
import com.prostuti.student.domain.SubmitTimedExamUseCase
import com.prostuti.student.util.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ERR_SUBMIT = "SUBMIT"

data class TimedExamAttemptUiState(
    val isLoading: Boolean = true,
    val questions: List<Question> = emptyList(),
    val answers: Map<String, String> = emptyMap(),
    val timeRemainingSeconds: Int = 0,
    val totalDurationSeconds: Int = 0,
    val showSubmitDialog: Boolean = false,
    /** The exam time ran out: answering is locked, the attempt submits on its own and the
     *  screen shows the "time is up" dialog instead of jumping straight to the result. */
    val isTimeUp: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    /** True only when this screen auto-submitted an attempt abandoned on a previous visit —
     * the caller should skip straight to the locked-result screen without ever showing a
     * question (see the Q12 auto-submit-on-return decision). */
    val skippedToResult: Boolean = false,
    val errors: Map<String, String> = emptyMap()
)

/**
 * The shared one-shot attempt screen for live AND group exams — unlike the practice-exam engine
 * ([com.prostuti.student.ui.screen.examengine.ExamAttemptViewModel]), every answer is saved to
 * Room right away ([SaveTimedExamAnswerUseCase]), which is what lets [AutoSubmitAbandonedAttemptUseCase]
 * finish an attempt the student abandoned by closing the app mid-exam.
 */
class TimedExamAttemptViewModel(
    private val examId: String,
    private val kind: TimedExamKind,
    private val fetchTimedExamQuestionsUseCase: FetchTimedExamQuestionsUseCase,
    private val fetchInProgressAnswersUseCase: FetchInProgressAnswersUseCase,
    private val saveTimedExamAnswerUseCase: SaveTimedExamAnswerUseCase,
    private val submitTimedExamUseCase: SubmitTimedExamUseCase,
    private val autoSubmitAbandonedAttemptUseCase: AutoSubmitAbandonedAttemptUseCase,
    private val fetchTimedExamDurationUseCase: FetchTimedExamDurationUseCase,
    private val fetchTimedExamWindowUseCase: FetchTimedExamWindowUseCase,
    private val timeProvider: TimeProvider
) : ViewModel() {
    private val _state = MutableStateFlow(TimedExamAttemptUiState())
    val state: StateFlow<TimedExamAttemptUiState> = _state.asStateFlow()

    /** When the time runs out (epoch millis). The clock runs on real time, so time spent with the
     *  screen locked or the app in the background still counts. */
    private var deadlineEpochMillis = 0L

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val autoSubmission = autoSubmitAbandonedAttemptUseCase(examId, kind)
            if (autoSubmission != null) {
                _state.update { it.copy(isLoading = false, isSubmitted = true, skippedToResult = true) }
                return@launch
            }

            val questions = fetchTimedExamQuestionsUseCase()
            val existingAnswers = fetchInProgressAnswersUseCase(examId)
            val durationMinutes = fetchTimedExamDurationUseCase(examId)
            val (_, windowEnd) = fetchTimedExamWindowUseCase(examId)
            val now = timeProvider.nowEpochMillis()
            val remainingWindowSeconds = ((windowEnd - now) / 1000).toInt().coerceAtLeast(0)
            val totalSeconds = minOf(durationMinutes * 60, remainingWindowSeconds)
            deadlineEpochMillis = now + totalSeconds * 1000L

            _state.update {
                it.copy(
                    isLoading = false, questions = questions, answers = existingAnswers,
                    totalDurationSeconds = totalSeconds, timeRemainingSeconds = totalSeconds
                )
            }
            if (totalSeconds == 0) onTimeUp()
        }
    }

    /** The first pick is final: once a question has an answer, later picks are ignored. */
    fun selectAnswer(mcqId: String, optionId: String) {
        var accepted = false
        _state.update {
            accepted = mcqId !in it.answers && !it.isSubmitted && !it.isTimeUp
            if (accepted) it.copy(answers = it.answers + (mcqId to optionId)) else it
        }
        if (!accepted) return
        viewModelScope.launch { saveTimedExamAnswerUseCase(examId, kind, mcqId, optionId) }
    }

    /** Re-reads the remaining time from the clock; called every second and when the screen returns. */
    fun tickTimer() {
        _state.update { s ->
            if (s.isLoading || s.isSubmitted) return@update s
            val remaining = ((deadlineEpochMillis - timeProvider.nowEpochMillis() + 999) / 1000).toInt()
            s.copy(timeRemainingSeconds = remaining.coerceIn(0, s.totalDurationSeconds))
        }
        val s = _state.value
        if (!s.isLoading && s.timeRemainingSeconds == 0) onTimeUp()
    }

    /** Time is over: lock answering and submit without asking. */
    private fun onTimeUp() {
        if (_state.value.isSubmitted) return
        _state.update { it.copy(isTimeUp = true, showSubmitDialog = false) }
        submitExam()
    }

    fun showSubmitDialog() { _state.update { if (it.isTimeUp) it else it.copy(showSubmitDialog = true) } }
    fun dismissSubmitDialog() { _state.update { it.copy(showSubmitDialog = false) } }

    fun submitExam() {
        if (_state.value.isSubmitted || _state.value.isSubmitting) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            when (val r = submitTimedExamUseCase(examId, kind)) {
                is ApiResponse.Success -> _state.update { it.copy(isSubmitting = false, isSubmitted = true, showSubmitDialog = false) }
                is ApiResponse.Failure -> _state.update { it.copy(isSubmitting = false, errors = it.errors + (ERR_SUBMIT to r.message)) }
            }
        }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
