package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ExamResponseAckResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class PracticeElapsedTimeTest {
    private fun saved(timeMs: Int, optionId: String? = "o1") =
        ExamResponseAckResponse(mcqId = "m$timeMs", selectedOptionId = optionId, responseTimeMs = timeMs)

    // The real log: response_time grows 6000 → 23000, i.e. it is cumulative exam time.
    private val realLog = listOf(6_000, 8_000, 10_000, 12_000, 13_000, 15_000, 17_000, 19_000, 23_000).map { saved(it) }

    @Test fun `server time is the biggest response_time`() =
        assertEquals(23_000L, practiceElapsedMs(realLog, localElapsedMs = null))

    @Test fun `local wins when it is ahead`() =
        assertEquals(30_000L, practiceElapsedMs(realLog, localElapsedMs = 30_000))

    @Test fun `server wins when it is ahead`() =
        assertEquals(23_000L, practiceElapsedMs(realLog, localElapsedMs = 5_000))

    @Test fun `equal values are fine`() =
        assertEquals(23_000L, practiceElapsedMs(realLog, localElapsedMs = 23_000))

    @Test fun `skipped answers still count`() =
        assertEquals(9_000L, practiceElapsedMs(listOf(saved(4_000), saved(9_000, optionId = null)), localElapsedMs = null))

    @Test fun `nothing anywhere means no time used`() =
        assertEquals(0L, practiceElapsedMs(emptyList(), localElapsedMs = null))
}
