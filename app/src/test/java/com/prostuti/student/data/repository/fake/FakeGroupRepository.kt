package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.JoinRequestStatus
import com.prostuti.student.data.model.request.GroupCreateRequest
import com.prostuti.student.data.model.request.GroupUpdateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupActionAck
import com.prostuti.student.data.model.response.GroupJoinRequest
import com.prostuti.student.data.model.response.MetaData
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.model.response.StudyGroupMember
import com.prostuti.student.data.repository.GroupRepository

/** Configurable [GroupRepository] for ViewModel tests; records what was asked for. */
class FakeGroupRepository : GroupRepository {
    /** Page number -> response of `myGroups`. */
    var myGroupsPages: Map<Int, ApiResponse<List<StudyGroup>>> = mapOf(1 to ApiResponse.Success(emptyList()))
    var exploreResult: (query: String?, page: Int) -> ApiResponse<List<StudyGroup>> = { _, _ -> ApiResponse.Success(emptyList()) }
    var groupResult: ApiResponse<StudyGroup> = ApiResponse.Success(group())
    var membersPages: Map<Int, ApiResponse<List<StudyGroupMember>>> = mapOf(1 to ApiResponse.Success(emptyList()))
    var createResult: ApiResponse<StudyGroup> = ApiResponse.Success(group(id = "new", role = GroupRole.ADMIN))
    var joinResult: ApiResponse<StudyGroup> = ApiResponse.Success(group(id = "joined"))
    var updateResult: ApiResponse<StudyGroup>? = null
    var regenerateResult: ApiResponse<StudyGroup>? = null
    var addMemberResult: ApiResponse<StudyGroupMember> = ApiResponse.Success(member("added", username = "added_user"))
    var setRoleResult: ApiResponse<StudyGroupMember>? = null
    var removeResult: ApiResponse<GroupActionAck> = ApiResponse.Success(GroupActionAck("g1"))
    var leaveResult: ApiResponse<GroupActionAck> = ApiResponse.Success(GroupActionAck("g1"))
    var deleteResult: ApiResponse<GroupActionAck> = ApiResponse.Success(GroupActionAck("g1"))
    var requestToJoinResult: ApiResponse<GroupJoinRequest> = ApiResponse.Success(request("r-new"))
    var joinRequestsResult: (status: JoinRequestStatus, page: Int) -> ApiResponse<List<GroupJoinRequest>> = { _, _ -> ApiResponse.Success(emptyList()) }
    var decideResult: ApiResponse<GroupJoinRequest>? = null

    val myGroupsPagesAsked = mutableListOf<Int>()
    val membersAsked = mutableListOf<Pair<Int, Int>>()
    val exploreQueries = mutableListOf<String?>()
    val joinRequestsAsked = mutableListOf<Triple<JoinRequestStatus, Int, Int>>()
    var lastCreate: GroupCreateRequest? = null
    var lastUpdate: GroupUpdateRequest? = null
    var lastJoinCode: String? = null
    var lastAddIdentifier: String? = null
    var lastRole: Pair<String, GroupRole>? = null
    var groupCalls = 0

    override suspend fun createGroup(body: GroupCreateRequest) = createResult.also { lastCreate = body }
    override suspend fun myGroups(page: Int, limit: Int): ApiResponse<List<StudyGroup>> {
        myGroupsPagesAsked += page
        return myGroupsPages[page] ?: ApiResponse.Success(emptyList())
    }
    override suspend fun exploreGroups(query: String?, page: Int, limit: Int): ApiResponse<List<StudyGroup>> {
        exploreQueries += query
        return exploreResult(query, page)
    }
    override suspend fun joinByCode(code: String) = joinResult.also { lastJoinCode = code }
    override suspend fun group(groupId: String) = groupResult.also { groupCalls++ }
    override suspend fun updateGroup(groupId: String, body: GroupUpdateRequest): ApiResponse<StudyGroup> {
        lastUpdate = body
        return updateResult ?: groupResult
    }
    override suspend fun deleteGroup(groupId: String) = deleteResult
    override suspend fun regenerateCode(groupId: String) = regenerateResult ?: groupResult
    override suspend fun members(groupId: String, page: Int, limit: Int): ApiResponse<List<StudyGroupMember>> {
        membersAsked += page to limit
        return membersPages[page] ?: ApiResponse.Success(emptyList())
    }
    override suspend fun addMember(groupId: String, identifier: String) = addMemberResult.also { lastAddIdentifier = identifier }
    override suspend fun setMemberRole(groupId: String, userId: String, role: GroupRole): ApiResponse<StudyGroupMember> {
        lastRole = userId to role
        return setRoleResult ?: ApiResponse.Success(member(userId, role = role))
    }
    override suspend fun removeMember(groupId: String, userId: String) = removeResult
    override suspend fun leaveGroup(groupId: String) = leaveResult
    override suspend fun requestToJoin(groupId: String) = requestToJoinResult
    override suspend fun joinRequests(groupId: String, status: JoinRequestStatus, page: Int, limit: Int): ApiResponse<List<GroupJoinRequest>> {
        joinRequestsAsked += Triple(status, page, limit)
        return joinRequestsResult(status, page)
    }
    override suspend fun approveJoinRequest(groupId: String, requestId: String) =
        decideResult ?: ApiResponse.Success(request(requestId, JoinRequestStatus.APPROVED))
    override suspend fun rejectJoinRequest(groupId: String, requestId: String) =
        decideResult ?: ApiResponse.Success(request(requestId, JoinRequestStatus.REJECTED))

    companion object {
        fun group(
            id: String = "g1", role: GroupRole? = GroupRole.MEMBER, inviteCode: String? = null, memberCount: Int = 3,
            isPublic: Boolean = false, maxMembers: Int = 500, hasPendingRequest: Boolean = false, description: String? = null,
        ) = StudyGroup(
            id = id, name = "HSC 26-27", description = description, isPublic = isPublic, maxMembers = maxMembers, memberCount = memberCount,
            createdAt = "2026-11-02T10:00:00", myRole = role, inviteCode = inviteCode, hasPendingRequest = hasPendingRequest,
        )

        fun member(userId: String, username: String = "user_$userId", role: GroupRole = GroupRole.MEMBER) =
            StudyGroupMember(userId = userId, username = username, role = role, joinedAt = "2026-11-02T10:05:00")

        fun request(id: String, status: JoinRequestStatus = JoinRequestStatus.PENDING) = GroupJoinRequest(
            id = id, groupId = "g1", userId = "u_$id", username = "user_$id", status = status, createdAt = "2026-11-02T10:00:00",
        )

        fun page(totalPages: Int, totalItems: Int? = null) =
            MetaData(currentPage = null, pageLimit = null, totalItems = totalItems, totalPages = totalPages)
    }
}
