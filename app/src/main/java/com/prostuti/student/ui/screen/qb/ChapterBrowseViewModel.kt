package com.prostuti.student.ui.screen.qb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.domain.AddBookmarkUseCase
import com.prostuti.student.domain.ObserveBookmarkedIdsUseCase
import com.prostuti.student.domain.RemoveBookmarkUseCase
import com.prostuti.student.ui.screen.bookmark.BookmarkToggler
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
    val favoriteIds: Set<String> = emptySet(),
    val errors: Map<String, String> = emptyMap()
)

class ChapterBrowseViewModel(
    val subjectId: String,
    observeBookmarkedIdsUseCase: ObserveBookmarkedIdsUseCase,
    addBookmarkUseCase: AddBookmarkUseCase,
    removeBookmarkUseCase: RemoveBookmarkUseCase
) : ViewModel() {
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

    private val bookmarks = BookmarkToggler(
        scope = viewModelScope,
        type = BookmarkResourceType.MCQ,
        observeBookmarkedIds = observeBookmarkedIdsUseCase,
        addBookmark = addBookmarkUseCase,
        removeBookmark = removeBookmarkUseCase,
        onChange = { ids -> _state.update { it.copy(favoriteIds = ids) } },
        onError = { message -> _state.update { it.copy(errors = it.errors + (ERR_BOOKMARK to message)) } }
    )

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

    fun toggleFavorite(questionId: String) = bookmarks.toggle(questionId)

    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }

    fun submitReport(questionId: String, reason: String, detail: String) {
        // TODO: call SubmitMCQReportUseCase(questionId, reason, detail) when API is available
        //       API endpoint needed: POST /mcq/{questionId}/report { reason, detail }
    }

    private companion object {
        const val ERR_BOOKMARK = "BOOKMARK"
    }
}
