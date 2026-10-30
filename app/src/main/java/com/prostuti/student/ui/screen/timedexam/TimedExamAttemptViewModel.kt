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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimedExamAttemptUiState(
    val isLoading: Boolean = true,
    val questions: List<Question> = emptyList(),
    val answers: Map<String, String> = emptyMap(),
    val timeRemainingSeconds: Int = 0,
    val totalDurationSeconds: Int = 0,
    val showSubmitDialog: Boolean = false,
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
    private val fetchTimedExamWindowUseCase: FetchTimedExamWindowUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(TimedExamAttemptUiState())
    val state: StateFlow<TimedExamAttemptUiState> = _state.asStateFlow()

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
            val remainingWindowSeconds = ((windowEnd - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            val totalSeconds = minOf(durationMinutes * 60, remainingWindowSeconds)

            _state.update {
                it.copy(
                    isLoading = false, questions = questions, answers = existingAnswers,
                    totalDurationSeconds = totalSeconds, timeRemainingSeconds = totalSeconds
                )
            }
        }
    }

    fun selectAnswer(mcqId: String, optionId: String) {
        if (_state.value.isSubmitted) return
        _state.update { it.copy(answers = it.answers + (mcqId to optionId)) }
        viewModelScope.launch { saveTimedExamAnswerUseCase(examId, kind, mcqId, optionId) }
    }

    fun tickTimer() {
        _state.update { s ->
            if (s.timeRemainingSeconds <= 0 || s.isSubmitted) s
            else s.copy(timeRemainingSeconds = s.timeRemainingSeconds - 1)
        }
        if (_state.value.timeRemainingSeconds <= 0 && !_state.value.isSubmitted) submitExam()
    }

    fun showSubmitDialog() { _state.update { it.copy(showSubmitDialog = true) } }
    fun dismissSubmitDialog() { _state.update { it.copy(showSubmitDialog = false) } }

    fun submitExam() {
        if (_state.value.isSubmitted || _state.value.isSubmitting) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            when (val r = submitTimedExamUseCase(examId, kind)) {
                is ApiResponse.Success -> _state.update { it.copy(isSubmitting = false, isSubmitted = true, showSubmitDialog = false) }
                is ApiResponse.Failure -> _state.update { it.copy(isSubmitting = false, errors = it.errors + ("SUBMIT" to r.message)) }
            }
        }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
