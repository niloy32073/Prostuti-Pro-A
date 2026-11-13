package com.prostuti.student.data.service.remote

import com.prostuti.student.data.model.request.ExamHistoryFilter
import com.prostuti.student.data.model.response.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Request shape and JSON parsing of the history / answer-sheet / analysis endpoints. */
class ExamHistoryApiTest {

    private var requestedUrl: Url? = null

    private fun api(body: String, status: HttpStatusCode = HttpStatusCode.OK) = ExamApiService(
        HttpClient(MockEngine { request ->
            requestedUrl = request.url
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            defaultRequest { url("https://api.test/api/v1/") }
            install(ContentNegotiation) { json(Json { isLenient = true; ignoreUnknownKeys = true; explicitNulls = false }) }
        }
    )

    @Test
    fun `history sends only the filters that are set and parses completed and missed rows`() = runTest {
        val body = """
            { "success": true, "message": "ok",
              "meta_data": { "total_items": 2, "current_page": 1, "total_pages": 1, "page_limit": 20 },
              "data": [
                { "exam_id": "e1", "attempt_id": "a1", "exam_title_en": "Weekly", "exam_title_bn": "সাপ্তাহিক",
                  "attempted_at": "2026-11-05T04:00:00Z", "mode": "LIVE", "status": "COMPLETED",
                  "correct_count": 18, "wrong_count": 5, "skipped_count": 2, "total_score": 16.75,
                  "subject_ids": ["s1"] },
                { "exam_id": "e2", "attempt_id": "a2", "exam_title_en": "Routine", "exam_title_bn": "রুটিন",
                  "attempted_at": "2026-11-03T00:00:00Z", "mode": null, "status": "MISSED",
                  "correct_count": 0, "wrong_count": 0, "skipped_count": 0, "total_score": 0 }
              ] }
        """

        val result = api(body).getExamHistory(
            ExamHistoryFilter(subjectId = "s1", status = "COMPLETED"), page = 2, limit = 20
        )

        val url = requestedUrl!!
        assertEquals("/api/v1/exams/attempts/history", url.encodedPath)
        assertEquals("s1", url.parameters["subject_id"])
        assertEquals("COMPLETED", url.parameters["status"])
        assertEquals("2", url.parameters["page"])
        assertNull(url.parameters["date_from"])
        assertTrue(result is ApiResponse.Success)
        result as ApiResponse.Success
        assertEquals(listOf("a1", "a2"), result.data.map { it.attemptId })
        assertEquals(16.75, result.data[0].totalScore, 0.0)
        assertNull(result.data[1].mode)
        assertTrue(result.data[1].subjectIds.isEmpty())
        assertEquals(1, result.metaData?.totalPages)
    }

    @Test
    fun `history 400 surfaces the error code`() = runTest {
        val body = """{ "success": false, "message": "date_from must not be after date_to.",
            "error": { "code": "INVALID_DATE_RANGE" } }"""

        val result = api(body, HttpStatusCode.BadRequest).getExamHistory(ExamHistoryFilter(), 1, 20)

        assertTrue(result is ApiResponse.Failure)
        assertEquals("INVALID_DATE_RANGE", (result as ApiResponse.Failure).errorCode)
    }

    @Test
    fun `answer sheet parses null topic, skipped answer and explanation image`() = runTest {
        val body = """
            { "success": true, "message": "ok", "data": {
              "attempt_id": "a1", "exam_id": "e1", "source": "SNAPSHOT",
              "questions": [
                { "mcq_id": "m1", "order": 1, "subject_id": "s1", "chapter_id": "c1", "topic_id": null,
                  "selected_option_id": null, "correct_option_id": "o2", "result": "SKIPPED", "is_correct": false,
                  "explanation_text": null, "explanation_image_link": "https://cdn/e.png" } ] } }
        """

        val result = api(body).getAnswerSheet("a1")

        assertEquals("/api/v1/exams/attempts/a1/answer-sheet", requestedUrl?.encodedPath)
        result as ApiResponse.Success
        val q = result.data.questions.single()
        assertNull(q.topicId)
        assertNull(q.selectedOptionId)
        assertEquals("SKIPPED", q.result)
        assertEquals("https://cdn/e.png", q.explanationImageLink)
    }

    @Test
    fun `answer sheet of a missed exam is a 409 ATTEMPT_MISSED failure`() = runTest {
        val body = """{ "success": false, "message": "This exam was missed; there are no answers to show.",
            "error": { "code": "ATTEMPT_MISSED" } }"""

        val result = api(body, HttpStatusCode.Conflict).getAnswerSheet("a1")

        assertEquals("ATTEMPT_MISSED", (result as ApiResponse.Failure).errorCode)
    }

    @Test
    fun `analysis parses topics including the no-topic group`() = runTest {
        val body = """
            { "success": true, "message": "ok", "data": { "attempt_id": "a1", "exam_id": "e1", "source": "LIVE",
              "topics": [
                { "topic_id": "t1", "total_questions": 6, "correct": 3, "wrong": 2, "skipped": 1 },
                { "topic_id": null, "total_questions": 3, "correct": 3, "wrong": 0, "skipped": 0 } ] } }
        """

        val result = api(body).getAttemptAnalysis("a1")

        assertEquals("/api/v1/exams/attempts/a1/analysis", requestedUrl?.encodedPath)
        result as ApiResponse.Success
        assertEquals(listOf("t1", null), result.data.topics.map { it.topicId })
        assertEquals(6, result.data.topics[0].totalQuestions)
    }
}
