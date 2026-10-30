package com.prostuti.student.ui.screen.qb

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class QuestionReaderUiState(
    val questions: List<DummyQuestion> = emptyList(),
    val revealedAnswers: Set<String> = emptySet(),
    val showAllAnswers: Boolean = false,
    val favoriteIds: Set<String> = emptySet()
)

class QuestionReaderViewModel : ViewModel() {
    // TODO: Inject FetchExamQuestionsUseCase(examId) when API is ready
    //       API endpoint needed: GET /mcq/exam/{examId}/questions (read-only, no attempt created)
    //       The examId is passed via the QuestionReader route.

    private val _state = MutableStateFlow(QuestionReaderUiState(questions = dummyExamQuestions()))
    val state: StateFlow<QuestionReaderUiState> = _state.asStateFlow()

    fun toggleAnswer(questionId: String) {
        _state.update { s ->
            val updated = if (questionId in s.revealedAnswers)
                s.revealedAnswers - questionId
            else
                s.revealedAnswers + questionId
            s.copy(revealedAnswers = updated, showAllAnswers = false)
        }
    }

    fun toggleAllAnswers() {
        _state.update { s ->
            if (s.showAllAnswers) {
                s.copy(showAllAnswers = false, revealedAnswers = emptySet())
            } else {
                val all = s.questions.map { it.id }.toSet()
                s.copy(showAllAnswers = true, revealedAnswers = all)
            }
        }
    }

    fun toggleFavorite(questionId: String) {
        _state.update { s ->
            // TODO: persist with ToggleFavoriteUseCase(questionId) when API is available
            //       API endpoint needed: POST /mcq/{questionId}/favorite (toggle)
            val updated = if (questionId in s.favoriteIds) s.favoriteIds - questionId
                          else s.favoriteIds + questionId
            s.copy(favoriteIds = updated)
        }
    }

    fun submitReport(questionId: String, reason: String, detail: String) {
        // TODO: call SubmitMCQReportUseCase(questionId, reason, detail) when API is available
        //       API endpoint needed: POST /mcq/{questionId}/report { reason, detail }
    }
}
