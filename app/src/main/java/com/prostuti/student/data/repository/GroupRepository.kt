package com.prostuti.student.data.repository

import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.JoinRequestStatus
import com.prostuti.student.data.model.request.GroupCreateRequest
import com.prostuti.student.data.model.request.GroupUpdateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupActionAck
import com.prostuti.student.data.model.response.GroupJoinRequest
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.model.response.StudyGroupMember

/** Study groups, their members and join requests. Online only: nothing is cached. */
interface GroupRepository {
    suspend fun createGroup(body: GroupCreateRequest): ApiResponse<StudyGroup>
    suspend fun myGroups(page: Int, limit: Int): ApiResponse<List<StudyGroup>>
    suspend fun exploreGroups(query: String?, page: Int, limit: Int): ApiResponse<List<StudyGroup>>
    suspend fun joinByCode(code: String): ApiResponse<StudyGroup>
    suspend fun group(groupId: String): ApiResponse<StudyGroup>
    suspend fun updateGroup(groupId: String, body: GroupUpdateRequest): ApiResponse<StudyGroup>
    suspend fun deleteGroup(groupId: String): ApiResponse<GroupActionAck>
    suspend fun regenerateCode(groupId: String): ApiResponse<StudyGroup>
    suspend fun members(groupId: String, page: Int, limit: Int): ApiResponse<List<StudyGroupMember>>
    suspend fun addMember(groupId: String, identifier: String): ApiResponse<StudyGroupMember>
    suspend fun setMemberRole(groupId: String, userId: String, role: GroupRole): ApiResponse<StudyGroupMember>
    suspend fun removeMember(groupId: String, userId: String): ApiResponse<GroupActionAck>
    suspend fun leaveGroup(groupId: String): ApiResponse<GroupActionAck>
    suspend fun requestToJoin(groupId: String): ApiResponse<GroupJoinRequest>
    suspend fun joinRequests(groupId: String, status: JoinRequestStatus, page: Int, limit: Int): ApiResponse<List<GroupJoinRequest>>
    suspend fun approveJoinRequest(groupId: String, requestId: String): ApiResponse<GroupJoinRequest>
    suspend fun rejectJoinRequest(groupId: String, requestId: String): ApiResponse<GroupJoinRequest>
}
