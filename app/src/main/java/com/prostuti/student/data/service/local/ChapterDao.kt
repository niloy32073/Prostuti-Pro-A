package com.prostuti.student.data.service.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.prostuti.student.data.model.response.Chapter
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    // @Upsert so refreshing a chapter doesn't cascade-delete its cached topics.
    @Upsert
    suspend fun upsertAll(chapters: List<Chapter>)

    @Query("SELECT * FROM chapter WHERE subject_id = :subjectId ORDER BY `order`")
    suspend fun getBySubjectId(subjectId: String): List<Chapter>

    @Query("SELECT * FROM chapter WHERE subject_id = :subjectId ORDER BY `order`")
    fun observeBySubjectId(subjectId: String): Flow<List<Chapter>>

    @Query("SELECT * FROM chapter WHERE id = :id")
    suspend fun getById(id: String): Chapter?

    @Query("DELETE FROM chapter WHERE subject_id = :subjectId AND id NOT IN (:keepIds)")
    suspend fun deleteBySubjectIdExcept(subjectId: String, keepIds: List<String>)

    @Transaction
    suspend fun replaceBySubjectId(subjectId: String, chapters: List<Chapter>) {
        deleteBySubjectIdExcept(subjectId, chapters.map { it.id })
        upsertAll(chapters)
    }
}
