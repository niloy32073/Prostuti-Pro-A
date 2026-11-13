package com.prostuti.student.data.repository

import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.JoinRequestStatus
import com.prostuti.student.data.model.request.GroupCreateRequest
import com.prostuti.student.data.model.request.GroupUpdateRequest
import com.prostuti.student.data.model.request.MemberRoleRequest
import com.prostuti.student.data.service.remote.GroupApiService
import com.prostuti.student.util.retryWhileAuthzUnavailable

/** Every call retries with backoff while the server's permission check is down (§6.20). */
class GroupRepositoryImpl(private val api: GroupApiService) : GroupRepository {
    override suspend fun createGroup(body: GroupCreateRequest) = retryWhileAuthzUnavailable { api.createGroup(body) }
    override suspend fun myGroups(page: Int, limit: Int) = retryWhileAuthzUnavailable { api.myGroups(page, limit) }
    override suspend fun exploreGroups(query: String?, page: Int, limit: Int) = retryWhileAuthzUnavailable { api.exploreGroups(query, page, limit) }
    override suspend fun joinByCode(code: String) = retryWhileAuthzUnavailable { api.joinByCode(code) }
    override suspend fun group(groupId: String) = retryWhileAuthzUnavailable { api.group(groupId) }
    override suspend fun updateGroup(groupId: String, body: GroupUpdateRequest) = retryWhileAuthzUnavailable { api.updateGroup(groupId, body) }
    override suspend fun deleteGroup(groupId: String) = retryWhileAuthzUnavailable { api.deleteGroup(groupId) }
    override suspend fun regenerateCode(groupId: String) = retryWhileAuthzUnavailable { api.regenerateCode(groupId) }
    override suspend fun members(groupId: String, page: Int, limit: Int) = retryWhileAuthzUnavailable { api.members(groupId, page, limit) }
    override suspend fun addMember(groupId: String, identifier: String) = retryWhileAuthzUnavailable { api.addMember(groupId, identifier) }
    override suspend fun setMemberRole(groupId: String, userId: String, role: GroupRole) = retryWhileAuthzUnavailable { api.setMemberRole(groupId, userId, MemberRoleRequest(role)) }
    override suspend fun removeMember(groupId: String, userId: String) = retryWhileAuthzUnavailable { api.removeMember(groupId, userId) }
    override suspend fun leaveGroup(groupId: String) = retryWhileAuthzUnavailable { api.leaveGroup(groupId) }
    override suspend fun requestToJoin(groupId: String) = retryWhileAuthzUnavailable { api.requestToJoin(groupId) }
    override suspend fun joinRequests(groupId: String, status: JoinRequestStatus, page: Int, limit: Int) = retryWhileAuthzUnavailable { api.joinRequests(groupId, status, page, limit) }
    override suspend fun approveJoinRequest(groupId: String, requestId: String) = retryWhileAuthzUnavailable { api.approveJoinRequest(groupId, requestId) }
    override suspend fun rejectJoinRequest(groupId: String, requestId: String) = retryWhileAuthzUnavailable { api.rejectJoinRequest(groupId, requestId) }
}
