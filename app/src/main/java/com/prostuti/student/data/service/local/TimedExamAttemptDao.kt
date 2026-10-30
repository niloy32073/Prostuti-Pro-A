package com.prostuti.student.data.service.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface TimedExamAttemptDao {
    @Query("SELECT * FROM timed_exam_attempt WHERE exam_id = :examId")
    suspend fun getByExamId(examId: String): TimedExamAttemptEntity?

    @Upsert
    suspend fun upsert(attempt: TimedExamAttemptEntity)

    @Query("DELETE FROM timed_exam_attempt WHERE exam_id = :examId")
    suspend fun deleteByExamId(examId: String)
}
