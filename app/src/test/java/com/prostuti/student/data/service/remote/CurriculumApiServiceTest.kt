package com.prostuti.student.data.service.remote

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.service.remote.MockApi.Companion.error
import com.prostuti.student.data.service.remote.MockApi.Companion.ok
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** The curriculum lists come in pages; the service must return all of them, not just the first. */
class CurriculumApiServiceTest {
    private val pagesAsked = mutableListOf<Pair<String?, String?>>()

    /** A server holding [total] subjects, [pageSize] a page; [failPage] answers with an error. */
    private fun server(total: Int, withTotalPages: Boolean = true, failPage: Int? = null) = HttpClient(MockEngine { request ->
        val page = request.url.parameters["page"]!!.toInt()
        val limit = request.url.parameters["limit"]!!.toInt()
        pagesAsked += request.url.parameters["page"] to request.url.parameters["limit"]
        if (page == failPage) return@MockEngine respond(error("TIMEOUT_ERROR"), HttpStatusCode.ServiceUnavailable, headersOf(HttpHeaders.ContentType, "application/json"))
        val ids = ((page - 1) * limit until minOf(page * limit, total))
        val data = ids.joinToString(",", "[", "]") { """{ "id": "s$it", "name_bn": "বিষয় $it", "name_en": "Subject $it", "created_at": "2026-01-01T00:00:00", "created_by": "admin" }""" }
        val pages = (total + limit - 1) / limit
        val meta = if (withTotalPages) """{ "total_items": $total, "current_page": $page, "total_pages": $pages, "page_limit": $limit }""" else "null"
        respond(ok(data, meta), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }) {
        defaultRequest { url("https://api.test/api/v1/") }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }
    }

    @Test fun `every page is fetched, at the largest page size`() = runTest {
        val r = CurriculumApiService(server(total = 230)).fetchSubjects()
        assertEquals(230, (r as ApiResponse.Success).data.size)
        assertEquals("s229", r.data.last().id)
        assertEquals(listOf("1" to "100", "2" to "100", "3" to "100"), pagesAsked)
    }

    @Test fun `without total_pages a short page ends the list`() = runTest {
        val r = CurriculumApiService(server(total = 150, withTotalPages = false)).fetchSubjects()
        assertEquals(150, (r as ApiResponse.Success).data.size)
        assertEquals(2, pagesAsked.size)
    }

    @Test fun `one page is one call`() = runTest {
        val r = CurriculumApiService(server(total = 12)).fetchSubjects()
        assertEquals(12, (r as ApiResponse.Success).data.size)
        assertEquals(1, pagesAsked.size)
    }

    @Test fun `a failed page fails the whole list`() = runTest {
        val r = CurriculumApiService(server(total = 230, failPage = 2)).fetchSubjects()
        assertEquals("TIMEOUT_ERROR", (r as ApiResponse.Failure).errorCode)
    }
}
