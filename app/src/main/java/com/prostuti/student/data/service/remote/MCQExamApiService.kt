package com.prostuti.student.data.service.remote

import com.prostuti.student.data.model.enums.ExamTypes
import com.prostuti.student.data.model.enums.Level
import com.prostuti.student.data.model.request.ExamGenerateRequest
import com.prostuti.student.data.model.request.ExamResponseSubmitRequest
import com.prostuti.student.data.model.request.IntegrityEventSubmitRequest
import com.prostuti.student.data.model.request.McqReportRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.data.model.response.ExamAttemptReviewData
import com.prostuti.student.data.model.response.ExamLeaderboardData
import com.prostuti.student.data.model.response.ExamResponseAckResponse
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.model.response.IntegrityEventResponse
import com.prostuti.student.data.model.response.MCQExam
import com.prostuti.student.data.model.response.McqReportResponse
import com.prostuti.student.util.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class MCQExamApiService(private val client: HttpClient) {

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

    suspend fun submitAttempt(attemptId: String): ApiResponse<ExamAttemptResultResponse> = safeApiCall {
        client.post("exams/attempts/$attemptId/submit")
    }

    suspend fun getAttempt(attemptId: String): ApiResponse<ExamAttemptResultResponse> = safeApiCall {
        client.get("exams/attempts/$attemptId")
    }

    suspend fun listMyAttempts(
        examId: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): ApiResponse<List<ExamAttemptResultResponse>> = safeApiCall {
        client.get("exams/attempts/manage") {
            examId?.let { parameter("exam_id", it) }
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

    suspend fun getAttemptReview(attemptId: String): ApiResponse<ExamAttemptReviewData> = safeApiCall {
        client.get("exams/attempts/$attemptId/review")
    }

    suspend fun getExamLeaderboard(examId: String, page: Int = 1): ApiResponse<ExamLeaderboardData> = safeApiCall {
        client.get("exams/$examId/leaderboard") {
            parameter("page", page)
        }
    }

    suspend fun fetchPassage(passageId: String): ApiResponse<FetchPassageData> = safeApiCall {
        client.get("questions/passages/$passageId")
    }

    suspend fun reportMcq(mcqId: String, request: McqReportRequest): ApiResponse<McqReportResponse> = safeApiCall {
        client.post("questions/mcq/$mcqId/report") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
