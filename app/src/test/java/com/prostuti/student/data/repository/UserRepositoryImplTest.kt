package com.prostuti.student.data.repository

import app.cash.turbine.test
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeCurrentUserDao
import com.prostuti.student.data.repository.fake.FakeUserRepository
import com.prostuti.student.data.service.local.toEntity
import com.prostuti.student.data.service.remote.AuthApiService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class UserRepositoryImplTest {

    private val requests = mutableListOf<HttpRequestData>()

    // Same JSON settings as the app's HttpClient (di/appModule.kt).
    private fun buildClient(handler: MockRequestHandler) = HttpClient(MockEngine { request ->
        requests += request
        handler(request)
    }) {
        defaultRequest { url("https://api.test/api/v1/") }
        install(ContentNegotiation) { json(Json { isLenient = true; ignoreUnknownKeys = true }) }
    }

    private fun jsonOk(body: String, status: HttpStatusCode = HttpStatusCode.OK): MockRequestHandler = {
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    // The spec's default example: no phone / profile_image / gender, no denied lists.
    private val meBody = """
        {
          "success": true,
          "message": "Current user retrieved.",
          "data": {
            "user_id": "018f3c7e-007b-7000-8000-000000000001",
            "first_name": "Muwaz", "last_name": "Ahmed", "username": "muwy",
            "email": "muwy@proton.me", "role": "STUDENT", "status": "ACTIVE", "provider": "EMAIL",
            "is_email_verified": true, "is_phone_verified": false,
            "last_login_at": "2026-06-20T10:30:00Z", "created_at": "2025-01-15T08:00:00Z",
            "scopes": ["curriculum:read", "exam:attempt", "exam:read"]
          }
        }
    """.trimIndent()

    private val meWithDeniedBody = """
        {
          "success": true, "message": "Current user retrieved.",
          "data": {
            "user_id": "u1", "first_name": "Muwaz", "last_name": "Ahmed", "username": "muwy",
            "phone": "+8801712345678", "profile_image": "https://cdn.prostuti.com/p.jpg", "gender": "MALE",
            "role": "STUDENT", "status": "ACTIVE", "provider": "EMAIL",
            "is_email_verified": true, "is_phone_verified": false, "created_at": "2025-01-15T08:00:00Z",
            "scopes": ["exam:read"],
            "denied_scopes": ["exam:write"], "denied_roles": ["ADMIN", "TEACHER"]
          }
        }
    """.trimIndent()

    // ── ApiService ────────────────────────────────────────────────────────────────────────

    @Test fun `getMe calls GET auth me without include_denied and parses the profile`() = runTest {
        val api = AuthApiService(buildClient(jsonOk(meBody)))

        val result = api.getMe()

        assertTrue(result is ApiResponse.Success)
        val me = (result as ApiResponse.Success).data
        assertEquals("Muwaz", me.firstName)
        assertEquals("Ahmed", me.lastName)
        assertEquals("muwy", me.username)
        assertEquals("muwy@proton.me", me.email)
        assertNull(me.phone)
        assertNull(me.profileImage)
        assertTrue(me.isEmailVerified)
        assertFalse(me.isPhoneVerified)
        assertEquals(listOf("curriculum:read", "exam:attempt", "exam:read"), me.scopes)
        assertNull(me.deniedScopes)

        val request = requests.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/api/v1/auth/me", request.url.encodedPath)
        assertFalse(request.url.parameters.contains("include_denied"))
    }

    @Test fun `getMe with includeDenied sends the flag and parses the denied lists`() = runTest {
        val api = AuthApiService(buildClient(jsonOk(meWithDeniedBody)))

        val me = (api.getMe(includeDenied = true) as ApiResponse.Success).data

        assertEquals("true", requests.single().url.parameters["include_denied"])
        assertEquals(listOf("exam:write"), me.deniedScopes)
        assertEquals(listOf("ADMIN", "TEACHER"), me.deniedRoles)
        assertEquals("https://cdn.prostuti.com/p.jpg", me.profileImage)
        assertNull(me.email)
    }

    @Test fun `401 becomes a failure with the server message`() = runTest {
        val body = """{"success": false, "message": "Not authenticated", "error": {"code": "UNAUTHORIZED"}}"""
        val result = AuthApiService(buildClient(jsonOk(body, HttpStatusCode.Unauthorized))).getMe()

        assertTrue(result is ApiResponse.Failure)
        assertEquals("Not authenticated", (result as ApiResponse.Failure).message)
        assertEquals("UNAUTHORIZED", result.errorCode)
    }

    @Test fun `404 without a body becomes a failure with the status code`() = runTest {
        val result = AuthApiService(buildClient(jsonOk("", HttpStatusCode.NotFound))).getMe()

        assertEquals("404", (result as ApiResponse.Failure).errorCode)
    }

    @Test fun `500 becomes a failure`() = runTest {
        val result = AuthApiService(buildClient(jsonOk("oops", HttpStatusCode.InternalServerError))).getMe()

        assertEquals("500", (result as ApiResponse.Failure).errorCode)
    }

    @Test fun `success false from the backend becomes a failure`() = runTest {
        val body = """{"success": false, "message": "User not found", "data": null, "error": {"code": "NOT_FOUND"}}"""
        val result = AuthApiService(buildClient(jsonOk(body))).getMe()

        assertEquals("NOT_FOUND", (result as ApiResponse.Failure).errorCode)
        assertEquals("User not found", result.message)
    }

    @Test fun `missing required field becomes a failure instead of a crash`() = runTest {
        val body = """{"success": true, "message": "ok", "data": {"user_id": "u1"}}"""
        val result = AuthApiService(buildClient(jsonOk(body))).getMe()

        // Ktor 3 wraps the SerializationException in JsonConvertException, which safeApiCall
        // currently maps to UNKNOWN_ERROR rather than SERIALIZATION_ERROR.
        assertTrue(result is ApiResponse.Failure)
    }

    @Test fun `network exception becomes a failure`() = runTest {
        val result = AuthApiService(buildClient { throw IOException("Network unreachable") }).getMe()

        assertEquals("UNKNOWN_ERROR", (result as ApiResponse.Failure).errorCode)
    }

    // ── Repository (cache-first) ──────────────────────────────────────────────────────────

    @Test fun `empty cache fetches, stores and emits the profile`() = runTest {
        val dao = FakeCurrentUserDao()
        val repo = UserRepositoryImpl(AuthApiService(buildClient(jsonOk(meBody))), dao)

        val first = repo.observeMe(refresh = false).first()

        assertEquals("Muwaz", (first as ApiResponse.Success).data.firstName)
        assertEquals(1, requests.size)
        assertEquals("muwy", dao.get()?.username)
    }

    @Test fun `cached profile without refresh skips the network`() = runTest {
        val dao = FakeCurrentUserDao(FakeUserRepository.me(firstName = "Cached").toEntity())
        val repo = UserRepositoryImpl(AuthApiService(buildClient(jsonOk(meBody))), dao)

        val first = repo.observeMe(refresh = false).first()

        assertEquals("Cached", (first as ApiResponse.Success).data.firstName)
        assertTrue(requests.isEmpty())
    }

    @Test fun `refresh replaces the cached profile`() = runTest {
        val dao = FakeCurrentUserDao(FakeUserRepository.me(firstName = "Old").toEntity())
        val repo = UserRepositoryImpl(AuthApiService(buildClient(jsonOk(meBody))), dao)

        val first = repo.observeMe(refresh = true).first()

        assertEquals("Muwaz", (first as ApiResponse.Success).data.firstName)
        assertEquals(1, dao.replaceCount)
    }

    @Test fun `failure with nothing cached emits the failure and completes`() = runTest {
        val repo = UserRepositoryImpl(AuthApiService(buildClient { throw IOException("down") }), FakeCurrentUserDao())

        val all = repo.observeMe(refresh = false).toList()

        assertEquals(1, all.size)
        assertTrue(all.single() is ApiResponse.Failure)
    }

    @Test fun `failed refresh emits the failure then keeps showing the cache`() = runTest {
        val dao = FakeCurrentUserDao(FakeUserRepository.me(firstName = "Cached").toEntity())
        val repo = UserRepositoryImpl(AuthApiService(buildClient { throw IOException("down") }), dao)

        repo.observeMe(refresh = true).test {
            assertTrue(awaitItem() is ApiResponse.Failure)
            assertEquals("Cached", (awaitItem() as ApiResponse.Success).data.firstName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `cached profile round-trips every field`() = runTest {
        val me = FakeUserRepository.me(profileImage = "https://cdn/x.jpg")
        val dao = FakeCurrentUserDao(me.toEntity())
        val repo = UserRepositoryImpl(AuthApiService(buildClient(jsonOk(meBody))), dao)

        assertEquals(me, (repo.observeMe(refresh = false).first() as ApiResponse.Success).data)
    }

    @Test fun `clearCachedMe empties the cache`() = runTest {
        val dao = FakeCurrentUserDao(FakeUserRepository.me().toEntity())
        UserRepositoryImpl(AuthApiService(buildClient(jsonOk(meBody))), dao).clearCachedMe()

        assertNull(dao.get())
    }
}
