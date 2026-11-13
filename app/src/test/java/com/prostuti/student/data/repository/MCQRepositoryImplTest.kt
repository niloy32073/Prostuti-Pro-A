package com.prostuti.student.data.repository

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.service.remote.MCQApiService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MCQRepositoryImplTest {

    private var lastRequest: HttpRequestData? = null

    // Same JSON settings as the app's HttpClient (di/appModule.kt).
    private fun repo(body: String, status: HttpStatusCode = HttpStatusCode.OK) = MCQRepositoryImpl(
        MCQApiService(
            HttpClient(MockEngine { request ->
                lastRequest = request
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }) {
                defaultRequest { url("https://api.test/api/v1/") }
                install(ContentNegotiation) { json(Json { isLenient = true; ignoreUnknownKeys = true }) }
            }
        )
    )

    @Test fun `fetchPassage gets the passage by id`() = runTest {
        val result = repo("""{ "success": true, "data": { "id": "p1", "passage_text": "Read this" } }""")
            .fetchPassage("p1")

        assertEquals(HttpMethod.Get, lastRequest?.method)
        assertEquals("/api/v1/questions/passages/p1", lastRequest?.url?.encodedPath)
        assertEquals("Read this", (result as ApiResponse.Success).data.passageText)
    }

    @Test fun `fetchMcq gets the mcq with its correct option`() = runTest {
        val result = repo(
            """{ "success": true, "data": { "id": "m1", "question_text": "Q?", "explanation": "Because",
                 "options": [ { "id": "o1", "option_text": "A", "order": 1, "is_correct": false },
                              { "id": "o2", "option_text": "B", "order": 2, "is_correct": true } ] } }"""
        ).fetchMcq("m1")

        assertEquals(HttpMethod.Get, lastRequest?.method)
        assertEquals("/api/v1/questions/mcq/m1", lastRequest?.url?.encodedPath)
        val mcq = (result as ApiResponse.Success).data
        assertEquals("Q?", mcq.questionText)
        assertEquals("o2", mcq.options.single { it.isCorrect }.id)
    }

    @Test fun `server error comes back as Failure, not an exception`() = runTest {
        val result = repo(
            """{ "success": false, "message": "Not found", "error": { "code": "NOT_FOUND" } }""",
            HttpStatusCode.NotFound
        ).fetchPassage("missing")

        assertTrue(result is ApiResponse.Failure)
        assertEquals("NOT_FOUND", (result as ApiResponse.Failure).errorCode)
    }
}
