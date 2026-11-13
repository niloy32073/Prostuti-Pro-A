package com.prostuti.student.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.request.ExamHistoryFilter
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamHistoryItem
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.domain.FetchSubjectsUseCase
import com.prostuti.student.domain.GetExamHistoryUseCase
import com.prostuti.student.domain.HistoryTimeRange
import com.prostuti.student.domain.historyDateBounds
import com.prostuti.student.util.TimeProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ExamHistoryUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<ExamHistoryItem> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = false,
    val totalItems: Int? = null,
    /** A `HistoryStatus` value, or null for both. */
    val status: String? = null,
    val subjectId: String? = null,
    val timeRange: HistoryTimeRange = HistoryTimeRange.ALL,
    val customStart: LocalDate? = null,
    val customEnd: LocalDate? = null,
    val subjects: List<Subject> = emptyList(),
    val errors: Map<String, String> = emptyMap()
) {
    val hasFilter: Boolean get() = status != null || subjectId != null || timeRange != HistoryTimeRange.ALL
}

/** Exam history: paginated, filtered by status / subject / time. Any filter change reloads page 1. */
class ExamHistoryViewModel(
    private val getExamHistoryUseCase: GetExamHistoryUseCase,
    private val fetchSubjectsUseCase: FetchSubjectsUseCase,
    private val timeProvider: TimeProvider
) : ViewModel() {
    private val _state = MutableStateFlow(ExamHistoryUiState())
    val state: StateFlow<ExamHistoryUiState> = _state.asStateFlow()

    private var pageJob: Job? = null

    init {
        loadFirstPage()
        loadSubjects()
    }

    fun refresh() = loadFirstPage(isRefresh = true)

    fun setStatus(status: String?) {
        if (status == _state.value.status) return
        _state.update { it.copy(status = status) }
        loadFirstPage()
    }

    fun setSubject(subjectId: String?) {
        if (subjectId == _state.value.subjectId) return
        _state.update { it.copy(subjectId = subjectId) }
        loadFirstPage()
    }

    /** A preset range. For [HistoryTimeRange.CUSTOM] use [setCustomRange]. */
    fun setTimeRange(range: HistoryTimeRange) {
        if (range == HistoryTimeRange.CUSTOM || range == _state.value.timeRange) return
        _state.update { it.copy(timeRange = range, customStart = null, customEnd = null) }
        loadFirstPage()
    }

    fun setCustomRange(start: LocalDate, end: LocalDate) {
        _state.update { it.copy(timeRange = HistoryTimeRange.CUSTOM, customStart = start, customEnd = end) }
        loadFirstPage()
    }

    fun resetFilters() {
        if (!_state.value.hasFilter) return
        _state.update {
            it.copy(status = null, subjectId = null, timeRange = HistoryTimeRange.ALL, customStart = null, customEnd = null)
        }
        loadFirstPage()
    }

    /** Called when the list nears its end. Does nothing while a page is loading or at the last page. */
    fun loadMore() {
        val s = _state.value
        if (!s.hasMore || s.isLoading || s.isLoadingMore || s.isRefreshing) return
        _state.update { it.copy(isLoadingMore = true) }
        loadPage(s.page + 1)
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }

    private fun loadFirstPage(isRefresh: Boolean = false) {
        _state.update {
            it.copy(
                isLoading = !isRefresh,
                isRefreshing = isRefresh,
                isLoadingMore = false,
                items = if (isRefresh) it.items else emptyList(),
                page = if (isRefresh) it.page else 0,
                hasMore = if (isRefresh) it.hasMore else false
            )
        }
        loadPage(1)
    }

    private fun loadPage(page: Int) {
        pageJob?.cancel()  // a newer filter wins over a page still in flight
        pageJob = viewModelScope.launch {
            val s = _state.value
            val (from, to) = historyDateBounds(s.timeRange, timeProvider.nowEpochMillis(), s.customStart, s.customEnd)
            val filter = ExamHistoryFilter(subjectId = s.subjectId, dateFrom = from, dateTo = to, status = s.status)
            when (val r = getExamHistoryUseCase(filter, page)) {
                is ApiResponse.Success -> _state.update {
                    val totalPages = r.metaData?.totalPages
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        items = if (page == 1) r.data else (it.items + r.data).distinctBy { item -> item.attemptId },
                        page = page,
                        hasMore = if (totalPages != null) page < totalPages else r.data.size >= GetExamHistoryUseCase.PAGE_SIZE,
                        totalItems = r.metaData?.totalItems
                    )
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errors = it.errors + (ERR_LOAD to r.message)
                    )
                }
            }
        }
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            fetchSubjectsUseCase().collect { r ->
                if (r is ApiResponse.Success) _state.update { it.copy(subjects = r.data) }
            }
        }
    }

    private companion object {
        const val ERR_LOAD = "LOAD"
    }
}
