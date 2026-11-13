package com.prostuti.student.data.service.remote

import com.prostuti.student.data.model.enums.JoinRequestStatus
import com.prostuti.student.data.model.request.DirectAddRequest
import com.prostuti.student.data.model.request.GroupCreateRequest
import com.prostuti.student.data.model.request.GroupUpdateRequest
import com.prostuti.student.data.model.request.JoinByCodeRequest
import com.prostuti.student.data.model.request.MemberRoleRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupActionAck
import com.prostuti.student.data.model.response.GroupJoinRequest
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.model.response.StudyGroupMember
import com.prostuti.student.util.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post

/** `/groups` endpoints (scope `group:use`). List calls carry paging in `ApiResponse.Success.metaData`. */
class GroupApiService(private val client: HttpClient) {

    suspend fun createGroup(body: GroupCreateRequest): ApiResponse<StudyGroup> = safeApiCall {
        client.post("groups") { json(body) }
    }

    suspend fun myGroups(page: Int, limit: Int): ApiResponse<List<StudyGroup>> = safeApiCall {
        client.get("groups/manage") { paging(page, limit) }
    }

    suspend fun exploreGroups(query: String?, page: Int, limit: Int): ApiResponse<List<StudyGroup>> = safeApiCall {
        client.get("groups/explore") {
            query?.takeIf { it.isNotBlank() }?.let { parameter("q", it) }
            paging(page, limit)
        }
    }

    suspend fun joinByCode(code: String): ApiResponse<StudyGroup> = safeApiCall {
        client.post("groups/join") { json(JoinByCodeRequest(code)) }
    }

    suspend fun group(groupId: String): ApiResponse<StudyGroup> = safeApiCall {
        client.get("groups/$groupId")
    }

    suspend fun updateGroup(groupId: String, body: GroupUpdateRequest): ApiResponse<StudyGroup> = safeApiCall {
        client.patch("groups/$groupId") { json(body) }
    }

    suspend fun deleteGroup(groupId: String): ApiResponse<GroupActionAck> = safeApiCall {
        client.delete("groups/$groupId")
    }

    suspend fun regenerateCode(groupId: String): ApiResponse<StudyGroup> = safeApiCall {
        client.post("groups/$groupId/regenerate-code")
    }

    suspend fun members(groupId: String, page: Int, limit: Int): ApiResponse<List<StudyGroupMember>> = safeApiCall {
        client.get("groups/$groupId/members") { paging(page, limit) }
    }

    suspend fun addMember(groupId: String, identifier: String): ApiResponse<StudyGroupMember> = safeApiCall {
        client.post("groups/$groupId/members") { json(DirectAddRequest(identifier)) }
    }

    suspend fun setMemberRole(groupId: String, userId: String, body: MemberRoleRequest): ApiResponse<StudyGroupMember> =
        safeApiCall { client.patch("groups/$groupId/members/$userId") { json(body) } }

    suspend fun removeMember(groupId: String, userId: String): ApiResponse<GroupActionAck> = safeApiCall {
        client.delete("groups/$groupId/members/$userId")
    }

    suspend fun leaveGroup(groupId: String): ApiResponse<GroupActionAck> = safeApiCall {
        client.post("groups/$groupId/leave")
    }

    suspend fun requestToJoin(groupId: String): ApiResponse<GroupJoinRequest> = safeApiCall {
        client.post("groups/$groupId/join-requests")
    }

    suspend fun joinRequests(
        groupId: String,
        status: JoinRequestStatus,
        page: Int,
        limit: Int,
    ): ApiResponse<List<GroupJoinRequest>> = safeApiCall {
        client.get("groups/$groupId/join-requests") {
            parameter("status", status.name)
            paging(page, limit)
        }
    }

    suspend fun approveJoinRequest(groupId: String, requestId: String): ApiResponse<GroupJoinRequest> = safeApiCall {
        client.post("groups/$groupId/join-requests/$requestId/approve")
    }

    suspend fun rejectJoinRequest(groupId: String, requestId: String): ApiResponse<GroupJoinRequest> = safeApiCall {
        client.post("groups/$groupId/join-requests/$requestId/reject")
    }
}
