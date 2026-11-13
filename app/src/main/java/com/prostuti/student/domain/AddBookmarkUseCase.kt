package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.BookmarkRepository

class AddBookmarkUseCase(private val repository: BookmarkRepository) {
    suspend operator fun invoke(
        type: BookmarkResourceType,
        resourceId: String,
        pageNo: Int? = null,
        comments: String? = null
    ): ApiResponse<Unit> = repository.addBookmark(type, resourceId, pageNo, comments)
}
