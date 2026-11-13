package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.SlotStart
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest.Companion.defaultAttemptData
import com.prostuti.student.data.repository.fake.FakeRoutineRepository.Companion.slot
import com.prostuti.student.domain.liveRemainingSeconds
import com.prostuti.student.domain.routine.StartScheduledExamUseCaseTest.Companion.attempt
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/** Default slot window: 10 Nov 13:00–17:00 UTC. */
class OpenScheduledExamUseCaseTest {
    private val start = mockk<StartScheduledExamUseCase>()
    private val find = mockk<FindMyAttemptForExamUseCase>()
    private val useCase = OpenScheduledExamUseCase(start, find)

    private val during = Instant.parse("2026-11-10T14:00:00Z")
    private val after = Instant.parse("2026-11-10T18:00:00Z")
    private val closes = "2026-11-10T17:00:00"
    private val slot = slot(id = "s1", status = SlotMyStatus.OPEN).copy(examId = "exam1")

    private fun started(isLive: Boolean, mode: AttemptMode) = ApiResponse.Success(
        ScheduledExamStart(SlotStart("s1", "exam1", isLive, closes), defaultAttemptData(mode = mode))
    )

    @Test fun `a live start locks the result until the window closes`() = runTest {
        coEvery { start("s1") } returns started(isLive = true, AttemptMode.LIVE)

        val o = (useCase.take(slot, during) as ApiResponse.Success).data as ScheduledExamOutcome.Attempt

        assertEquals("attempt1", o.attempt.attemptId)
        assertEquals(closes, o.lockResultUntil)
    }

    @Test fun `practice after the window isn't locked`() = runTest {
        coEvery { start("s1") } returns started(isLive = false, AttemptMode.PRACTICE)

        val o = (useCase.take(slot, after) as ApiResponse.Success).data as ScheduledExamOutcome.Attempt

        assertEquals(null, o.lockResultUntil)
    }

    @Test fun `finishing an unfinished live attempt opens its locked result (D9)`() = runTest {
        coEvery { start("s1") } returns ApiResponse.Failure("submitted", "LIVE_ATTEMPT_SUBMITTED")
        coEvery { find("exam1", true) } returns ApiResponse.Success(attempt("live7", AttemptMode.LIVE, endAt = "2026-11-10T14:30:00"))

        val o = (useCase.take(slot, during) as ApiResponse.Success).data

        assertEquals(ScheduledExamOutcome.Result("live7", closes), o)
    }

    @Test fun `other start failures pass through`() = runTest {
        coEvery { start("s1") } returns ApiResponse.Failure("not open", "SLOT_NOT_OPEN")
        assertEquals("SLOT_NOT_OPEN", (useCase.take(slot, during) as ApiResponse.Failure).errorCode)
        coVerify(exactly = 0) { find(any(), any()) }
    }

    @Test fun `viewing a result is locked only while the window is open`() = runTest {
        coEvery { find("exam1", true) } returns ApiResponse.Success(attempt("live7", AttemptMode.LIVE, endAt = "2026-11-10T14:30:00"))

        assertEquals(ScheduledExamOutcome.Result("live7", closes), (useCase.viewResult(slot, during) as ApiResponse.Success).data)
        assertEquals(ScheduledExamOutcome.Result("live7", null), (useCase.viewResult(slot, after) as ApiResponse.Success).data)
    }

    @Test fun `no submitted attempt is said so`() = runTest {
        coEvery { find("exam1", true) } returns ApiResponse.Success(null)
        assertEquals("NO_ATTEMPT", (useCase.viewResult(slot, after) as ApiResponse.Failure).errorCode)
        assertEquals("no exam yet", "NO_ATTEMPT", (useCase.viewResult(slot.copy(examId = null), after) as ApiResponse.Failure).errorCode)
    }

    @Test fun `the live timer stops at the window's end`() {
        val startAt = "2026-11-10T16:50:00"
        val now = Instant.parse("2026-11-10T16:50:00Z").toEpochMilli()
        assertEquals("25-minute exam, 10 minutes of window left", 600, liveRemainingSeconds(startAt, 25, now, closes))
        assertEquals("no window: the full duration", 1500, liveRemainingSeconds(startAt, 25, now, null))
        assertEquals(0, liveRemainingSeconds(startAt, 25, Instant.parse("2026-11-10T17:05:00Z").toEpochMilli(), closes))
    }
}
