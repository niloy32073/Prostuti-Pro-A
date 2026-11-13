package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.domain.routine.StartScheduledExamUseCaseTest.Companion.attempt
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FindMyAttemptForExamUseCaseTest {
    private val exams = FakeExamAttemptRepositoryForEngineTest()
    private val useCase = FindMyAttemptForExamUseCase(exams)

    private val live = attempt("live", AttemptMode.LIVE, endAt = "2026-11-15T01:20:00", startAt = "2026-11-15T01:00:00")
    private val retake = attempt("retake", AttemptMode.PRACTICE, endAt = "2026-11-16T09:20:00", startAt = "2026-11-16T09:00:00")
    private val unfinished = attempt("open", AttemptMode.PRACTICE, endAt = null, startAt = "2026-11-17T09:00:00")
    private val otherExam = attempt("other", AttemptMode.LIVE, endAt = "2026-11-18T01:20:00", startAt = "2026-11-18T01:00:00", examId = "exam2")

    @Test fun `picks the latest submitted attempt on that exam`() = runTest {
        exams.listMyAttemptsResult = ApiResponse.Success(listOf(live, retake, unfinished, otherExam))

        val r = useCase("exam1") as ApiResponse.Success

        assertEquals("retake", r.data?.id)
        assertEquals("exam1" to false, exams.lastListArgs)
    }

    @Test fun `live only skips practice retakes`() = runTest {
        exams.listMyAttemptsResult = ApiResponse.Success(listOf(live, retake))
        assertEquals("live", (useCase("exam1", liveOnly = true) as ApiResponse.Success).data?.id)
    }

    @Test fun `no submitted attempt gives null`() = runTest {
        exams.listMyAttemptsResult = ApiResponse.Success(listOf(unfinished))
        assertNull((useCase("exam1") as ApiResponse.Success).data)
    }

    @Test fun `a list failure is passed on`() = runTest {
        exams.listMyAttemptsResult = ApiResponse.Failure("down", "NETWORK_ERROR")
        assertEquals("NETWORK_ERROR", (useCase("exam1") as ApiResponse.Failure).errorCode)
    }
}
