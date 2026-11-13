package com.prostuti.student.data.service.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.prostuti.student.data.model.response.Topic
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Upsert
    suspend fun upsertAll(topics: List<Topic>)

    @Query("SELECT * FROM topic WHERE chapter_id = :chapterId ORDER BY `order`")
    suspend fun getByChapterId(chapterId: String): List<Topic>

    @Query("SELECT * FROM topic WHERE chapter_id = :chapterId ORDER BY `order`")
    fun observeByChapterId(chapterId: String): Flow<List<Topic>>

    @Query("SELECT * FROM topic WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<Topic>

    @Query("SELECT * FROM topic WHERE id = :id")
    suspend fun getById(id: String): Topic?

    @Query("DELETE FROM topic WHERE chapter_id = :chapterId AND id NOT IN (:keepIds)")
    suspend fun deleteByChapterIdExcept(chapterId: String, keepIds: List<String>)

    @Transaction
    suspend fun replaceByChapterId(chapterId: String, topics: List<Topic>) {
        deleteByChapterIdExcept(chapterId, topics.map { it.id })
        upsertAll(topics)
    }
}
