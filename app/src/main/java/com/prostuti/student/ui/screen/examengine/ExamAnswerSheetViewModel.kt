package com.prostuti.student.ui.screen.examengine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MCQReviewQuestionResponse
import com.prostuti.student.domain.GetAttemptReviewUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ERR_LOAD = "LOAD"

enum class AnswerSheetFilter { ALL, CORRECT, WRONG, SKIPPED }

data class ExamAnswerSheetUiState(
    val isLoading: Boolean = true,
    val questions: List<MCQReviewQuestionResponse> = emptyList(),
    val filter: AnswerSheetFilter = AnswerSheetFilter.ALL,
    val expandedMcqId: String? = null,
    val errors: Map<String, String> = emptyMap()
) {
    val filteredQuestions: List<MCQReviewQuestionResponse>
        get() = when (filter) {
            AnswerSheetFilter.ALL -> questions
            AnswerSheetFilter.CORRECT -> questions.filter { it.isCorrect == true }
            AnswerSheetFilter.WRONG -> questions.filter { it.isCorrect == false }
            AnswerSheetFilter.SKIPPED -> questions.filter { it.selectedOptionId == null }
        }

    val correctCount get() = questions.count { it.isCorrect == true }
    val wrongCount get() = questions.count { it.isCorrect == false }
    val skippedCount get() = questions.count { it.selectedOptionId == null }
}

/** Not available at all when the exam's show_explanation_after=NEVER — the review call 403s. */
class ExamAnswerSheetViewModel(
    private val attemptId: String,
    private val getAttemptReviewUseCase: GetAttemptReviewUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(ExamAnswerSheetUiState())
    val state: StateFlow<ExamAnswerSheetUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val r = getAttemptReviewUseCase(attemptId)) {
                is ApiResponse.Success -> _state.update {
                    it.copy(isLoading = false, questions = r.data.questions.sortedBy { q -> q.order })
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isLoading = false, errors = it.errors + (ERR_LOAD to r.message))
                }
            }
        }
    }

    fun setFilter(filter: AnswerSheetFilter) {
        _state.update { it.copy(filter = filter, expandedMcqId = null) }
    }

    fun toggleExpanded(mcqId: String) {
        _state.update { it.copy(expandedMcqId = if (it.expandedMcqId == mcqId) null else mcqId) }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
