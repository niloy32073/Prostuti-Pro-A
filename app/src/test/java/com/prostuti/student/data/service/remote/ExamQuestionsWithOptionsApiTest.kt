package com.prostuti.student.data.service.remote

import com.prostuti.student.data.model.response.ApiResponse
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamQuestionsWithOptionsApiTest {

    private var requestedPath: String? = null

    /** Same JSON settings as the app's HttpClient. */
    private fun api(body: String) = ExamApiService(
        HttpClient(MockEngine { request ->
            requestedPath = request.url.encodedPath
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            defaultRequest { url("https://api.test/api/v1/") }
            install(ContentNegotiation) { json(Json { isLenient = true; ignoreUnknownKeys = true }) }
        }
    )

    @Test
    fun `empty exam with null paging meta_data parses as success`() = runTest {
        // Exact shape the test server returned for an exam with no questions.
        val body = """
            { "success": true, "message": "Exam questions with options retrieved successfully.",
              "data": [], "error": null,
              "meta_data": { "total_items": 0, "current_page": null, "total_pages": null, "page_limit": null,
                             "filters": null, "curriculum_by_exam": null } }
        """

        val result = api(body).getExamQuestionsWithOptions("exam-1")

        assertEquals("/api/v1/exams/exam-1/questions/with-options", requestedPath)
        assertTrue(result is ApiResponse.Success)
        result as ApiResponse.Success
        assertTrue(result.data.isEmpty())
        assertEquals(0, result.metaData?.totalItems)
        assertNull(result.metaData?.totalPages)
    }

    @Test
    fun `question with passage image and options parses`() = runTest {
        val body = """
            { "success": true, "message": "ok", "meta_data": { "total_items": 1 },
              "data": [ { "mcq_id": "m1", "order": 1, "question_text": "Q", "question_image_link": "https://cdn/q.png",
                          "version": "BN", "difficulty": 45, "level": "ACADEMIC", "bloom_level": "KNOWLEDGE",
                          "subject_id": "s", "chapter_id": "c", "topic_id": "t",
                          "passage_id": "p1", "passage_order": 1,
                          "passage": { "id": "p1", "passage_text": "P", "passage_image_link": "https://cdn/p.png" },
                          "options": [ { "id": "o1", "order": 1, "option_text": "A", "option_image": null, "is_correct": true },
                                       { "id": "o2", "order": 2, "option_text": "B", "option_image": null, "is_correct": null } ] } ] }
        """

        val result = api(body).getExamQuestionsWithOptions("exam-1")

        assertTrue(result is ApiResponse.Success)
        val question = (result as ApiResponse.Success).data.single()
        assertEquals("https://cdn/p.png", question.passage?.passageImage)
        assertEquals(1, question.passageOrder)
        assertEquals("A", question.options.first().optionText)
        assertEquals(listOf(true, null), question.options.map { it.isCorrect })
    }
}
