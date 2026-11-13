package com.prostuti.student.data.service.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.prostuti.student.data.model.response.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    // @Upsert updates in place; @Insert(REPLACE) deletes the row first, which would cascade-delete
    // every cached chapter/topic under it.
    @Upsert
    suspend fun upsertAll(subjects: List<Subject>)

    @Query("SELECT * FROM subject")
    suspend fun getAll(): List<Subject>

    @Query("SELECT * FROM subject")
    fun observeAll(): Flow<List<Subject>>

    @Query("SELECT * FROM subject WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<Subject>

    @Query("SELECT * FROM subject WHERE id = :id")
    suspend fun getById(id: String): Subject?

    @Query("DELETE FROM subject WHERE id NOT IN (:keepIds)")
    suspend fun deleteAllExcept(keepIds: List<String>)

    @Query("DELETE FROM subject")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(subjects: List<Subject>) {
        deleteAllExcept(subjects.map { it.id })
        upsertAll(subjects)
    }
}
