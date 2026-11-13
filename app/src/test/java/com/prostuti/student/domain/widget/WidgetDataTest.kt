package com.prostuti.student.domain.widget

import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.repository.fake.FakeRoutineRepository.Companion.slot
import com.prostuti.student.data.repository.fake.fakeTimedExam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** "Now" is Tue 10 Nov 2026, 20:00 Dhaka (14:00 UTC). Fake slots run 19:00–23:00 Dhaka. */
class WidgetDataTest {
    private val now = Instant.parse("2026-11-10T14:00:00Z")
    private fun at(iso: String) = Instant.parse(iso).toEpochMilli()

    @Test fun `running exams come first, then the next ones, three at most`() {
        val live = listOf(
            fakeTimedExam(examId = "live-later", windowStartEpochMillis = at("2026-11-12T10:00:00Z"), windowEndEpochMillis = at("2026-11-12T12:00:00Z")),
            fakeTimedExam(examId = "live-ended", windowStartEpochMillis = at("2026-11-10T08:00:00Z"), windowEndEpochMillis = at("2026-11-10T09:00:00Z")),
            fakeTimedExam(examId = "live-soon", windowStartEpochMillis = at("2026-11-11T03:00:00Z"), windowEndEpochMillis = at("2026-11-11T05:00:00Z")),
        )
        val slots = listOf(
            slot(id = "group-now", groupId = "g1", status = SlotMyStatus.OPEN),
            slot(id = "group-tomorrow", groupId = "g1", date = "2026-11-11"),
        )

        val picked = pickUpcoming(live, slots, now)

        assertEquals(listOf("group-now", "live-soon", "group-tomorrow"), picked.map { it.idForTest() })
        assertTrue(picked.first().isRunning(now))
    }

    @Test fun `personal, finished and missed exams aren't upcoming`() {
        val slots = listOf(
            slot(id = "mine", groupId = null),
            slot(id = "done", groupId = "g1", status = SlotMyStatus.COMPLETED),
            slot(id = "missed", groupId = "g1", date = "2026-11-11", status = SlotMyStatus.MISSED),
            slot(id = "next", groupId = "g1", date = "2026-11-11"),
        )

        assertEquals(listOf("next"), pickUpcoming(emptyList(), slots, now).map { it.idForTest() })
    }

    @Test fun `today keeps personal and group exams, earliest first, finished ones too`() {
        val slots = listOf(
            slot(id = "late", groupId = "g1", opensAt = "2026-11-10T15:00:00", closesAt = "2026-11-10T16:00:00"),
            slot(id = "early", groupId = null, opensAt = "2026-11-10T03:00:00", closesAt = "2026-11-10T04:00:00", status = SlotMyStatus.COMPLETED),
            slot(id = "tomorrow", groupId = null, date = "2026-11-11"),
        )

        assertEquals(listOf("early", "late"), pickToday(slots, LocalDate.of(2026, 11, 10)).map { it.id })
    }

    private fun UpcomingExam.idForTest(): String = when (this) {
        is UpcomingExam.Live -> exam.examId
        is UpcomingExam.Group -> slot.id
    }
}
