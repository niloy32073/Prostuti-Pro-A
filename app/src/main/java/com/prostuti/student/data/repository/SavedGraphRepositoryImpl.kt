package com.prostuti.student.data.repository

import com.prostuti.student.data.service.local.SavedGraphDao
import com.prostuti.student.data.service.local.SavedGraphEntity
import com.prostuti.student.util.TimeProvider
import kotlinx.coroutines.flow.Flow

class SavedGraphRepositoryImpl(
    private val dao: SavedGraphDao,
    private val time: TimeProvider,
) : SavedGraphRepository {

    override fun observeAll(): Flow<List<SavedGraphEntity>> = dao.observeAll()

    override suspend fun get(id: Long): SavedGraphEntity? = dao.getById(id)

    override suspend fun save(id: Long?, name: String, content: String, preview: String, isSpace: Boolean): Long {
        val row = SavedGraphEntity(id ?: 0, name, content, preview, isSpace, time.nowEpochMillis())
        val result = dao.upsert(row)
        // @Upsert returns -1 when it updated an existing row.
        return if (id != null && result == -1L) id else result
    }

    override suspend fun rename(id: Long, name: String) = dao.rename(id, name)

    override suspend fun delete(id: Long) = dao.delete(id)
}
