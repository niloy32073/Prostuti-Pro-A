package com.prostuti.student.domain

import com.prostuti.student.data.repository.BookmarkRepository

class ClearBookmarksUseCase(private val repository: BookmarkRepository) {
    suspend operator fun invoke() = repository.clearBookmarks()
}
