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
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.model.response.IntegrityEventResponse
import com.prostuti.student.data.model.response.MCQExam
import com.prostuti.student.data.model.response.McqReportResponse
import com.prostuti.student.data.repository.MCQExamRepository

/** androidTest-scoped copy of the test/ fake of the same name — `test` and `androidTest` are
 * separate compile units in this project (no shared source set), so this can't be reused as-is. */
class FakeQbMcqExamRepository(
    private val fetchResult: (ExamTypes) -> ApiResponse<List<MCQExam>> = { ApiResponse.Success(emptyList()) },
    private val attemptResult: ApiResponse<ExamAttemptData> = ApiResponse.Failure("not configured", "NOT_CONFIGURED")
) : MCQExamRepository {
    var fetchCallCount = 0
    var attemptCallCount = 0

    override suspend fun fetchMCQExams(
        subjectId: String,
        examType: ExamTypes,
        level: Level,
        page: Int,
        limit: Int
    ): ApiResponse<List<MCQExam>> {
        fetchCallCount++
        return fetchResult(examType)
    }

    override suspend fun attemptMCQExam(examId: String): ApiResponse<ExamAttemptData> {
        attemptCallCount++
        return attemptResult
    }

    override suspend fun generateExam(request: ExamGenerateRequest): ApiResponse<ExamAttemptData> =
        error("not used by MCQExamListScreen tests")

    override suspend fun getAttemptQuestions(attemptId: String): ApiResponse<ExamAttemptData> =
        error("not used by MCQExamListScreen tests")

    override suspend fun submitResponses(attemptId: String, request: ExamResponseSubmitRequest): ApiResponse<List<ExamResponseAckResponse>> =
        error("not used by MCQExamListScreen tests")

    override suspend fun submitAttempt(attemptId: String): ApiResponse<ExamAttemptResultResponse> =
        error("not used by MCQExamListScreen tests")

    override suspend fun getAttempt(attemptId: String): ApiResponse<ExamAttemptResultResponse> =
        error("not used by MCQExamListScreen tests")

    override suspend fun listMyAttempts(examId: String?, page: Int, limit: Int): ApiResponse<List<ExamAttemptResultResponse>> =
        error("not used by MCQExamListScreen tests")

    override suspend fun submitIntegrityEvents(attemptId: String, request: IntegrityEventSubmitRequest): ApiResponse<List<IntegrityEventResponse>> =
        error("not used by MCQExamListScreen tests")

    override suspend fun getAttemptReview(attemptId: String): ApiResponse<ExamAttemptReviewData> =
        error("not used by MCQExamListScreen tests")

    override suspend fun getExamLeaderboard(examId: String, page: Int): ApiResponse<ExamLeaderboardData> =
        error("not used by MCQExamListScreen tests")

    override suspend fun fetchPassage(passageId: String): ApiResponse<FetchPassageData> =
        error("not used by MCQExamListScreen tests")

    override suspend fun reportMcq(mcqId: String, request: McqReportRequest): ApiResponse<McqReportResponse> =
        error("not used by MCQExamListScreen tests")
}
