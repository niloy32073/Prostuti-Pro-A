package com.prostuti.student.ui.screen.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** HomeViewModel currently exposes only static demo state (no use cases yet — see HomeUiState's
 * doc comment); these tests pin down that default shape so a future real-data wiring is a
 * deliberate, visible change rather than a silent one. */
class HomeViewModelTest {

    @Test fun initialStateHasTwoUpcomingExamCardsWithOnePrimary() {
        val state = HomeViewModel().state.value
        assertEquals(2, state.upcomingExams.size)
        assertTrue(state.upcomingExams.count { it.isPrimary } == 1)
    }

    @Test fun initialStateShowsAdmissionReminderAndContinueProgress() {
        val state = HomeViewModel().state.value
        assertTrue(state.hasAdmissionReminder)
        assertTrue(state.hasContinueProgress)
        assertEquals(48, state.continuePercent)
    }

    @Test fun initialStateHasThreeLeaderboardEntriesRankedFirstSecondThird() {
        val state = HomeViewModel().state.value
        assertEquals(3, state.leaderboardTop3.size)
        assertEquals(listOf(2, 1, 3), state.leaderboardTop3.map { it.rank })
        assertTrue(state.showMyRankCard)
    }
}
