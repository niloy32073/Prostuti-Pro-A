package com.prostuti.student.data.repository.fake

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
import com.prostuti.student.data.model.response.ExamSummary
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.model.response.IntegrityEventResponse
import com.prostuti.student.data.model.response.MCQExam
import com.prostuti.student.data.model.response.McqReportResponse
import com.prostuti.student.data.repository.MCQExamRepository

/**
 * Test-only fake for [MCQExamRepository], scoped to the qb (question bank) feature's tests.
 * Only [fetchMCQExams] and [attemptMCQExam] are exercised by MCQExamListViewModel today — every
 * other member errors loudly if a test accidentally depends on it, rather than returning a
 * silently-wrong default.
 */
class FakeQbMcqExamRepository(
    private val fetchResult: (ExamTypes) -> ApiResponse<List<MCQExam>> = { ApiResponse.Success(emptyList()) },
    private val attemptResult: ApiResponse<ExamAttemptData> = ApiResponse.Failure("not configured", "NOT_CONFIGURED")
) : MCQExamRepository {
    var fetchCallCount = 0
    var lastFetchArgs: Quad? = null
    var attemptCallCount = 0
    var lastAttemptedExamId: String? = null

    data class Quad(val subjectId: String, val examType: ExamTypes, val level: Level, val page: Int)

    override suspend fun fetchMCQExams(
        subjectId: String,
        examType: ExamTypes,
        level: Level,
        page: Int,
        limit: Int
    ): ApiResponse<List<MCQExam>> {
        fetchCallCount++
        lastFetchArgs = Quad(subjectId, examType, level, page)
        return fetchResult(examType)
    }

    override suspend fun attemptMCQExam(examId: String): ApiResponse<ExamAttemptData> {
        attemptCallCount++
        lastAttemptedExamId = examId
        return attemptResult
    }

    override suspend fun generateExam(request: ExamGenerateRequest): ApiResponse<ExamAttemptData> =
        error("not used by MCQExamListViewModel tests")

    override suspend fun getAttemptQuestions(attemptId: String): ApiResponse<ExamAttemptData> =
        error("not used by MCQExamListViewModel tests")

    override suspend fun submitResponses(attemptId: String, request: ExamResponseSubmitRequest): ApiResponse<List<ExamResponseAckResponse>> =
        error("not used by MCQExamListViewModel tests")

    override suspend fun submitAttempt(attemptId: String): ApiResponse<ExamAttemptResultResponse> =
        error("not used by MCQExamListViewModel tests")

    override suspend fun getAttempt(attemptId: String): ApiResponse<ExamAttemptResultResponse> =
        error("not used by MCQExamListViewModel tests")

    override suspend fun listMyAttempts(examId: String?, unfinished: Boolean, page: Int, limit: Int): ApiResponse<List<ExamAttemptResultResponse>> =
        error("not used by MCQExamListViewModel tests")

    override suspend fun getAttemptResponses(attemptId: String): ApiResponse<List<ExamResponseAckResponse>> =
        error("not used by MCQExamListViewModel tests")

    override suspend fun getExam(examId: String): ApiResponse<ExamSummary> =
        error("not used by MCQExamListViewModel tests")

    override suspend fun submitIntegrityEvents(attemptId: String, request: IntegrityEventSubmitRequest): ApiResponse<List<IntegrityEventResponse>> =
        error("not used by MCQExamListViewModel tests")

    override suspend fun getAttemptReview(attemptId: String): ApiResponse<ExamAttemptReviewData> =
        error("not used by MCQExamListViewModel tests")

    override suspend fun getExamLeaderboard(examId: String, page: Int): ApiResponse<ExamLeaderboardData> =
        error("not used by MCQExamListViewModel tests")

    override suspend fun fetchPassage(passageId: String): ApiResponse<FetchPassageData> =
        error("not used by MCQExamListViewModel tests")

    override suspend fun reportMcq(mcqId: String, request: McqReportRequest): ApiResponse<McqReportResponse> =
        error("not used by MCQExamListViewModel tests")
}
