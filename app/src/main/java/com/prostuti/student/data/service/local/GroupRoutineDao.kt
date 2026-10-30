package com.prostuti.student.data.service.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface GroupRoutineDao {
    @Query("SELECT * FROM group_routine WHERE group_id = :groupId")
    suspend fun getByGroupId(groupId: String): GroupRoutineEntity?

    @Upsert
    suspend fun upsert(routine: GroupRoutineEntity)
}
