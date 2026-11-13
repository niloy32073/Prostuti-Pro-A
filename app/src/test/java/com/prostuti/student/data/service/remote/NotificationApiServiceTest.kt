package com.prostuti.student.data.service.remote

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.service.remote.MockApi.Companion.error
import com.prostuti.student.data.service.remote.MockApi.Companion.ok
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationApiServiceTest {

    @Test fun `list parses entries, including unknown types`() = runTest {
        val mock = MockApi(ok("""[
            { "id": "n1", "title": "New group exam", "message": "Exam on 10 Nov.", "type": "GROUP_ACTIVITY",
              "target_url": "/groups/g1", "is_read": false, "created_at": "2026-11-02T10:00:00" },
            { "id": "n2", "title": "Hi", "message": "Something new", "type": "BRAND_NEW_TYPE",
              "target_url": null, "is_read": true, "created_at": "2026-11-01T10:00:00" } ]""",
            """{ "total_items": 2, "current_page": 1, "total_pages": 1, "page_limit": 20 }"""))

        val r = NotificationApiService(mock.client).list(unreadOnly = false, page = 1, limit = 20)

        assertEquals("/api/v1/notifications", mock.path)
        assertNull("unread_only is only sent when true", mock.param("unread_only"))
        r as ApiResponse.Success
        assertEquals("/groups/g1", r.data[0].targetUrl)
        assertFalse(r.data[0].isRead)
        assertEquals("BRAND_NEW_TYPE", r.data[1].type)
        assertNull(r.data[1].targetUrl)
        assertEquals(2, r.metaData?.totalItems)
    }

    @Test fun `unread only sends the flag`() = runTest {
        val mock = MockApi(ok("[]"))
        NotificationApiService(mock.client).list(unreadOnly = true, page = 1, limit = 1)
        assertEquals("true", mock.param("unread_only"))
        assertEquals("1", mock.param("limit"))
    }

    @Test fun `mark read accepts a null-data success`() = runTest {
        val mock = MockApi(ok("null"))
        val r = NotificationApiService(mock.client).markRead("n1")
        assertEquals(HttpMethod.Post, mock.method)
        assertEquals("/api/v1/notifications/n1/read", mock.path)
        assertTrue(r is ApiResponse.Success)
    }

    @Test fun `mark read of someone else's notification fails with its code`() = runTest {
        val mock = MockApi(error("NOTIFICATION_NOT_FOUND"), HttpStatusCode.NotFound)
        val r = NotificationApiService(mock.client).markRead("n9")
        assertEquals("NOTIFICATION_NOT_FOUND", (r as ApiResponse.Failure).errorCode)
    }
}
