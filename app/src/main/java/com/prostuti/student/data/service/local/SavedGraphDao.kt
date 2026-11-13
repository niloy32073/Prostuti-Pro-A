package com.prostuti.student.data.service.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedGraphDao {
    @Query("SELECT * FROM saved_graph ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<SavedGraphEntity>>

    @Query("SELECT * FROM saved_graph WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SavedGraphEntity?

    /** Inserts (id = 0) or replaces; returns the row id. */
    @Upsert
    suspend fun upsert(graph: SavedGraphEntity): Long

    @Query("UPDATE saved_graph SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM saved_graph WHERE id = :id")
    suspend fun delete(id: Long)
}
