package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.enums.ExamTypes
import com.prostuti.student.data.model.enums.Level
import com.prostuti.student.data.model.request.ExamGenerateRequest
import com.prostuti.student.data.model.request.ExamResponseSubmitRequest
import com.prostuti.student.data.model.request.IntegrityEventSubmitRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.data.model.response.ExamQuestionWithOptions
import com.prostuti.student.data.model.request.ExamHistoryFilter
import com.prostuti.student.data.model.response.AnswerSheetData
import com.prostuti.student.data.model.response.AttemptAnalysisData
import com.prostuti.student.data.model.response.ExamHistoryItem
import com.prostuti.student.data.model.response.ExamLeaderboardData
import com.prostuti.student.data.model.response.ExamResponseAckResponse
import com.prostuti.student.data.model.response.ExamSummary
import com.prostuti.student.data.model.response.IntegrityEventResponse
import com.prostuti.student.data.model.response.MCQExam
import com.prostuti.student.data.repository.ExamRepository

/** androidTest-scoped copy of the test/ fake of the same name — `test` and `androidTest` are
 * separate compile units in this project (no shared source set), so this can't be reused as-is. */
class FakeQbMcqExamRepository(
    private val fetchResult: (ExamTypes) -> ApiResponse<List<MCQExam>> = { ApiResponse.Success(emptyList()) },
    private val attemptResult: ApiResponse<ExamAttemptData> = ApiResponse.Failure("not configured", "NOT_CONFIGURED"),
    private val questionsWithOptionsResult: ApiResponse<List<ExamQuestionWithOptions>> = ApiResponse.Failure("not configured", "NOT_CONFIGURED")
) : ExamRepository {
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

    override suspend fun listMyAttempts(examId: String?, unfinished: Boolean, page: Int, limit: Int): ApiResponse<List<ExamAttemptResultResponse>> =
        error("not used by MCQExamListScreen tests")

    override suspend fun getAttemptResponses(attemptId: String): ApiResponse<List<ExamResponseAckResponse>> =
        error("not used by MCQExamListScreen tests")

    override suspend fun getExam(examId: String): ApiResponse<ExamSummary> =
        error("not used by MCQExamListScreen tests")

    override suspend fun submitIntegrityEvents(attemptId: String, request: IntegrityEventSubmitRequest): ApiResponse<List<IntegrityEventResponse>> =
        error("not used by MCQExamListScreen tests")

    override suspend fun getExamHistory(filter: ExamHistoryFilter, page: Int, limit: Int): ApiResponse<List<ExamHistoryItem>> =
        ApiResponse.Failure("not used", "NOT_USED")

    override suspend fun getAnswerSheet(attemptId: String): ApiResponse<AnswerSheetData> =
        ApiResponse.Failure("not used", "NOT_USED")

    override suspend fun getAttemptAnalysis(attemptId: String): ApiResponse<AttemptAnalysisData> =
        ApiResponse.Failure("not used", "NOT_USED")

    override suspend fun getExamLeaderboard(examId: String, page: Int): ApiResponse<ExamLeaderboardData> =
        error("not used by MCQExamListScreen tests")

    var questionsWithOptionsCalls = 0
        private set

    override suspend fun getExamQuestionsWithOptions(examId: String): ApiResponse<List<ExamQuestionWithOptions>> {
        questionsWithOptionsCalls++
        return questionsWithOptionsResult
    }
}
