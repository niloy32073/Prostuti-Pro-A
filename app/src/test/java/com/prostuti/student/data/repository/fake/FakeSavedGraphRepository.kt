package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.repository.SavedGraphRepository
import com.prostuti.student.data.service.local.SavedGraphEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory "My graphs" for unit tests. */
class FakeSavedGraphRepository : SavedGraphRepository {
    private val rows = MutableStateFlow<Map<Long, SavedGraphEntity>>(emptyMap())
    private var nextId = 1L
    var clock = 1_000L

    val all: List<SavedGraphEntity> get() = rows.value.values.sortedByDescending { it.updatedAt }

    override fun observeAll(): Flow<List<SavedGraphEntity>> = rows.map { m -> m.values.sortedByDescending { it.updatedAt } }

    override suspend fun get(id: Long): SavedGraphEntity? = rows.value[id]

    override suspend fun save(id: Long?, name: String, content: String, preview: String, isSpace: Boolean): Long {
        val key = id ?: nextId++
        rows.value = rows.value + (key to SavedGraphEntity(key, name, content, preview, isSpace, clock++))
        return key
    }

    override suspend fun rename(id: Long, name: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(name = name)) }
    }

    override suspend fun delete(id: Long) {
        rows.value = rows.value - id
    }
}
