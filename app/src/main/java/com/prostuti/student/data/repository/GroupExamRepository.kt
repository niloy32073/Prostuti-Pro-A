package com.prostuti.student.data.repository

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.CreateGroupRoutineRequest
import com.prostuti.student.data.model.response.Group
import com.prostuti.student.data.model.response.GroupDetail
import com.prostuti.student.data.model.response.GroupMember
import com.prostuti.student.data.model.response.GroupPastExam
import kotlinx.coroutines.flow.Flow

interface GroupExamRepository {
    fun myGroups(): Flow<ApiResponse<List<Group>>>
    suspend fun groupDetail(groupId: String): ApiResponse<GroupDetail>

    /** Fails with errorCode "FORBIDDEN" when the caller isn't this group's admin, or
     * "ROUTINE_LOCKED" when one already exists — a created routine can never be edited. */
    suspend fun createRoutine(groupId: String, request: CreateGroupRoutineRequest): ApiResponse<Unit>

    suspend fun createGroup(nameBn: String): ApiResponse<Group>

    /** Fails with errorCode "INVALID_CODE" or "ALREADY_MEMBER". */
    suspend fun joinGroup(code: String): ApiResponse<Group>

    suspend fun pastExams(groupId: String): ApiResponse<List<GroupPastExam>>
    suspend fun members(groupId: String): ApiResponse<List<GroupMember>>
}
