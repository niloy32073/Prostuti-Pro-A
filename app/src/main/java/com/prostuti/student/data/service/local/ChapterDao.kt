package com.prostuti.student.data.service.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prostuti.student.data.model.response.Chapter
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<Chapter>)

    @Query("SELECT * FROM chapter WHERE subject_id = :subjectId ORDER BY `order`")
    suspend fun getBySubjectId(subjectId: String): List<Chapter>

    @Query("SELECT * FROM chapter WHERE subject_id = :subjectId ORDER BY `order`")
    fun observeBySubjectId(subjectId: String): Flow<List<Chapter>>

    @Query("DELETE FROM chapter WHERE subject_id = :subjectId")
    suspend fun deleteBySubjectId(subjectId: String)
}
