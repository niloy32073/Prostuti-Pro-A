package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.BookmarkRepository
import com.prostuti.student.data.service.local.BookmarkEntity
import kotlinx.coroutines.flow.Flow

class ObserveBookmarksUseCase(private val repository: BookmarkRepository) {
    operator fun invoke(type: BookmarkResourceType, refresh: Boolean = false): Flow<ApiResponse<List<BookmarkEntity>>> =
        repository.observeBookmarks(type, refresh)
}
