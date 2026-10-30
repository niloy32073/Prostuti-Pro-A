package com.prostuti.student.data.service.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DemoExamWindowDao {
    @Query("SELECT * FROM demo_exam_window WHERE exam_id = :examId")
    suspend fun getByExamId(examId: String): DemoExamWindowEntity?

    @Upsert
    suspend fun upsert(window: DemoExamWindowEntity)

    @Query("DELETE FROM demo_exam_window WHERE exam_id = :examId")
    suspend fun deleteByExamId(examId: String)
}
