package com.prostuti.student.ui.screen.bookmark

import com.prostuti.student.data.model.response.McqDetail
import com.prostuti.student.data.service.local.BookmarkEntity

data class BookmarkHistoryUiState(
    val isLoading: Boolean = false,
    /** True once the bookmark list has arrived at least once (cached or from the server). */
    val hasLoaded: Boolean = false,
    /** Newest first; MCQs being removed are already left out. */
    val bookmarks: List<BookmarkEntity> = emptyList(),
    /** mcqId -> question, filled in as each one downloads. */
    val mcqs: Map<String, McqDetail> = emptyMap(),
    /** MCQs whose question could not be downloaded (the card offers a retry). */
    val failedMcqIds: Set<String> = emptySet(),
    /** Cards showing the answer and explanation. */
    val revealedMcqIds: Set<String> = emptySet(),
    val errors: Map<String, String> = emptyMap()
)
