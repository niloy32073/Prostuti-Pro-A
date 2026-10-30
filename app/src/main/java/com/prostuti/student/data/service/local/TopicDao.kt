package com.prostuti.student.data.service.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prostuti.student.data.model.response.Topic
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(topics: List<Topic>)

    @Query("SELECT * FROM topic WHERE chapter_id = :chapterId ORDER BY `order`")
    suspend fun getByChapterId(chapterId: String): List<Topic>

    @Query("SELECT * FROM topic WHERE chapter_id = :chapterId ORDER BY `order`")
    fun observeByChapterId(chapterId: String): Flow<List<Topic>>

    @Query("DELETE FROM topic WHERE chapter_id = :chapterId")
    suspend fun deleteByChapterId(chapterId: String)
}
