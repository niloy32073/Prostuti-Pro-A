package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.BookmarkRepository
import com.prostuti.student.data.service.local.BookmarkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** In-memory bookmarks of every type. Set [addResult]/[removeResult]/[loadFailure] to make a call fail. */
class FakeBookmarkRepository(initial: List<BookmarkEntity> = emptyList()) : BookmarkRepository {
    val bookmarks = MutableStateFlow(initial)

    var addResult: ApiResponse<Unit> = ApiResponse.Success(Unit)
    var removeResult: ApiResponse<Unit> = ApiResponse.Success(Unit)
    var syncResult: ApiResponse<Unit> = ApiResponse.Success(Unit)
    /** Emitted before the cached list by observeBookmarks. */
    var loadFailure: ApiResponse.Failure? = null

    val added = mutableListOf<Pair<BookmarkResourceType, String>>()
    val removed = mutableListOf<Pair<BookmarkResourceType, String>>()
    var syncCount = 0
    var clearCount = 0
    var lastRefresh: Boolean? = null

    override suspend fun syncIfNeeded(): ApiResponse<Unit> {
        syncCount++
        return syncResult
    }

    override fun observeBookmarkedIds(type: BookmarkResourceType): Flow<Set<String>> =
        bookmarks.map { list -> list.filter { it.resourceType == type }.map { it.resourceId }.toSet() }

    override fun observeBookmarks(type: BookmarkResourceType, refresh: Boolean): Flow<ApiResponse<List<BookmarkEntity>>> {
        lastRefresh = refresh
        val failure = loadFailure
        return bookmarks.map<List<BookmarkEntity>, ApiResponse<List<BookmarkEntity>>> { list ->
            ApiResponse.Success(list.filter { it.resourceType == type })
        }.onStart { if (failure != null) emit(failure) }
    }

    override suspend fun addBookmark(
        type: BookmarkResourceType,
        resourceId: String,
        pageNo: Int?,
        comments: String?
    ): ApiResponse<Unit> {
        added += type to resourceId
        if (addResult is ApiResponse.Success && bookmarks.value.none { it.resourceType == type && it.resourceId == resourceId }) {
            bookmarks.value = listOf(bookmark(resourceId, type = type)) + bookmarks.value
        }
        return addResult
    }

    override suspend fun removeBookmark(type: BookmarkResourceType, resourceId: String): ApiResponse<Unit> {
        removed += type to resourceId
        if (removeResult is ApiResponse.Success) {
            bookmarks.value = bookmarks.value.filterNot { it.resourceType == type && it.resourceId == resourceId }
        }
        return removeResult
    }

    override suspend fun clearBookmarks() {
        clearCount++
        bookmarks.value = emptyList()
    }

    companion object {
        fun bookmark(
            resourceId: String,
            createdAt: String = "2026-10-01T10:00:00Z",
            type: BookmarkResourceType = BookmarkResourceType.MCQ
        ) = BookmarkEntity(resourceType = type, resourceId = resourceId, bookmarkId = "b-$resourceId", createdAt = createdAt)
    }
}
