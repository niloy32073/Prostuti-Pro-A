package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MyTimedExamSubmission
import com.prostuti.student.data.model.response.Question
import com.prostuti.student.data.model.response.TimedExamAnswerSheet
import com.prostuti.student.data.model.response.WindowRanking
import com.prostuti.student.data.repository.TimedExamRepository

/** Configurable fake of [TimedExamRepository] for ViewModel-layer unit tests. Only
 * [mySubmission] and [advanceDemoState] are exercised by the `live` feature's ViewModels today
 * (see LiveExamListViewModel / LiveExamDetailViewModel); the rest are stubbed with harmless
 * defaults since nothing under test calls them, but call-tracking is still provided for the two
 * that matter. */
class FakeTimedExamRepositoryForTest(
    var mySubmissionResult: MyTimedExamSubmission? = null
) : TimedExamRepository {

    var advanceDemoStateCallCount = 0
    var lastAdvanceDemoStateExamId: String? = null
    var lastMySubmissionExamId: String? = null

    override suspend fun questions(): List<Question> = emptyList()

    override suspend fun examDurationMinutes(examId: String): Int = 60

    override suspend fun resolveWindow(examId: String): Pair<Long, Long> = 0L to 0L

    override suspend fun registerWindow(examId: String, windowStartEpochMillis: Long, windowEndEpochMillis: Long) = Unit

    override suspend fun mySubmission(examId: String): MyTimedExamSubmission? {
        lastMySubmissionExamId = examId
        return mySubmissionResult
    }

    override suspend fun inProgressAnswers(examId: String): Map<String, String> = emptyMap()

    override suspend fun saveAnswer(examId: String, kind: TimedExamKind, mcqId: String, optionId: String) = Unit

    override suspend fun submit(examId: String, kind: TimedExamKind): ApiResponse<MyTimedExamSubmission> =
        ApiResponse.Failure(message = "not stubbed", errorCode = "NOT_STUBBED")

    override suspend fun autoSubmitAbandonedAttempt(examId: String, kind: TimedExamKind): MyTimedExamSubmission? = null

    override suspend fun windowRanking(examId: String): ApiResponse<WindowRanking> =
        ApiResponse.Failure(message = "not stubbed", errorCode = "NOT_STUBBED")

    override suspend fun myAnswerSheet(examId: String): ApiResponse<TimedExamAnswerSheet> =
        ApiResponse.Failure(message = "not stubbed", errorCode = "NOT_STUBBED")

    override suspend fun advanceDemoState(examId: String, kind: TimedExamKind) {
        advanceDemoStateCallCount++
        lastAdvanceDemoStateExamId = examId
    }
}
