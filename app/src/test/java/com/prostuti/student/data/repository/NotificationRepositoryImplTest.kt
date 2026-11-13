package com.prostuti.student.data.repository

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.service.remote.NotificationApiService
import com.prostuti.student.data.service.remote.PushTokenSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRepositoryImplTest {

    private val requests = mutableListOf<Pair<HttpRequestData, String>>()

    // Same JSON settings as the app's HttpClient (di/appModule.kt) — explicitNulls stays on.
    private fun buildClient(registered: Boolean) = HttpClient(MockEngine { request ->
        requests += request to String(request.body.toByteArray())
        respondJson("""{ "success": true, "message": "ok", "data": { "registered": $registered } }""")
    }) {
        defaultRequest { url("https://api.test/api/v1/") }
        install(ContentNegotiation) { json(Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true }) }
    }

    private fun MockRequestHandleScope.respondJson(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun source(token: String?) = object : PushTokenSource {
        override suspend fun currentToken() = token
    }

    private fun sentToken() = Json.parseToJsonElement(requests.single().second).jsonObject["token"]

    @Test fun `register puts the current FCM token`() = runTest {
        val repo = NotificationRepositoryImpl(NotificationApiService(buildClient(registered = true)), source("device-token-1234567890"))

        val result = repo.registerPushToken()

        assertTrue(result is ApiResponse.Success && result.data.registered)
        assertEquals(HttpMethod.Put, requests.single().first.method)
        assertEquals("/api/v1/notifications/fcm-token", requests.single().first.url.encodedPath)
        assertEquals(JsonPrimitive("device-token-1234567890"), sentToken())
    }

    @Test fun `register prefers the token handed in by onNewToken`() = runTest {
        val repo = NotificationRepositoryImpl(NotificationApiService(buildClient(registered = true)), source("old-token-1234567890"))

        repo.registerPushToken("rotated-token-1234567890")

        assertEquals(JsonPrimitive("rotated-token-1234567890"), sentToken())
    }

    @Test fun `clear sends an explicit null token`() = runTest {
        val repo = NotificationRepositoryImpl(NotificationApiService(buildClient(registered = false)), source("device-token-1234567890"))

        val result = repo.clearPushToken()

        assertTrue(result is ApiResponse.Success && !result.data.registered)
        assertEquals(JsonNull, sentToken()) // {"token": null}, not {}
    }

    @Test fun `no FCM token means no request`() = runTest {
        val repo = NotificationRepositoryImpl(NotificationApiService(buildClient(registered = true)), source(null))

        val result = repo.registerPushToken()

        assertTrue(result is ApiResponse.Failure)
        assertEquals(NotificationRepositoryImpl.PUSH_TOKEN_UNAVAILABLE, (result as ApiResponse.Failure).errorCode)
        assertTrue(requests.isEmpty())
    }
}
