package com.prostuti.student.data.repository

import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.service.local.BookmarkEntity
import kotlinx.coroutines.flow.Flow

/**
 * The signed-in user's bookmarks of every type. Room is the source of truth: the server list is
 * downloaded once per user (see [syncIfNeeded]) and after that every add/remove writes to the
 * server first, then to Room, so screens never need to ask the server again.
 */
interface BookmarkRepository {
    /**
     * Downloads the bookmark list only if this device has never synced for this user. After a
     * successful download — even an empty one — a saved flag stops further downloads until
     * logout or an explicit refresh.
     */
    suspend fun syncIfNeeded(): ApiResponse<Unit>

    /** Ids of bookmarked resources of [type], from Room (syncs first if never synced). */
    fun observeBookmarkedIds(type: BookmarkResourceType): Flow<Set<String>>

    /** Bookmarks of [type], newest first. [refresh] downloads the list again (pull-to-refresh). */
    fun observeBookmarks(type: BookmarkResourceType, refresh: Boolean): Flow<ApiResponse<List<BookmarkEntity>>>

    /** Server first, then Room. Already bookmarked (here or on another device) counts as success. */
    suspend fun addBookmark(
        type: BookmarkResourceType,
        resourceId: String,
        pageNo: Int? = null,
        comments: String? = null
    ): ApiResponse<Unit>

    /** Server first, then Room. Not bookmarked (any more) counts as success. */
    suspend fun removeBookmark(type: BookmarkResourceType, resourceId: String): ApiResponse<Unit>

    /** On logout: the next user must not see these bookmarks; their list is downloaded fresh. */
    suspend fun clearBookmarks()
}
