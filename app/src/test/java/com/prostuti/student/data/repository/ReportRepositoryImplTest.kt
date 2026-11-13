package com.prostuti.student.data.repository

import com.prostuti.student.data.model.enums.ReportResourceType
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeReportedResourceDao
import com.prostuti.student.data.service.remote.MCQApiService
import com.prostuti.student.util.TimeProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportRepositoryImplTest {

    private val requests = mutableListOf<HttpRequestData>()
    private val dao = FakeReportedResourceDao()
    private val clock = object : TimeProvider { override fun nowEpochMillis() = 1_000L }

    private fun repo(body: String, status: HttpStatusCode = HttpStatusCode.OK) = ReportRepositoryImpl(
        MCQApiService(
            HttpClient(MockEngine { request ->
                requests += request
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }) {
                defaultRequest { url("https://api.test/api/v1/") }
                install(ContentNegotiation) { json(Json { isLenient = true; ignoreUnknownKeys = true }) }
            }
        ),
        dao,
        clock
    )

    private val okBody = """{ "success": true, "message": "MCQ reported successfully.", "data": { "id": "r1", "mcq_id": "m1", "reported_by": "u1" } }"""

    @Test fun `reportMcq posts the report, then remembers the mcq as reported`() = runTest {
        val repo = repo(okBody, HttpStatusCode.Created)

        val result = repo.reportMcq("m1", "উত্তর ভুল আছে", "Option 2 is right")

        assertTrue(result is ApiResponse.Success)
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/questions/mcq/m1/report", request.url.encodedPath)
        assertTrue((request.body as TextContent).text.contains("\"description\":\"Option 2 is right\""))
        assertEquals(setOf("m1"), repo.observeReportedIds(ReportResourceType.MCQ).first())
        val row = dao.rows.value.values.single()
        assertEquals("r1", row.reportId)
        assertEquals(1_000L, row.reportedAt)
    }

    @Test fun `an mcq already reported is not sent again`() = runTest {
        val repo = repo(okBody)
        repo.reportMcq("m1", "reason", null)

        assertTrue(repo.reportMcq("m1", "another reason", null) is ApiResponse.Success)
        assertEquals(1, requests.size)
    }

    @Test fun `a failed report is not remembered`() = runTest {
        val repo = repo("""{ "success": false, "message": "MCQ not found", "error": { "code": "NOT_FOUND" } }""", HttpStatusCode.NotFound)

        assertTrue(repo.reportMcq("m1", "reason", null) is ApiResponse.Failure)
        assertTrue(repo.observeReportedIds(ReportResourceType.MCQ).first().isEmpty())
    }

    @Test fun `clearReports forgets every report`() = runTest {
        val repo = repo(okBody)
        repo.reportMcq("m1", "reason", null)

        repo.clearReports()

        assertTrue(repo.observeReportedIds(ReportResourceType.MCQ).first().isEmpty())
    }
}
