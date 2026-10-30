package com.prostuti.student.ui.screen.examengine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.request.ExamResponseItem
import com.prostuti.student.data.model.request.ExamResponseSubmitRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.model.response.Question
import com.prostuti.student.domain.FetchPassageUseCase
import com.prostuti.student.domain.GetAttemptQuestionsUseCase
import com.prostuti.student.domain.SubmitExamAttemptUseCase
import com.prostuti.student.domain.SubmitExamResponsesUseCase
import com.prostuti.student.domain.SubmitMcqReportUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExamAttemptUiState(
    val attemptId: String = "",
    val questions: List<Question> = emptyList(),
    val selectedAnswers: Map<String, String> = emptyMap(),
    val passages: Map<String, FetchPassageData> = emptyMap(),
    val totalDurationSeconds: Int = 0,
    val timeRemainingSeconds: Int = 0,
    val showSubmitDialog: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val favoriteMcqIds: Set<String> = emptySet(),
    val reportResultMessage: String? = null,
    val errors: Map<String, String> = emptyMap()
)

/**
 * Drives the attempt (question-answering) flow shared by every exam mode — practice, live and
 * group all finalize to the same `/exams/attempts/{id}/submit`. Mode-specific behaviour (locked
 * result, ranking, …) lives downstream in the Result/AnswerSheet/Leaderboard screens, not here.
 */
class ExamAttemptViewModel(
    private val attemptId: String,
    private val fetchPassageUseCase: FetchPassageUseCase,
    private val getAttemptQuestionsUseCase: GetAttemptQuestionsUseCase,
    private val submitExamResponsesUseCase: SubmitExamResponsesUseCase,
    private val submitExamAttemptUseCase: SubmitExamAttemptUseCase,
    private val submitMcqReportUseCase: SubmitMcqReportUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ExamAttemptUiState(attemptId = attemptId))
    val state: StateFlow<ExamAttemptUiState> = _state.asStateFlow()

    init {
        val stashed = ExamAttemptStore.consume()
        if (stashed != null) {
            applyAttemptData(stashed)
        } else {
            resumeAttempt()
        }
    }

    private fun resumeAttempt() {
        viewModelScope.launch {
            when (val r = getAttemptQuestionsUseCase(attemptId)) {
                is ApiResponse.Success -> applyAttemptData(r.data)
                is ApiResponse.Failure -> _state.update {
                    it.copy(errors = it.errors + ("RESUME" to r.message))
                }
            }
        }
    }

    private fun applyAttemptData(data: ExamAttemptData) {
        _state.update {
            it.copy(
                questions = data.questions,
                totalDurationSeconds = data.duration * 60,
                timeRemainingSeconds = data.duration * 60
            )
        }
        prefetchPassages(data.questions)
    }

    private fun prefetchPassages(questions: List<Question>) {
        val passageIds = questions.mapNotNull { it.passageId }.distinct()
        passageIds.forEach { passageId ->
            viewModelScope.launch {
                when (val r = fetchPassageUseCase(passageId)) {
                    is ApiResponse.Success ->
                        _state.update { it.copy(passages = it.passages + (passageId to r.data)) }
                    is ApiResponse.Failure ->
                        _state.update { it.copy(errors = it.errors + ("PASSAGE_$passageId" to r.message)) }
                }
            }
        }
    }

    fun selectAnswer(mcqId: String, optionId: String) {
        _state.update { it.copy(selectedAnswers = it.selectedAnswers + (mcqId to optionId)) }

        val elapsedMs = ((_state.value.totalDurationSeconds - _state.value.timeRemainingSeconds) * 1000)
            .coerceAtLeast(0)
        viewModelScope.launch {
            submitExamResponsesUseCase(
                attemptId,
                ExamResponseSubmitRequest(
                    responses = listOf(ExamResponseItem(mcqId = mcqId, selectedOptionId = optionId, responseTimeMs = elapsedMs))
                )
            )
            // Best-effort sync — a failure here just means the answer stays local until the
            // final submit; selectedAnswers is always the source of truth for the UI.
        }
    }

    fun tickTimer() {
        _state.update { s ->
            val next = (s.timeRemainingSeconds - 1).coerceAtLeast(0)
            s.copy(
                timeRemainingSeconds = next,
                showSubmitDialog = if (next == 0 && !s.isSubmitted) true else s.showSubmitDialog
            )
        }
    }

    fun showSubmitDialog() { _state.update { it.copy(showSubmitDialog = true) } }
    fun dismissSubmitDialog() { _state.update { it.copy(showSubmitDialog = false) } }

    fun submitExam() {
        if (_state.value.isSubmitting || _state.value.isSubmitted) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, showSubmitDialog = false) }
            when (val r = submitExamAttemptUseCase(attemptId)) {
                is ApiResponse.Success -> _state.update { it.copy(isSubmitting = false, isSubmitted = true) }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isSubmitting = false, errors = it.errors + ("SUBMIT" to r.message))
                }
            }
        }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }

    fun toggleFavorite(mcqId: String) {
        // TODO: persist with a ToggleFavoriteUseCase once a favorite-MCQ API endpoint exists.
        _state.update { s ->
            val updated = if (mcqId in s.favoriteMcqIds) s.favoriteMcqIds - mcqId else s.favoriteMcqIds + mcqId
            s.copy(favoriteMcqIds = updated)
        }
    }

    fun submitReport(mcqId: String, reason: String, description: String) {
        viewModelScope.launch {
            when (val r = submitMcqReportUseCase(mcqId, reason, description.ifBlank { null })) {
                is ApiResponse.Success -> _state.update {
                    it.copy(reportResultMessage = "প্রশ্নটি রিপোর্ট করা হয়েছে।")
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(errors = it.errors + ("REPORT" to r.message))
                }
            }
        }
    }

    fun clearReportResultMessage() { _state.update { it.copy(reportResultMessage = null) } }
}
