package com.prostuti.student.data.service.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prostuti.student.data.model.response.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subjects: List<Subject>)

    @Query("SELECT * FROM subject")
    suspend fun getAll(): List<Subject>

    @Query("SELECT * FROM subject")
    fun observeAll(): Flow<List<Subject>>

    @Query("DELETE FROM subject")
    suspend fun deleteAll()
}
