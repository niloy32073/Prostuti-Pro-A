package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest.Companion.defaultAttemptData
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest.Companion.fakeQuestion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StartOrResumeExamAttemptUseCaseTest {

    private val repo = FakeExamAttemptRepositoryForEngineTest()
    private val useCase = StartOrResumeExamAttemptUseCase(repo)

    private val alreadyActive = ApiResponse.Failure(message = "active", errorCode = "ATTEMPT_ALREADY_ACTIVE")

    private fun unfinished(id: String, mode: AttemptMode = AttemptMode.PRACTICE) = ExamAttemptResultResponse(
        id = id, examId = "exam1", studentId = "s1", mode = mode, startAt = "2026-01-01T00:00:00Z",
        endAt = null, rightAnswerCount = 0, wrongAnswerCount = 0, skipCount = 0, totalScore = 0.0
    )

    @Test fun `a fresh start returns the new attempt without looking anything up`() = runTest {
        repo.startAttemptResult = ApiResponse.Success(defaultAttemptData())

        val r = useCase("exam1")

        assertTrue(r is ApiResponse.Success)
        assertNull(repo.lastListArgs)
        assertEquals(0, repo.getAttemptQuestionsCallCount)
    }

    @Test fun `ATTEMPT_ALREADY_ACTIVE resumes the unfinished attempt on that exam`() = runTest {
        repo.startAttemptResult = alreadyActive
        repo.listMyAttemptsResult = ApiResponse.Success(listOf(unfinished("attempt7")))
        repo.attemptQuestionsResult = ApiResponse.Success(defaultAttemptData(questions = listOf(fakeQuestion(mcqId = "resumed"))))

        val r = useCase("exam1")

        assertEquals("resumed", (r as ApiResponse.Success).data.questions.single().mcqId)
        assertEquals("exam1" to true, repo.lastListArgs)
        assertEquals("attempt7", repo.lastQuestionsAttemptId)
    }

    @Test fun `ATTEMPT_ALREADY_ACTIVE with no unfinished attempt found returns the original error`() = runTest {
        repo.startAttemptResult = alreadyActive
        repo.listMyAttemptsResult = ApiResponse.Success(emptyList())

        assertEquals(alreadyActive, useCase("exam1"))
    }

    @Test fun `other start errors are returned unchanged`() = runTest {
        val empty = ApiResponse.Failure(message = "no questions", errorCode = "EXAM_EMPTY")
        repo.startAttemptResult = empty

        assertEquals(empty, useCase("exam1"))
        assertNull(repo.lastListArgs)
    }

    @Test fun `a leftover LIVE attempt is submitted instead of resumed`() = runTest {
        repo.startAttemptResult = alreadyActive
        repo.listMyAttemptsResult = ApiResponse.Success(listOf(unfinished("live7", mode = AttemptMode.LIVE)))

        val r = useCase("exam1")

        assertEquals("LIVE_ATTEMPT_SUBMITTED", (r as ApiResponse.Failure).errorCode)
        assertEquals(listOf("live7"), repo.submittedAttemptIds)
        assertEquals(0, repo.getAttemptQuestionsCallCount)
    }

    @Test fun `a resumed attempt carries the mode from the list`() = runTest {
        repo.startAttemptResult = alreadyActive
        repo.listMyAttemptsResult = ApiResponse.Success(listOf(unfinished("attempt7")))

        val r = useCase("exam1")

        assertEquals(AttemptMode.PRACTICE, (r as ApiResponse.Success).data.mode)
    }
}
