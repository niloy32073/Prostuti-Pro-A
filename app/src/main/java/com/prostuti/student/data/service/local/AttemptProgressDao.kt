package com.prostuti.student.data.service.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface AttemptProgressDao {
    @Query("SELECT * FROM attempt_progress WHERE attempt_id = :attemptId")
    suspend fun getByAttemptId(attemptId: String): AttemptProgressEntity?

    @Upsert
    suspend fun upsert(progress: AttemptProgressEntity)

    @Query("DELETE FROM attempt_progress WHERE attempt_id = :attemptId")
    suspend fun deleteByAttemptId(attemptId: String)
}
