package com.prostuti.student.ui.screen.qb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.model.enums.ReportResourceType
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.domain.AddBookmarkUseCase
import com.prostuti.student.domain.GetExamQuestionsWithOptionsUseCase
import com.prostuti.student.domain.ObserveReportedIdsUseCase
import com.prostuti.student.domain.SubmitMcqReportUseCase
import com.prostuti.student.domain.ObserveBookmarkedIdsUseCase
import com.prostuti.student.domain.RemoveBookmarkUseCase
import com.prostuti.student.ui.screen.bookmark.BookmarkToggler
import com.prostuti.student.ui.screen.examengine.component.McqGridItem
import com.prostuti.student.ui.screen.examengine.component.groupQuestionsByPassage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuestionReaderUiState(
    val isLoading: Boolean = false,
    /** Questions in screen order, passage questions grouped and numbered as shown. */
    val items: List<McqGridItem> = emptyList(),
    val questionCount: Int = 0,
    val revealedAnswers: Set<String> = emptySet(),
    val showAllAnswers: Boolean = false,
    val favoriteIds: Set<String> = emptySet(),
    /** Reported MCQs: their flag shows red and can't be tapped again. */
    val reportedIds: Set<String> = emptySet(),
    /** One-shot "report sent" notice; consume with [QuestionReaderViewModel.onReportSentConsumed]. */
    val isReportSent: Boolean = false,
    val errors: Map<String, String> = emptyMap()
)

/** Read an exam's questions without starting an attempt ("প্রশ্ন দেখো"). */
class QuestionReaderViewModel(
    private val examId: String,
    private val getExamQuestionsWithOptions: GetExamQuestionsWithOptionsUseCase,
    observeBookmarkedIdsUseCase: ObserveBookmarkedIdsUseCase,
    addBookmarkUseCase: AddBookmarkUseCase,
    removeBookmarkUseCase: RemoveBookmarkUseCase,
    private val submitMcqReportUseCase: SubmitMcqReportUseCase,
    observeReportedIdsUseCase: ObserveReportedIdsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(QuestionReaderUiState())
    val state: StateFlow<QuestionReaderUiState> = _state.asStateFlow()

    private val bookmarks = BookmarkToggler(
        scope = viewModelScope,
        type = BookmarkResourceType.MCQ,
        observeBookmarkedIds = observeBookmarkedIdsUseCase,
        addBookmark = addBookmarkUseCase,
        removeBookmark = removeBookmarkUseCase,
        onChange = { ids -> _state.update { it.copy(favoriteIds = ids) } },
        onError = { message -> _state.update { it.copy(errors = it.errors + (ERR_BOOKMARK to message)) } }
    )

    private var loadJob: Job? = null
    private var questionIds: Set<String> = emptySet()

    init {
        load()
        viewModelScope.launch {
            observeReportedIdsUseCase(ReportResourceType.MCQ).collect { ids ->
                _state.update { it.copy(reportedIds = ids) }
            }
        }
    }

    fun refresh() = load()

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errors = it.errors - ERR_LOAD) }
            when (val result = getExamQuestionsWithOptions(examId)) {
                is ApiResponse.Success -> {
                    val sorted = result.data.sortedBy { it.order }
                    // Each passage comes nested in its questions; keep the first copy that has content.
                    val passages: Map<String, FetchPassageData> = sorted
                        .mapNotNull { q -> q.passage?.let { (q.passageId ?: it.id) to it } }
                        .groupBy({ it.first }, { it.second })
                        .mapValues { (_, copies) ->
                            copies.firstOrNull { !it.passageText.isNullOrBlank() || !it.passageImage.isNullOrBlank() }
                                ?: copies.first()
                        }
                    val questions = sorted.map { it.toQuestion() }
                    questionIds = questions.map { it.mcqId }.toSet()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = groupQuestionsByPassage(questions, passages),
                            questionCount = questions.size,
                            revealedAnswers = if (it.showAllAnswers) questionIds else it.revealedAnswers intersect questionIds
                        )
                    }
                }

                is ApiResponse.Failure -> _state.update {
                    it.copy(isLoading = false, errors = it.errors + (ERR_LOAD to result.message))
                }
            }
        }
    }

    fun toggleAnswer(questionId: String) = _state.update { s ->
        val revealed = if (questionId in s.revealedAnswers) s.revealedAnswers - questionId else s.revealedAnswers + questionId
        s.copy(revealedAnswers = revealed, showAllAnswers = revealed.isNotEmpty() && revealed == questionIds)
    }

    fun toggleAllAnswers() = _state.update { s ->
        if (s.showAllAnswers) s.copy(showAllAnswers = false, revealedAnswers = emptySet())
        else s.copy(showAllAnswers = true, revealedAnswers = questionIds)
    }

    fun toggleFavorite(questionId: String) = bookmarks.toggle(questionId)

    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }

    fun submitReport(questionId: String, reason: String, detail: String) {
        if (questionId in _state.value.reportedIds) return // reports are final
        viewModelScope.launch {
            when (val r = submitMcqReportUseCase(questionId, reason, detail.ifBlank { null })) {
                is ApiResponse.Success -> _state.update { it.copy(isReportSent = true) }
                is ApiResponse.Failure -> _state.update { it.copy(errors = it.errors + (ERR_REPORT to r.message)) }
            }
        }
    }

    fun onReportSentConsumed() = _state.update { it.copy(isReportSent = false) }

    private companion object {
        const val ERR_LOAD = "LOAD"
        const val ERR_BOOKMARK = "BOOKMARK"
        const val ERR_REPORT = "REPORT"
    }
}
