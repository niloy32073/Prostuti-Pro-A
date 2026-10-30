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

/**
 * Fake for the examengine ViewModel/UseCase tests (ExamAttempt/ExamAnswerSheet/ExamLeaderboard/
 * ExamResult). Scoped to this test area only — not shared with qb/group/live/timedexam fakes.
 */
class FakeExamAttemptRepositoryForEngineTest(
    var attemptQuestionsResult: ApiResponse<ExamAttemptData> = ApiResponse.Success(defaultAttemptData()),
    var submitResponsesResult: ApiResponse<List<ExamResponseAckResponse>> = ApiResponse.Success(emptyList()),
    var submitAttemptResult: ApiResponse<ExamAttemptResultResponse> = ApiResponse.Success(defaultResult()),
    var getAttemptResult: ApiResponse<ExamAttemptResultResponse> = ApiResponse.Success(defaultResult()),
    var getAttemptReviewResult: ApiResponse<ExamAttemptReviewData> = ApiResponse.Success(defaultReview()),
    var getExamLeaderboardResult: ApiResponse<ExamLeaderboardData> = ApiResponse.Success(defaultLeaderboard()),
    var fetchPassageResult: ApiResponse<FetchPassageData> = ApiResponse.Success(FetchPassageData(id = "p1")),
    var reportMcqResult: ApiResponse<McqReportResponse> = ApiResponse.Success(McqReportResponse(id = "rep1", mcqId = "m1", reportedBy = "u1"))
) : MCQExamRepository {

    var lastSubmittedResponses: ExamResponseSubmitRequest? = null
    var getAttemptQuestionsCallCount = 0
    var getExamLeaderboardLastPage: Int? = null

    override suspend fun fetchMCQExams(subjectId: String, examType: ExamTypes, level: Level, page: Int, limit: Int): ApiResponse<List<MCQExam>> =
        ApiResponse.Success(emptyList())

    override suspend fun generateExam(request: ExamGenerateRequest): ApiResponse<ExamAttemptData> = attemptQuestionsResult

    override suspend fun attemptMCQExam(examId: String): ApiResponse<ExamAttemptData> = attemptQuestionsResult

    override suspend fun getAttemptQuestions(attemptId: String): ApiResponse<ExamAttemptData> {
        getAttemptQuestionsCallCount++
        return attemptQuestionsResult
    }

    override suspend fun submitResponses(attemptId: String, request: ExamResponseSubmitRequest): ApiResponse<List<ExamResponseAckResponse>> {
        lastSubmittedResponses = request
        return submitResponsesResult
    }

    override suspend fun submitAttempt(attemptId: String): ApiResponse<ExamAttemptResultResponse> = submitAttemptResult

    override suspend fun getAttempt(attemptId: String): ApiResponse<ExamAttemptResultResponse> = getAttemptResult

    override suspend fun listMyAttempts(examId: String?, page: Int, limit: Int): ApiResponse<List<ExamAttemptResultResponse>> =
        ApiResponse.Success(emptyList())

    override suspend fun submitIntegrityEvents(attemptId: String, request: IntegrityEventSubmitRequest): ApiResponse<List<IntegrityEventResponse>> =
        ApiResponse.Success(emptyList())

    override suspend fun getAttemptReview(attemptId: String): ApiResponse<ExamAttemptReviewData> = getAttemptReviewResult

    override suspend fun getExamLeaderboard(examId: String, page: Int): ApiResponse<ExamLeaderboardData> {
        getExamLeaderboardLastPage = page
        return getExamLeaderboardResult
    }

    override suspend fun fetchPassage(passageId: String): ApiResponse<FetchPassageData> = fetchPassageResult

    override suspend fun reportMcq(mcqId: String, request: McqReportRequest): ApiResponse<McqReportResponse> = reportMcqResult

    companion object {
        fun defaultAttemptData(questions: List<com.prostuti.student.data.model.response.Question> = listOf(fakeQuestion())) =
            ExamAttemptData(attemptId = "attempt1", duration = 10, examId = "exam1", questions = questions, startAt = "2026-01-01T00:00:00Z")

        fun fakeQuestion(
            mcqId: String = "mcq1",
            order: Int = 1,
            passageId: String? = null
        ) = com.prostuti.student.data.model.response.Question(
            mcqId = mcqId,
            options = listOf(
                com.prostuti.student.data.model.response.Option(id = "o1", optionText = "Option A", order = 1),
                com.prostuti.student.data.model.response.Option(id = "o2", optionText = "Option B", order = 2)
            ),
            order = order,
            passageId = passageId,
            questionText = "Sample question $order?"
        )

        fun defaultResult() = ExamAttemptResultResponse(
            id = "attempt1", examId = "exam1", studentId = "student1",
            mode = com.prostuti.student.data.model.enums.AttemptMode.PRACTICE,
            startAt = "2026-01-01T00:00:00Z", endAt = "2026-01-01T00:10:00Z",
            rightAnswerCount = 3, wrongAnswerCount = 1, skipCount = 1, totalScore = 3.0, rank = 2
        )

        fun defaultReview() = ExamAttemptReviewData(
            attemptId = "attempt1", examId = "exam1",
            questions = listOf(
                com.prostuti.student.data.model.response.MCQReviewQuestionResponse(
                    mcqId = "mcq1", order = 1, questionText = "Q1?",
                    options = listOf(
                        com.prostuti.student.data.model.response.MCQReviewOptionResponse(id = "o1", optionText = "A", order = 1, isCorrect = true),
                        com.prostuti.student.data.model.response.MCQReviewOptionResponse(id = "o2", optionText = "B", order = 2, isCorrect = false)
                    ),
                    selectedOptionId = "o1", isCorrect = true
                ),
                com.prostuti.student.data.model.response.MCQReviewQuestionResponse(
                    mcqId = "mcq2", order = 2, questionText = "Q2?",
                    options = listOf(
                        com.prostuti.student.data.model.response.MCQReviewOptionResponse(id = "o3", optionText = "C", order = 1, isCorrect = true),
                        com.prostuti.student.data.model.response.MCQReviewOptionResponse(id = "o4", optionText = "D", order = 2, isCorrect = false)
                    ),
                    selectedOptionId = "o4", isCorrect = false
                ),
                com.prostuti.student.data.model.response.MCQReviewQuestionResponse(
                    mcqId = "mcq3", order = 3, questionText = "Q3?",
                    options = listOf(
                        com.prostuti.student.data.model.response.MCQReviewOptionResponse(id = "o5", optionText = "E", order = 1, isCorrect = true),
                        com.prostuti.student.data.model.response.MCQReviewOptionResponse(id = "o6", optionText = "F", order = 2, isCorrect = false)
                    ),
                    selectedOptionId = null, isCorrect = null
                )
            )
        )

        fun defaultLeaderboard() = ExamLeaderboardData(
            examId = "exam1", pageNumber = 1, pageSize = 20, totalParticipants = 3,
            computedAt = "2026-01-01T00:00:00Z",
            entries = listOf(
                com.prostuti.student.data.model.response.LeaderboardEntry(rank = 1, userId = "u1", username = "Alice", score = 9.5, timeTakenSec = 300),
                com.prostuti.student.data.model.response.LeaderboardEntry(rank = 2, userId = "u2", username = "Bob", score = 8.0, timeTakenSec = 320),
                com.prostuti.student.data.model.response.LeaderboardEntry(rank = 3, userId = "u3", username = "Carol", score = 7.0, timeTakenSec = 340)
            )
        )
    }
}
