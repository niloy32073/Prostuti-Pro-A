package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.BookmarkRepository

class RemoveBookmarkUseCase(private val repository: BookmarkRepository) {
    suspend operator fun invoke(type: BookmarkResourceType, resourceId: String): ApiResponse<Unit> =
        repository.removeBookmark(type, resourceId)
}
