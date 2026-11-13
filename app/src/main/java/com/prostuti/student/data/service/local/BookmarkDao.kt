package com.prostuti.student.data.service.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.prostuti.student.data.model.enums.BookmarkResourceType
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookmarks: List<BookmarkEntity>)

    /** Newest first, like the server's list. ISO-8601 strings sort by time. */
    @Query("SELECT * FROM bookmark WHERE resource_type = :type ORDER BY created_at DESC")
    fun observeByType(type: BookmarkResourceType): Flow<List<BookmarkEntity>>

    @Query("SELECT resource_id FROM bookmark WHERE resource_type = :type")
    fun observeIdsByType(type: BookmarkResourceType): Flow<List<String>>

    @Query("SELECT * FROM bookmark WHERE resource_type = :type AND resource_id = :resourceId")
    suspend fun get(type: BookmarkResourceType, resourceId: String): BookmarkEntity?

    @Query("DELETE FROM bookmark WHERE resource_type = :type AND resource_id = :resourceId")
    suspend fun delete(type: BookmarkResourceType, resourceId: String)

    @Query("DELETE FROM bookmark")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(bookmarks: List<BookmarkEntity>) {
        deleteAll()
        insertAll(bookmarks)
    }
}
