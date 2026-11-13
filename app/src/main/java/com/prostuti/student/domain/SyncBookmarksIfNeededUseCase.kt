package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.BookmarkRepository

/** App-open check: downloads the bookmark list only if this device has never synced for this user. */
class SyncBookmarksIfNeededUseCase(private val repository: BookmarkRepository) {
    suspend operator fun invoke(): ApiResponse<Unit> = repository.syncIfNeeded()
}
