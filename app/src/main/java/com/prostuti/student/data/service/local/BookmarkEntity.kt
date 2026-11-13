package com.prostuti.student.data.service.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.model.response.Bookmark

/**
 * The signed-in user's bookmarks of every type, mirrored from `GET learning/bookmarks`.
 * The server allows one live bookmark per (type, resource), so that pair is the key;
 * [bookmarkId] is what `DELETE` needs.
 */
@Entity(
    tableName = "bookmark",
    primaryKeys = ["resource_type", "resource_id"],
    indices = [Index(value = ["bookmark_id"], unique = true)]
)
data class BookmarkEntity(
    @ColumnInfo(name = "resource_type") val resourceType: BookmarkResourceType,
    @ColumnInfo(name = "resource_id") val resourceId: String,
    @ColumnInfo(name = "bookmark_id") val bookmarkId: String,
    @ColumnInfo(name = "page_no") val pageNo: Int? = null,
    @ColumnInfo(name = "comments") val comments: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String
)

/** Null for a type this app version doesn't know. */
fun Bookmark.toBookmarkEntity(): BookmarkEntity? {
    val type = BookmarkResourceType.entries.firstOrNull { it.name == resourceType } ?: return null
    return BookmarkEntity(
        resourceType = type,
        resourceId = resourceId,
        bookmarkId = id,
        pageNo = pageNo,
        comments = comments,
        createdAt = createdAt
    )
}
