package com.prostuti.student.data.repository

import com.prostuti.student.data.model.enums.DeviceCategory
import com.prostuti.student.data.model.request.PasswordBasedSignInRequest
import com.prostuti.student.data.model.request.ResetPasswordRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeCurrentUserDao
import com.prostuti.student.data.repository.fake.FakeDataStoreStorageRepository
import com.prostuti.student.data.repository.fake.FakeNotificationRepository
import com.prostuti.student.data.repository.fake.FakeUserRepository
import com.prostuti.student.data.service.local.toEntity
import com.prostuti.student.data.service.remote.AuthApiService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {

    // Same JSON settings as the app's HttpClient (di/appModule.kt).
    private fun buildClient(body: String, status: HttpStatusCode) = HttpClient(MockEngine {
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }) {
        defaultRequest { url("https://api.test/api/v1/") }
        install(ContentNegotiation) { json(Json { isLenient = true; ignoreUnknownKeys = true }) }
    }

    private val loginOkBody = """
        {
          "success": true, "message": "Logged in.",
          "data": {
            "access_token": "new-access", "refresh_token": "new-refresh", "token_type": "Bearer",
            "expires_in": 900, "user_id": "u2", "username": "next", "email": "next@test.com",
            "first_name": "Next", "last_name": "User", "role": "STUDENT", "status": "ACTIVE",
            "is_email_verified": true, "is_phone_verified": false
          }
        }
    """.trimIndent()

    private val loginFailBody = """
        { "success": false, "message": "Wrong password", "error": { "code": "INVALID_CREDENTIALS" } }
    """.trimIndent()

    private val request = PasswordBasedSignInRequest(
        identifier = "next@test.com", password = "secret",
        deviceCategory = DeviceCategory.entries.first(), deviceName = "test"
    )

    @Test fun `successful sign in clears the previous user's cached profile`() = runTest {
        val dao = FakeCurrentUserDao(FakeUserRepository.me(firstName = "Previous").toEntity())
        val storage = FakeDataStoreStorageRepository()
        val repo = AuthRepositoryImpl(AuthApiService(buildClient(loginOkBody, HttpStatusCode.OK)), storage, dao, FakeNotificationRepository(), this)

        val result = repo.passwordBasedSignIn(request)

        assertTrue(result is ApiResponse.Success)
        assertNull(dao.get())
        assertEquals("new-access", storage.getJWTToken())
    }

    @Test fun `failed sign in keeps the cached profile`() = runTest {
        val dao = FakeCurrentUserDao(FakeUserRepository.me(firstName = "Current").toEntity())
        val storage = FakeDataStoreStorageRepository()
        val repo = AuthRepositoryImpl(AuthApiService(buildClient(loginFailBody, HttpStatusCode.Unauthorized)), storage, dao, FakeNotificationRepository(), this)

        val result = repo.passwordBasedSignIn(request)

        assertTrue(result is ApiResponse.Failure)
        assertNotNull(dao.get())
        assertNull(storage.getJWTToken())
    }

    @Test fun `successful sign in uploads this device's push token`() = runTest {
        val notifications = FakeNotificationRepository()
        val repo = AuthRepositoryImpl(
            AuthApiService(buildClient(loginOkBody, HttpStatusCode.OK)),
            FakeDataStoreStorageRepository(), FakeCurrentUserDao(null), notifications, this
        )

        repo.passwordBasedSignIn(request)
        advanceUntilIdle()

        assertEquals(listOf<String?>(null), notifications.registeredTokens) // null = fetch the current FCM token
    }

    @Test fun `failed sign in does not upload a push token`() = runTest {
        val notifications = FakeNotificationRepository()
        val repo = AuthRepositoryImpl(
            AuthApiService(buildClient(loginFailBody, HttpStatusCode.Unauthorized)),
            FakeDataStoreStorageRepository(), FakeCurrentUserDao(null), notifications, this
        )

        repo.passwordBasedSignIn(request)
        advanceUntilIdle()

        assertTrue(notifications.registeredTokens.isEmpty())
    }

    private val resetRequest = ResetPasswordRequest(identifier = "next@test.com", code = "612461", newPassword = "Newpass12")

    private fun CoroutineScope.resetRepo(body: String, status: HttpStatusCode, onRequest: suspend (HttpRequestData) -> Unit = {}) =
        AuthRepositoryImpl(
            AuthApiService(HttpClient(MockEngine { req ->
                onRequest(req)
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }) {
                defaultRequest { url("https://api.test/api/v1/") }
                install(ContentNegotiation) { json(Json { isLenient = true; ignoreUnknownKeys = true }) }
            }),
            FakeDataStoreStorageRepository(), FakeCurrentUserDao(null), FakeNotificationRepository(), this
        )

    @Test fun `password reset posts identifier, code and new password in one call`() = runTest {
        var path: String? = null
        var sentBody: String? = null
        val repo = resetRepo("""{ "success": true, "message": "Password reset.", "data": null }""", HttpStatusCode.OK) { req ->
            path = req.url.encodedPath
            sentBody = req.body.toByteArray().decodeToString()
        }

        val result = repo.resetPassword(resetRequest)

        assertTrue(result is ApiResponse.Success) // "data": null is still a success here
        assertEquals("/api/v1/auth/password/reset", path)
        val json = Json.parseToJsonElement(sentBody!!).toString()
        assertTrue(json.contains("\"identifier\":\"next@test.com\""))
        assertTrue(json.contains("\"code\":\"612461\""))
        assertTrue(json.contains("\"new_password\":\"Newpass12\""))
    }

    @Test fun `password reset with a wrong code is a failure with the server message`() = runTest {
        val repo = resetRepo(
            """{ "success": false, "message": "Invalid or expired OTP.", "data": null, "error": { "code": "OTP_INVALID" } }""",
            HttpStatusCode.BadRequest
        )

        val result = repo.resetPassword(resetRequest)

        assertTrue(result is ApiResponse.Failure)
        assertEquals("Invalid or expired OTP.", (result as ApiResponse.Failure).message)
    }
}
