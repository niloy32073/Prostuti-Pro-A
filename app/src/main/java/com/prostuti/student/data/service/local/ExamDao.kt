package com.prostuti.student.data.service.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Backs the group/live exam fake repositories — the one piece of this feature's dummy data that
 * is Room-persisted (routines, submissions, created/joined groups, demo window anchors), so it
 * survives the app being closed and reopened instead of resetting like the static dummy content. */
@Dao
interface ExamDao {
    @Query("SELECT * FROM group_routine WHERE group_id = :groupId")
    suspend fun getRoutine(groupId: String): GroupRoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutine(routine: GroupRoutineEntity)

    @Query("SELECT * FROM timed_exam_attempt WHERE exam_id = :examId")
    suspend fun getAttempt(examId: String): TimedExamAttemptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttempt(attempt: TimedExamAttemptEntity)

    @Query("DELETE FROM timed_exam_attempt WHERE exam_id = :examId")
    suspend fun deleteAttempt(examId: String)

    @Query("SELECT * FROM demo_exam_window WHERE exam_id = :examId")
    suspend fun getDemoWindow(examId: String): DemoExamWindowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDemoWindow(window: DemoExamWindowEntity)

    @Query("DELETE FROM demo_exam_window WHERE exam_id = :examId")
    suspend fun deleteDemoWindow(examId: String)

    @Query("SELECT * FROM created_group")
    suspend fun getCreatedGroups(): List<CreatedGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCreatedGroup(group: CreatedGroupEntity)

    @Query("SELECT * FROM created_group WHERE join_code = :code LIMIT 1")
    suspend fun findCreatedGroupByJoinCode(code: String): CreatedGroupEntity?

    @Query("SELECT * FROM created_group WHERE id = :groupId LIMIT 1")
    suspend fun getCreatedGroup(groupId: String): CreatedGroupEntity?
}
