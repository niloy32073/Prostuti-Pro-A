package com.prostuti.student.data.repository

import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.model.request.BookmarkCreateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Bookmark
import com.prostuti.student.data.service.local.BookmarkDao
import com.prostuti.student.data.service.local.BookmarkEntity
import com.prostuti.student.data.service.local.toBookmarkEntity
import com.prostuti.student.data.service.remote.BookmarkApiService
import com.prostuti.student.dataStore.PrefKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The list endpoint allows at most 100 per page. */
private const val PAGE_LIMIT = 100

/** Safety stop for the page loop (10 000 bookmarks). */
private const val MAX_PAGES = 100

class BookmarkRepositoryImpl(
    private val apiService: BookmarkApiService,
    private val dao: BookmarkDao,
    private val storage: StorageRepository
) : BookmarkRepository {

    private val syncMutex = Mutex()

    /** In-memory copy of [PrefKeys.BOOKMARKS_SYNCED]; null until read once. */
    @Volatile
    private var synced: Boolean? = null

    override suspend fun syncIfNeeded(): ApiResponse<Unit> = sync(force = false)

    override fun observeBookmarkedIds(type: BookmarkResourceType): Flow<Set<String>> = flow {
        sync(force = false) // a failure keeps whatever is cached
        emitAll(dao.observeIdsByType(type).map { it.toSet() })
    }

    override fun observeBookmarks(
        type: BookmarkResourceType,
        refresh: Boolean
    ): Flow<ApiResponse<List<BookmarkEntity>>> = flow {
        val result = sync(force = refresh)
        if (result is ApiResponse.Failure) {
            emit(result)
            if (!isSynced()) return@flow // never synced: the empty cache would be a lie
        }
        emitAll(dao.observeByType(type).map { ApiResponse.Success(it) })
    }

    override suspend fun addBookmark(
        type: BookmarkResourceType,
        resourceId: String,
        pageNo: Int?,
        comments: String?
    ): ApiResponse<Unit> {
        if (dao.get(type, resourceId) != null) return ApiResponse.Success(Unit)
        val request = BookmarkCreateRequest(type, resourceId, pageNo, comments)
        return when (val r = apiService.createBookmark(request)) {
            is ApiResponse.Success -> {
                r.data.toBookmarkEntity()?.let { dao.insert(it) }
                ApiResponse.Success(Unit, r.message)
            }
            // 409: bookmarked on another device, and the cache doesn't know its id yet.
            is ApiResponse.Failure -> reconcile(r) { dao.get(type, resourceId) != null }
        }
    }

    override suspend fun removeBookmark(type: BookmarkResourceType, resourceId: String): ApiResponse<Unit> {
        var bookmark = dao.get(type, resourceId)
        if (bookmark == null && !isSynced()) {
            val r = sync(force = false)
            if (r is ApiResponse.Failure) return r
            bookmark = dao.get(type, resourceId)
        }
        if (bookmark == null) return ApiResponse.Success(Unit)

        return when (val r = apiService.deleteBookmark(bookmark.bookmarkId)) {
            is ApiResponse.Success -> {
                dao.delete(type, resourceId)
                r
            }
            // 404: already removed on another device.
            is ApiResponse.Failure -> reconcile(r) { dao.get(type, resourceId) == null }
        }
    }

    override suspend fun clearBookmarks() {
        syncMutex.withLock {
            dao.deleteAll()
            setSynced(false)
        }
    }

    /** After a failed write, the server list decides: if it already shows the wanted state, that is success. */
    private suspend fun reconcile(failure: ApiResponse.Failure, isDone: suspend () -> Boolean): ApiResponse<Unit> =
        if (sync(force = true) is ApiResponse.Success && isDone()) ApiResponse.Success(Unit) else failure

    private suspend fun isSynced(): Boolean =
        synced ?: (storage.getBoolean(PrefKeys.BOOKMARKS_SYNCED).first() == true).also { synced = it }

    private suspend fun setSynced(value: Boolean) {
        storage.putBoolean(PrefKeys.BOOKMARKS_SYNCED, value)
        synced = value
    }

    /** Downloads every page of bookmarks (all types) and replaces the cache. */
    private suspend fun sync(force: Boolean): ApiResponse<Unit> = syncMutex.withLock {
        if (!force && isSynced()) return ApiResponse.Success(Unit)

        val all = mutableListOf<Bookmark>()
        var page = 1
        while (true) {
            when (val r = apiService.listBookmarks(page = page, limit = PAGE_LIMIT)) {
                is ApiResponse.Failure -> return r
                is ApiResponse.Success -> {
                    all += r.data
                    val totalPages = r.metaData?.totalPages ?: page
                    if (r.data.isEmpty() || page >= totalPages || page >= MAX_PAGES) break
                    page++
                }
            }
        }
        dao.replaceAll(
            all.mapNotNull { it.toBookmarkEntity() }.distinctBy { it.resourceType to it.resourceId }
        )
        setSynced(true) // set even when the list is empty: "this user has no bookmarks"
        ApiResponse.Success(Unit)
    }
}
