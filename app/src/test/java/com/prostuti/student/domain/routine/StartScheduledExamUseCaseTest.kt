package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.data.model.response.SlotStart
import com.prostuti.student.data.repository.RoutineRepository
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest.Companion.defaultAttemptData
import com.prostuti.student.domain.StartOrResumeExamAttemptUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartScheduledExamUseCaseTest {
    private val routines = mockk<RoutineRepository>()
    private val exams = FakeExamAttemptRepositoryForEngineTest()
    private val useCase = StartScheduledExamUseCase(routines, StartOrResumeExamAttemptUseCase(exams))

    private val slot = SlotStart(slotId = "s1", examId = "exam1", isLive = true, closesAt = "2026-11-15T12:00:00")

    @Test fun `starts the slot then an attempt on its exam`() = runTest {
        coEvery { routines.start("s1") } returns ApiResponse.Success(slot)
        exams.startAttemptResult = ApiResponse.Success(defaultAttemptData(mode = AttemptMode.LIVE))

        val r = useCase("s1") as ApiResponse.Success

        assertEquals(slot, r.data.slot)
        assertEquals("attempt1", r.data.attempt.attemptId)
        assertEquals(AttemptMode.LIVE, r.data.attempt.mode)
    }

    @Test fun `a slot failure stops before any attempt`() = runTest {
        coEvery { routines.start("s1") } returns ApiResponse.Failure("not open", "SLOT_NOT_OPEN")

        val r = useCase("s1") as ApiResponse.Failure

        assertEquals("SLOT_NOT_OPEN", r.errorCode)
        assertNull(exams.lastListArgs)
    }

    @Test fun `an attempt failure is passed on`() = runTest {
        coEvery { routines.start("s1") } returns ApiResponse.Success(slot)
        exams.startAttemptResult = ApiResponse.Failure("empty", "EXAM_EMPTY")

        assertEquals("EXAM_EMPTY", (useCase("s1") as ApiResponse.Failure).errorCode)
    }

    @Test fun `an unfinished practice attempt is resumed like elsewhere in the engine`() = runTest {
        coEvery { routines.start("s1") } returns ApiResponse.Success(slot.copy(isLive = false))
        exams.startAttemptResult = ApiResponse.Failure("active", "ATTEMPT_ALREADY_ACTIVE")
        exams.listMyAttemptsResult = ApiResponse.Success(listOf(attempt("a7", AttemptMode.PRACTICE, endAt = null)))

        val r = useCase("s1") as ApiResponse.Success

        assertEquals("exam1" to true, exams.lastListArgs)
        assertEquals(AttemptMode.PRACTICE, r.data.attempt.mode)
    }

    companion object {
        fun attempt(id: String, mode: AttemptMode, endAt: String?, startAt: String = "2026-11-15T01:00:00", examId: String = "exam1") =
            ExamAttemptResultResponse(
                id = id, examId = examId, studentId = "me", mode = mode, startAt = startAt, endAt = endAt,
                rightAnswerCount = 0, wrongAnswerCount = 0, skipCount = 0, totalScore = 0.0
            )
    }
}
