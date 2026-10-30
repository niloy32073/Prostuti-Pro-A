package com.prostuti.student.ui.screen.qb

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ChapterSourceFilter { ALL, BOARD, COLLEGE }

data class ChapterBrowseUiState(
    val chapters: List<ChapterItem> = emptyList(),
    val selectedChapterId: String = "",
    val sourceFilter: ChapterSourceFilter = ChapterSourceFilter.ALL,
    val questions: List<DummyQuestion> = emptyList(),
    val revealedAnswers: Set<String> = emptySet(),
    val favoriteIds: Set<String> = emptySet()
)

class ChapterBrowseViewModel(val subjectId: String) : ViewModel() {
    // TODO: Replace dummyChapters() with FetchChaptersUseCase(subjectId) when API is available
    //       API endpoint needed: GET /curriculum/subject/{subjectId}/chapters
    // TODO: Replace dummyQuestionsForChapter() with FetchChapterQuestionsUseCase(chapterId, source)
    //       API endpoint needed: GET /mcq/chapter/{chapterId}/questions?source=board|college|all

    private val chapters = dummyChapters()

    private val _state = MutableStateFlow(
        ChapterBrowseUiState(
            chapters = chapters,
            selectedChapterId = chapters.firstOrNull()?.id ?: "",
            questions = dummyQuestionsForChapter(chapters.firstOrNull()?.id ?: "")
        )
    )
    val state: StateFlow<ChapterBrowseUiState> = _state.asStateFlow()

    fun selectChapter(chapterId: String) {
        _state.update { s ->
            // TODO: when API is ready, launch a coroutine to fetch questions for this chapter
            s.copy(
                selectedChapterId = chapterId,
                questions = dummyQuestionsForChapter(chapterId),
                revealedAnswers = emptySet()
            )
        }
    }

    fun selectSourceFilter(filter: ChapterSourceFilter) {
        _state.update { s ->
            // TODO: when API is ready, re-fetch questions with the new source filter
            s.copy(sourceFilter = filter, revealedAnswers = emptySet())
        }
    }

    fun toggleAnswer(questionId: String) {
        _state.update { s ->
            val updated = if (questionId in s.revealedAnswers)
                s.revealedAnswers - questionId
            else
                s.revealedAnswers + questionId
            s.copy(revealedAnswers = updated)
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
