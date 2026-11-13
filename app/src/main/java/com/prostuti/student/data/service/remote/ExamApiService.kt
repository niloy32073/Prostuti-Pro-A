package com.prostuti.student.data.service.remote

import com.prostuti.student.data.model.enums.ExamTypes
import com.prostuti.student.data.model.enums.Level
import com.prostuti.student.data.model.request.ExamGenerateRequest
import com.prostuti.student.data.model.request.ExamHistoryFilter
import com.prostuti.student.data.model.request.ExamResponseSubmitRequest
import com.prostuti.student.data.model.request.IntegrityEventSubmitRequest
import com.prostuti.student.data.model.response.AnswerSheetData
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.AttemptAnalysisData
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.data.model.response.ExamHistoryItem
import com.prostuti.student.data.model.response.ExamLeaderboardData
import com.prostuti.student.data.model.response.ExamResponseAckResponse
import com.prostuti.student.data.model.response.ExamQuestionWithOptions
import com.prostuti.student.data.model.response.ExamSummary
import com.prostuti.student.data.model.response.IntegrityEventResponse
import com.prostuti.student.data.model.response.MCQExam
import com.prostuti.student.util.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ExamApiService(private val client: HttpClient) {

    suspend fun fetchMCQExams(
        subjectId: String,
        examType: ExamTypes,
        level: Level,
        page: Int = 1,
        limit: Int = 10
    ): ApiResponse<List<MCQExam>> = safeApiCall {
        client.get("exams") {
            parameter("subject_id", subjectId)
            parameter("exam_type", examType.name)
            parameter("level", level.name)
            parameter("page", page)
            parameter("limit", limit)
        }
    }

    suspend fun generateExam(request: ExamGenerateRequest): ApiResponse<ExamAttemptData> = safeApiCall {
        client.post("exams/generate") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun attemptMCQExam(examId: String): ApiResponse<ExamAttemptData> = safeApiCall {
        client.post("exams/$examId/attempts")
    }

    suspend fun getAttemptQuestions(attemptId: String): ApiResponse<ExamAttemptData> = safeApiCall {
        client.get("exams/attempts/$attemptId/questions")
    }

    suspend fun submitResponses(
        attemptId: String,
        request: ExamResponseSubmitRequest
    ): ApiResponse<List<ExamResponseAckResponse>> = safeApiCall {
        client.post("exams/attempts/$attemptId/responses") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    /** Answers already saved for an in-progress attempt (selected_option_id = null means skipped). */
    suspend fun getAttemptResponses(attemptId: String): ApiResponse<List<ExamResponseAckResponse>> = safeApiCall {
        client.get("exams/attempts/$attemptId/responses")
    }

    /** Exam metadata only (title, duration, …) — no questions. */
    suspend fun getExam(examId: String): ApiResponse<ExamSummary> = safeApiCall {
        client.get("exams/$examId")
    }

    suspend fun submitAttempt(attemptId: String): ApiResponse<ExamAttemptResultResponse> = safeApiCall {
        client.post("exams/attempts/$attemptId/submit")
    }

    suspend fun getAttempt(attemptId: String): ApiResponse<ExamAttemptResultResponse> = safeApiCall {
        client.get("exams/attempts/$attemptId")
    }

    suspend fun listMyAttempts(
        examId: String? = null,
        unfinished: Boolean = false,
        page: Int = 1,
        limit: Int = 20
    ): ApiResponse<List<ExamAttemptResultResponse>> = safeApiCall {
        client.get("exams/attempts/manage") {
            examId?.let { parameter("exam_id", it) }
            if (unfinished) parameter("unfinished", true)
            parameter("page", page)
            parameter("limit", limit)
        }
    }

    suspend fun submitIntegrityEvents(
        attemptId: String,
        request: IntegrityEventSubmitRequest
    ): ApiResponse<List<IntegrityEventResponse>> = safeApiCall {
        client.post("exams/attempts/$attemptId/integrity-events") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    /** Finished attempts (all exam types), newest first. Unsubmitted attempts are not listed. */
    suspend fun getExamHistory(
        filter: ExamHistoryFilter,
        page: Int,
        limit: Int
    ): ApiResponse<List<ExamHistoryItem>> = safeApiCall {
        client.get("exams/attempts/history") {
            filter.subjectId?.let { parameter("subject_id", it) }
            filter.dateFrom?.let { parameter("date_from", it) }
            filter.dateTo?.let { parameter("date_to", it) }
            filter.status?.let { parameter("status", it) }
            parameter("page", page)
            parameter("limit", limit)
        }
    }

    /** Per-question result of a submitted attempt, frozen at submit time (IDs only, no text). */
    suspend fun getAnswerSheet(attemptId: String): ApiResponse<AnswerSheetData> = safeApiCall {
        client.get("exams/attempts/$attemptId/answer-sheet")
    }

    /** Per-topic correct/wrong/skipped of a submitted attempt, weakest topic first. */
    suspend fun getAttemptAnalysis(attemptId: String): ApiResponse<AttemptAnalysisData> = safeApiCall {
        client.get("exams/attempts/$attemptId/analysis")
    }

    /** Every question of an exam with options and passage, without starting an attempt. */
    suspend fun getExamQuestionsWithOptions(examId: String): ApiResponse<List<ExamQuestionWithOptions>> = safeApiCall {
        client.get("exams/$examId/questions/with-options")
    }

    suspend fun getExamLeaderboard(examId: String, page: Int = 1): ApiResponse<ExamLeaderboardData> = safeApiCall {
        client.get("exams/$examId/leaderboard") {
            parameter("page", page)
        }
    }
}
