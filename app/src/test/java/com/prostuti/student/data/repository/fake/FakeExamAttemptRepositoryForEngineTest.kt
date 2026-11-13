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
import com.prostuti.student.data.model.response.AnswerSheetQuestion
import com.prostuti.student.data.model.response.AttemptAnalysisData
import com.prostuti.student.data.model.response.ExamHistoryItem
import com.prostuti.student.data.model.response.ExamLeaderboardData
import com.prostuti.student.data.model.response.ExamResponseAckResponse
import com.prostuti.student.data.model.response.ExamSummary
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.model.response.IntegrityEventResponse
import com.prostuti.student.data.model.response.MCQExam
import com.prostuti.student.data.model.response.McqDetail
import com.prostuti.student.data.repository.ExamRepository
import com.prostuti.student.data.repository.MCQRepository

/**
 * Fake for the examengine ViewModel/UseCase tests (ExamAttempt/ExamAnswerSheet/ExamLeaderboard/
 * ExamResult). Scoped to this test area only — not shared with qb/group/live/timedexam fakes.
 */
class FakeExamAttemptRepositoryForEngineTest(
    var attemptQuestionsResult: ApiResponse<ExamAttemptData> = ApiResponse.Success(defaultAttemptData()),
    var submitResponsesResult: ApiResponse<List<ExamResponseAckResponse>> = ApiResponse.Success(emptyList()),
    var submitAttemptResult: ApiResponse<ExamAttemptResultResponse> = ApiResponse.Success(defaultResult()),
    var getAttemptResult: ApiResponse<ExamAttemptResultResponse> = ApiResponse.Success(defaultResult()),
    var answerSheetResult: ApiResponse<AnswerSheetData> = ApiResponse.Success(defaultAnswerSheet()),
    var questionsWithOptionsResult: ApiResponse<List<ExamQuestionWithOptions>> = ApiResponse.Success(defaultQuestionsWithOptions()),
    var historyResult: ApiResponse<List<ExamHistoryItem>> = ApiResponse.Success(emptyList()),
    var analysisResult: ApiResponse<AttemptAnalysisData> = ApiResponse.Success(AttemptAnalysisData(attemptId = "attempt1", examId = "exam1")),
    var getExamLeaderboardResult: ApiResponse<ExamLeaderboardData> = ApiResponse.Success(defaultLeaderboard()),
    var fetchPassageResult: ApiResponse<FetchPassageData> = ApiResponse.Success(FetchPassageData(id = "p1")),
    var startAttemptResult: ApiResponse<ExamAttemptData>? = null,
    var listMyAttemptsResult: ApiResponse<List<ExamAttemptResultResponse>> = ApiResponse.Success(emptyList()),
    var attemptResponsesResult: ApiResponse<List<ExamResponseAckResponse>> = ApiResponse.Success(emptyList()),
    var getExamResult: ApiResponse<ExamSummary> = ApiResponse.Failure(message = "not configured", errorCode = "NOT_FOUND")
) : ExamRepository, MCQRepository {

    var lastSubmittedResponses: ExamResponseSubmitRequest? = null
    val allSubmittedResponses = mutableListOf<ExamResponseSubmitRequest>()
    var lastListArgs: Pair<String?, Boolean>? = null
    var lastQuestionsAttemptId: String? = null
    val submittedAttemptIds = mutableListOf<String>()
    var getAttemptQuestionsCallCount = 0
    var getExamLeaderboardLastPage: Int? = null

    override suspend fun fetchMCQExams(subjectId: String, examType: ExamTypes, level: Level, page: Int, limit: Int): ApiResponse<List<MCQExam>> =
        ApiResponse.Success(emptyList())

    override suspend fun generateExam(request: ExamGenerateRequest): ApiResponse<ExamAttemptData> = attemptQuestionsResult

    override suspend fun attemptMCQExam(examId: String): ApiResponse<ExamAttemptData> = startAttemptResult ?: attemptQuestionsResult

    override suspend fun getAttemptQuestions(attemptId: String): ApiResponse<ExamAttemptData> {
        getAttemptQuestionsCallCount++
        lastQuestionsAttemptId = attemptId
        return attemptQuestionsResult
    }

    override suspend fun submitResponses(attemptId: String, request: ExamResponseSubmitRequest): ApiResponse<List<ExamResponseAckResponse>> {
        lastSubmittedResponses = request
        allSubmittedResponses += request
        return submitResponsesResult
    }

    override suspend fun submitAttempt(attemptId: String): ApiResponse<ExamAttemptResultResponse> {
        submittedAttemptIds += attemptId
        return submitAttemptResult
    }

    override suspend fun getAttempt(attemptId: String): ApiResponse<ExamAttemptResultResponse> = getAttemptResult

    override suspend fun listMyAttempts(
        examId: String?,
        unfinished: Boolean,
        page: Int,
        limit: Int
    ): ApiResponse<List<ExamAttemptResultResponse>> {
        lastListArgs = examId to unfinished
        return listMyAttemptsResult
    }

    override suspend fun getAttemptResponses(attemptId: String): ApiResponse<List<ExamResponseAckResponse>> = attemptResponsesResult

    override suspend fun getExam(examId: String): ApiResponse<ExamSummary> = getExamResult

    override suspend fun submitIntegrityEvents(attemptId: String, request: IntegrityEventSubmitRequest): ApiResponse<List<IntegrityEventResponse>> =
        ApiResponse.Success(emptyList())

    val historyCalls = mutableListOf<Pair<ExamHistoryFilter, Int>>()

    override suspend fun getExamHistory(filter: ExamHistoryFilter, page: Int, limit: Int): ApiResponse<List<ExamHistoryItem>> {
        historyCalls += filter to page
        return historyResult
    }

    override suspend fun getAnswerSheet(attemptId: String): ApiResponse<AnswerSheetData> = answerSheetResult

    override suspend fun getAttemptAnalysis(attemptId: String): ApiResponse<AttemptAnalysisData> = analysisResult

    override suspend fun getExamLeaderboard(examId: String, page: Int): ApiResponse<ExamLeaderboardData> {
        getExamLeaderboardLastPage = page
        return getExamLeaderboardResult
    }

    override suspend fun getExamQuestionsWithOptions(examId: String): ApiResponse<List<ExamQuestionWithOptions>> =
        questionsWithOptionsResult

    override suspend fun fetchPassage(passageId: String): ApiResponse<FetchPassageData> = fetchPassageResult

    override suspend fun fetchMcq(mcqId: String): ApiResponse<McqDetail> =
        ApiResponse.Failure(message = "not configured", errorCode = "NOT_FOUND")


    companion object {
        const val DEFAULT_START_AT = "2026-01-01T00:00:00Z"
        const val DEFAULT_START_AT_MILLIS = 1_767_225_600_000L // DEFAULT_START_AT as epoch millis

        fun defaultAttemptData(
            questions: List<com.prostuti.student.data.model.response.Question> = listOf(fakeQuestion()),
            startAt: String = DEFAULT_START_AT,
            mode: com.prostuti.student.data.model.enums.AttemptMode = com.prostuti.student.data.model.enums.AttemptMode.PRACTICE
        ) = ExamAttemptData(attemptId = "attempt1", duration = 10, examId = "exam1", questions = questions, startAt = startAt, mode = mode)

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

        fun defaultAnswerSheet() = AnswerSheetData(
            attemptId = "attempt1", examId = "exam1", source = "SNAPSHOT",
            questions = listOf(
                AnswerSheetQuestion(mcqId = "mcq3", order = 3, subjectId = "s1", chapterId = "c1", topicId = null,
                    selectedOptionId = null, correctOptionId = "o5", result = "SKIPPED", isCorrect = false),
                AnswerSheetQuestion(mcqId = "mcq1", order = 1, subjectId = "s1", chapterId = "c1", topicId = "t1",
                    selectedOptionId = "o1", correctOptionId = "o1", result = "CORRECT", isCorrect = true,
                    explanationText = "Because A."),
                AnswerSheetQuestion(mcqId = "mcq2", order = 2, subjectId = "s1", chapterId = "c1", topicId = "t2",
                    selectedOptionId = "o4", correctOptionId = "o3", result = "WRONG", isCorrect = false)
            )
        )

        fun defaultQuestionsWithOptions() = listOf(
            ExamQuestionWithOptions(mcqId = "mcq1", order = 1, questionText = "Q1?", options = listOf(
                com.prostuti.student.data.model.response.Option(id = "o1", optionText = "A", order = 1),
                com.prostuti.student.data.model.response.Option(id = "o2", optionText = "B", order = 2))),
            ExamQuestionWithOptions(mcqId = "mcq2", order = 2, questionText = "Q2?", options = listOf(
                com.prostuti.student.data.model.response.Option(id = "o3", optionText = "C", order = 1),
                com.prostuti.student.data.model.response.Option(id = "o4", optionText = "D", order = 2))),
            ExamQuestionWithOptions(mcqId = "mcq3", order = 3, questionText = "Q3?", options = listOf(
                com.prostuti.student.data.model.response.Option(id = "o5", optionText = "E", order = 1),
                com.prostuti.student.data.model.response.Option(id = "o6", optionText = "F", order = 2)))
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
