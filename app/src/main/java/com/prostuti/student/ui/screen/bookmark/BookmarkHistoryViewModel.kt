package com.prostuti.student.ui.screen.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.service.local.BookmarkEntity
import com.prostuti.student.domain.FetchMcqUseCase
import com.prostuti.student.domain.ObserveBookmarksUseCase
import com.prostuti.student.domain.RemoveBookmarkUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** How many questions download at the same time. */
private const val MCQ_FETCH_PARALLELISM = 4

/**
 * The user's bookmarked MCQs. The bookmark list comes from [ObserveBookmarksUseCase]; each
 * card's question is then downloaded on its own (`GET questions/mcq/{id}`), since the bookmark
 * list only carries ids.
 */
class BookmarkHistoryViewModel(
    private val observeBookmarksUseCase: ObserveBookmarksUseCase,
    private val fetchMcqUseCase: FetchMcqUseCase,
    private val removeBookmarkUseCase: RemoveBookmarkUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BookmarkHistoryUiState())
    val state: StateFlow<BookmarkHistoryUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private val fetchPermits = Semaphore(MCQ_FETCH_PARALLELISM)
    private val fetchingMcqIds = mutableSetOf<String>()

    /** Latest list from the cache, before hiding the ones being removed. */
    private var allBookmarks: List<BookmarkEntity> = emptyList()
    private val removingMcqIds = mutableSetOf<String>()

    init {
        load(refresh = false)
    }

    fun refresh() {
        _state.update { it.copy(failedMcqIds = emptySet()) }
        load(refresh = true)
    }

    private fun load(refresh: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            observeBookmarksUseCase(BookmarkResourceType.MCQ, refresh).collect { r ->
                when (r) {
                    is ApiResponse.Success -> {
                        allBookmarks = r.data
                        _state.update { it.copy(isLoading = false, hasLoaded = true) }
                        publishBookmarks()
                        r.data.forEach { fetchMcqIfNeeded(it.resourceId) }
                    }
                    is ApiResponse.Failure -> _state.update {
                        it.copy(isLoading = false, errors = it.errors + (ERR_LOAD to r.message))
                    }
                }
            }
            // The flow only ends early when nothing could be loaded at all.
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun fetchMcqIfNeeded(mcqId: String) {
        val s = _state.value
        if (mcqId in s.mcqs || mcqId in s.failedMcqIds || !fetchingMcqIds.add(mcqId)) return
        viewModelScope.launch {
            val r = fetchPermits.withPermit { fetchMcqUseCase(mcqId) }
            fetchingMcqIds.remove(mcqId)
            when (r) {
                is ApiResponse.Success -> _state.update { it.copy(mcqs = it.mcqs + (mcqId to r.data)) }
                is ApiResponse.Failure -> _state.update { it.copy(failedMcqIds = it.failedMcqIds + mcqId) }
            }
        }
    }

    fun retryMcq(mcqId: String) {
        _state.update { it.copy(failedMcqIds = it.failedMcqIds - mcqId) }
        fetchMcqIfNeeded(mcqId)
    }

    fun toggleAnswer(mcqId: String) = _state.update { s ->
        val revealed = if (mcqId in s.revealedMcqIds) s.revealedMcqIds - mcqId else s.revealedMcqIds + mcqId
        s.copy(revealedMcqIds = revealed)
    }

    /** Hides the card at once; it comes back if the server call fails. */
    fun removeBookmark(mcqId: String) {
        if (!removingMcqIds.add(mcqId)) return
        publishBookmarks()
        viewModelScope.launch {
            val r = removeBookmarkUseCase(BookmarkResourceType.MCQ, mcqId)
            if (r is ApiResponse.Success) {
                // Drop it here too, so it can't flash back before Room's next emit.
                allBookmarks = allBookmarks.filterNot { it.resourceId == mcqId }
            }
            removingMcqIds.remove(mcqId)
            publishBookmarks()
            if (r is ApiResponse.Failure) _state.update { it.copy(errors = it.errors + (ERR_REMOVE to r.message)) }
        }
    }

    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }

    private fun publishBookmarks() = _state.update { s ->
        s.copy(bookmarks = allBookmarks.filterNot { it.resourceId in removingMcqIds })
    }

    private companion object {
        const val ERR_LOAD = "LOAD"
        const val ERR_REMOVE = "REMOVE"
    }
}
