package com.prostuti.student.ui.screen.routine.slot

import com.prostuti.student.data.model.enums.SlotKind
import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.repository.fake.FakeRoutineRepository.Companion.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Default slot: 10 Nov, window 13:00–17:00 UTC (19:00–23:00 Dhaka). */
class SlotLogicTest {
    private val before = Instant.parse("2026-11-10T12:00:00Z")
    private val during = Instant.parse("2026-11-10T14:00:00Z")
    private val after = Instant.parse("2026-11-10T18:00:00Z")

    @Test fun `phase follows the clock and my status`() {
        assertEquals(SlotPhase.UPCOMING, slot().phase(before))
        assertEquals(SlotPhase.OPEN, slot(status = SlotMyStatus.OPEN).phase(during))
        assertEquals(SlotPhase.IN_PROGRESS, slot(status = SlotMyStatus.IN_PROGRESS).phase(during))
        assertEquals(SlotPhase.SUBMITTED, slot(status = SlotMyStatus.COMPLETED).phase(during))
        assertEquals(SlotPhase.ENDED, slot(status = SlotMyStatus.COMPLETED).phase(after))
        assertEquals(SlotPhase.MISSED, slot(status = SlotMyStatus.MISSED).phase(after))
    }

    @Test fun `a closed window shows as ended even if the server still says open`() {
        assertEquals(SlotPhase.MISSED, slot(status = SlotMyStatus.OPEN).phase(after))
        assertEquals("server says upcoming, clock says open", SlotPhase.OPEN, slot(status = SlotMyStatus.UPCOMING).phase(during))
    }

    @Test fun `only an exam nobody could have started can move or be deleted`() {
        assertTrue(slot().canReschedule(before))
        assertFalse(slot().canReschedule(during))
        assertTrue(slot(kind = SlotKind.ADHOC).canDelete(before))
        assertFalse("routine exams go with the routine", slot(kind = SlotKind.ROUTINE).canDelete(before))
    }

    @Test fun `next window edge`() {
        val slots = listOf(slot(), slot(id = "s2", opensAt = "2026-11-10T15:00:00", closesAt = "2026-11-10T16:00:00"))
        assertEquals(Instant.parse("2026-11-10T13:00:00Z"), slots.nextWindowEdge(before))
        assertEquals(Instant.parse("2026-11-10T15:00:00Z"), slots.nextWindowEdge(during))
        assertNull(slots.nextWindowEdge(after))
    }

    @Test fun `today badge`() {
        val open = slot(id = "open", status = SlotMyStatus.OPEN)
        val later = slot(id = "later", opensAt = "2026-11-10T16:00:00", closesAt = "2026-11-10T17:30:00")
        assertEquals(TodayBadge.Open(open), TodayBadge.of(listOf(later, open), during))
        assertEquals(TodayBadge.Upcoming(open), TodayBadge.of(listOf(later, open), before))
        assertEquals(TodayBadge.Done, TodayBadge.of(listOf(slot(status = SlotMyStatus.COMPLETED)), after))
        assertEquals("a missed one isn't done", TodayBadge.None, TodayBadge.of(listOf(slot(status = SlotMyStatus.MISSED)), after))
        assertEquals(TodayBadge.None, TodayBadge.of(emptyList(), during))
    }
}
