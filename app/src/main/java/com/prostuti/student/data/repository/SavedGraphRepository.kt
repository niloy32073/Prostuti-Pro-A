package com.prostuti.student.data.repository

import com.prostuti.student.data.service.local.SavedGraphEntity
import kotlinx.coroutines.flow.Flow

/** Graphs saved on this device (nothing is sent to the server). */
interface SavedGraphRepository {
    fun observeAll(): Flow<List<SavedGraphEntity>>
    suspend fun get(id: Long): SavedGraphEntity?

    /** Creates (id = null) or overwrites a saved graph; returns its id. */
    suspend fun save(id: Long?, name: String, content: String, preview: String, isSpace: Boolean): Long
    suspend fun rename(id: Long, name: String)
    suspend fun delete(id: Long)
}
