package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.service.local.BookmarkDao
import com.prostuti.student.data.service.local.BookmarkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory stand-in for the Room [BookmarkDao]. */
class FakeBookmarkDao(initial: List<BookmarkEntity> = emptyList()) : BookmarkDao {
    val rows = MutableStateFlow(initial.associateBy { it.resourceType to it.resourceId })

    override suspend fun insert(bookmark: BookmarkEntity) { rows.value = rows.value + ((bookmark.resourceType to bookmark.resourceId) to bookmark) }
    override suspend fun insertAll(bookmarks: List<BookmarkEntity>) { rows.value = rows.value + bookmarks.associateBy { it.resourceType to it.resourceId } }
    override fun observeByType(type: BookmarkResourceType): Flow<List<BookmarkEntity>> =
        rows.map { m -> m.values.filter { it.resourceType == type }.sortedByDescending { it.createdAt } }
    override fun observeIdsByType(type: BookmarkResourceType): Flow<List<String>> =
        rows.map { m -> m.keys.filter { it.first == type }.map { it.second } }
    override suspend fun get(type: BookmarkResourceType, resourceId: String): BookmarkEntity? = rows.value[type to resourceId]
    override suspend fun delete(type: BookmarkResourceType, resourceId: String) { rows.value = rows.value - (type to resourceId) }
    override suspend fun deleteAll() { rows.value = emptyMap() }
}
