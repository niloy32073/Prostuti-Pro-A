package com.prostuti.student.data.service.remote

import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.JoinRequestStatus
import com.prostuti.student.data.model.request.GroupCreateRequest
import com.prostuti.student.data.model.request.GroupUpdateRequest
import com.prostuti.student.data.model.request.MemberRoleRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.service.remote.MockApi.Companion.error
import com.prostuti.student.data.service.remote.MockApi.Companion.ok
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupApiServiceTest {

    private val adminGroup = """{ "id": "g1", "name": "HSC 26-27", "description": "Weekly practice", "is_public": true,
        "max_members": 500, "member_count": 42, "created_at": "2026-11-02T10:00:00", "my_role": "ADMIN",
        "invite_code": "K7M2XQ9P", "has_pending_request": false }"""

    @Test fun `create group posts the body and parses the admin view`() = runTest {
        val mock = MockApi(ok(adminGroup), HttpStatusCode.Created)

        val r = GroupApiService(mock.client).createGroup(GroupCreateRequest(name = "HSC 26-27", isPublic = true))

        assertEquals(HttpMethod.Post, mock.method)
        assertEquals("/api/v1/groups", mock.path)
        assertEquals("HSC 26-27", mock.jsonBody()["name"]!!.jsonPrimitive.content)
        assertEquals("true", mock.jsonBody()["is_public"]!!.jsonPrimitive.content)
        r as ApiResponse.Success
        assertEquals(GroupRole.ADMIN, r.data.myRole)
        assertEquals("K7M2XQ9P", r.data.inviteCode)
        assertEquals(42, r.data.memberCount)
    }

    @Test fun `a non-member group has no role and no invite code`() = runTest {
        val mock = MockApi(ok("""[{ "id": "g2", "name": "Open", "is_public": true, "max_members": 50, "member_count": 3,
            "created_at": "2026-11-02T10:00:00", "my_role": null, "has_pending_request": true }]""",
            """{ "total_items": 1, "current_page": 1, "total_pages": 1, "page_limit": 20 }"""))

        val r = GroupApiService(mock.client).exploreGroups(query = "  ", page = 1, limit = 20)

        assertEquals("/api/v1/groups/explore", mock.path)
        assertNull("a blank search is not sent", mock.param("q"))
        assertEquals("20", mock.param("limit"))
        r as ApiResponse.Success
        assertNull(r.data[0].myRole)
        assertNull(r.data[0].inviteCode)
        assertTrue(r.data[0].hasPendingRequest)
        assertEquals(1, r.metaData?.totalItems)
    }

    @Test fun `explore sends the search text`() = runTest {
        val mock = MockApi(ok("[]"))
        GroupApiService(mock.client).exploreGroups(query = "HSC", page = 2, limit = 10)
        assertEquals("HSC", mock.param("q"))
        assertEquals("2", mock.param("page"))
    }

    @Test fun `update sends only the fields that change`() = runTest {
        val mock = MockApi(ok(adminGroup))

        GroupApiService(mock.client).updateGroup("g1", GroupUpdateRequest(isPublic = false))

        assertEquals(HttpMethod.Patch, mock.method)
        assertEquals("/api/v1/groups/g1", mock.path)
        assertEquals(setOf("is_public"), mock.jsonBody().keys)
    }

    @Test fun `join by code sends the code`() = runTest {
        val mock = MockApi(ok(adminGroup))
        GroupApiService(mock.client).joinByCode("K7M2XQ9P")
        assertEquals("/api/v1/groups/join", mock.path)
        assertEquals("K7M2XQ9P", mock.jsonBody()["code"]!!.jsonPrimitive.content)
    }

    @Test fun `old or unknown member roles read as MEMBER`() = runTest {
        val mock = MockApi(ok("""[
            { "user_id": "u1", "username": "rafi", "role": "ADMIN", "joined_at": "2026-11-02T10:05:00" },
            { "user_id": "u2", "username": "sadia", "role": "MODERATOR", "joined_at": "2026-11-02T10:06:00" },
            { "user_id": "u3", "username": "imon", "role": "MEMBER", "joined_at": "2026-11-02T10:07:00" } ]"""))

        val r = GroupApiService(mock.client).members("g1", page = 1, limit = 50)

        assertEquals("/api/v1/groups/g1/members", mock.path)
        r as ApiResponse.Success
        assertEquals(listOf(GroupRole.ADMIN, GroupRole.MEMBER, GroupRole.MEMBER), r.data.map { it.role })
    }

    @Test fun `set role patches the member`() = runTest {
        val mock = MockApi(ok("""{ "user_id": "u3", "username": "imon", "role": "ADMIN", "joined_at": "2026-11-02T10:07:00" }"""))

        val r = GroupApiService(mock.client).setMemberRole("g1", "u3", MemberRoleRequest(GroupRole.ADMIN))

        assertEquals(HttpMethod.Patch, mock.method)
        assertEquals("/api/v1/groups/g1/members/u3", mock.path)
        assertEquals("ADMIN", mock.jsonBody()["role"]!!.jsonPrimitive.content)
        assertEquals(GroupRole.ADMIN, (r as ApiResponse.Success).data.role)
    }

    @Test fun `LAST_ADMIN surfaces as the error code`() = runTest {
        val mock = MockApi(error("LAST_ADMIN"), HttpStatusCode.Conflict)

        val r = GroupApiService(mock.client).leaveGroup("g1")

        assertEquals(HttpMethod.Post, mock.method)
        assertEquals("/api/v1/groups/g1/leave", mock.path)
        assertEquals("LAST_ADMIN", (r as ApiResponse.Failure).errorCode)
    }

    @Test fun `remove member returns the group ack`() = runTest {
        val mock = MockApi(ok("""{ "group_id": "g1" }"""))
        val r = GroupApiService(mock.client).removeMember("g1", "u2")
        assertEquals(HttpMethod.Delete, mock.method)
        assertEquals("/api/v1/groups/g1/members/u2", mock.path)
        assertEquals("g1", (r as ApiResponse.Success).data.groupId)
    }

    @Test fun `join requests send the status filter and parse`() = runTest {
        val mock = MockApi(ok("""[{ "id": "r1", "group_id": "g1", "user_id": "u9", "username": null, "status": "PENDING",
            "created_at": "2026-11-02T10:00:00", "decided_at": null }]"""))

        val r = GroupApiService(mock.client).joinRequests("g1", JoinRequestStatus.APPROVED, page = 1, limit = 20)

        assertEquals("/api/v1/groups/g1/join-requests", mock.path)
        assertEquals("APPROVED", mock.param("status"))
        r as ApiResponse.Success
        assertEquals(JoinRequestStatus.PENDING, r.data[0].status)
        assertNull(r.data[0].username)
    }

    @Test fun `approve posts to the request`() = runTest {
        val mock = MockApi(ok("""{ "id": "r1", "group_id": "g1", "user_id": "u9", "status": "APPROVED",
            "created_at": "2026-11-02T10:00:00", "decided_at": "2026-11-03T08:30:00" }"""))
        val r = GroupApiService(mock.client).approveJoinRequest("g1", "r1")
        assertEquals("/api/v1/groups/g1/join-requests/r1/approve", mock.path)
        assertEquals(JoinRequestStatus.APPROVED, (r as ApiResponse.Success).data.status)
    }

    @Test fun `add member posts the identifier`() = runTest {
        val mock = MockApi(error("USER_NOT_REGISTERED"), HttpStatusCode.NotFound)
        val r = GroupApiService(mock.client).addMember("g1", "+8801712345678")
        assertEquals("/api/v1/groups/g1/members", mock.path)
        assertEquals("+8801712345678", mock.jsonBody()["identifier"]!!.jsonPrimitive.content)
        assertEquals("USER_NOT_REGISTERED", (r as ApiResponse.Failure).errorCode)
    }

    @Test fun `regenerate code returns the new code`() = runTest {
        val mock = MockApi(ok(adminGroup.replace("K7M2XQ9P", "NEWCODE1")))
        val r = GroupApiService(mock.client).regenerateCode("g1")
        assertEquals("/api/v1/groups/g1/regenerate-code", mock.path)
        assertEquals("NEWCODE1", (r as ApiResponse.Success).data.inviteCode)
        assertFalse(r.data.hasPendingRequest)
    }
}
