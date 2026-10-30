package com.prostuti.student.data.service.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface CreatedGroupDao {
    @Query("SELECT * FROM created_group")
    suspend fun getAll(): List<CreatedGroupEntity>

    @Query("SELECT * FROM created_group WHERE id = :groupId LIMIT 1")
    suspend fun getById(groupId: String): CreatedGroupEntity?

    @Query("SELECT * FROM created_group WHERE join_code = :code LIMIT 1")
    suspend fun findByJoinCode(code: String): CreatedGroupEntity?

    @Upsert
    suspend fun upsert(group: CreatedGroupEntity)
}
