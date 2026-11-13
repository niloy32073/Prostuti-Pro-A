package com.prostuti.student.ui.screen.group

import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamLeaderboardData
import com.prostuti.student.data.model.response.LeaderboardEntry
import com.prostuti.student.data.repository.DummyGroupExamStatsProvider
import com.prostuti.student.data.repository.fake.FakeGroupRepository
import com.prostuti.student.data.repository.fake.FakeGroupRepository.Companion.group
import com.prostuti.student.data.repository.fake.FakeQbCurriculumRepository
import com.prostuti.student.data.repository.fake.FakeRoutineRepository
import com.prostuti.student.data.repository.fake.FakeRoutineRepository.Companion.slot
import com.prostuti.student.data.repository.fake.FakeUserRepository
import com.prostuti.student.domain.GetExamLeaderboardUseCase
import com.prostuti.student.domain.GetMeUseCase
import com.prostuti.student.domain.group.FetchGroupUseCase
import com.prostuti.student.domain.routine.DescribeTopicGroupingUseCase
import com.prostuti.student.domain.routine.FetchPastSlotsUseCase
import com.prostuti.student.domain.routine.FindMyAttemptForExamUseCase
import com.prostuti.student.domain.routine.StartScheduledExamUseCaseTest.Companion.attempt
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** "Now" is 10 Nov 20:00 Dhaka; the ranked exam's window closed at 17:00 UTC on 9 Nov. */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupRankingViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val groups = FakeGroupRepository().apply { groupResult = ApiResponse.Success(group(memberCount = 40)) }
    private val routines = FakeRoutineRepository()
    private val leaderboard = mockk<GetExamLeaderboardUseCase>()
    private val findMyAttempt = mockk<FindMyAttemptForExamUseCase>()
    private val now = Instant.parse("2026-11-10T14:00:00Z")

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun entry(rank: Int, user: String = "u$rank") = LeaderboardEntry(rank, user, "name$rank", 20.0 - rank, 600)
    private fun page(n: Int, total: Int, entries: List<LeaderboardEntry>) =
        ApiResponse.Success(ExamLeaderboardData("e1", n, entries.size, total, "2026-11-09T17:10:00", entries))

    private fun rankingVm(me: String = "me", closesAt: String = "2026-11-09T17:00:00") = GroupRankingViewModel(
        "g1", "e1", closesAt, FetchGroupUseCase(groups), leaderboard, findMyAttempt,
        GetMeUseCase(FakeUserRepository(listOf(ApiResponse.Success(FakeUserRepository.me().copy(userId = me))))),
        DummyGroupExamStatsProvider(), testErrorMessages, now = { now },
    )

    @Test fun `ranking shows took part X of Y and finds me on a later page`() = runTest(dispatcher) {
        coEvery { findMyAttempt("e1", true) } returns ApiResponse.Success(attempt("a1", AttemptMode.LIVE, endAt = "2026-11-09T14:00:00"))
        coEvery { leaderboard("e1", 1) } returns page(1, 4, listOf(entry(1), entry(2)))
        coEvery { leaderboard("e1", 2) } returns page(2, 4, listOf(entry(3), entry(4, "me")))
        val vm = rankingVm()
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(40, s.memberCount)
        assertEquals(4, s.totalParticipants)
        assertEquals(4, s.myEntry?.rank)
        assertEquals("a1", s.myAttemptId)
        assertFalse(s.canLoadMore)
        assertTrue("dummy stats are there", s.stats != null)
    }

    @Test fun `my rank isn't searched for when I didn't take part`() = runTest(dispatcher) {
        coEvery { findMyAttempt("e1", true) } returns ApiResponse.Success(null)
        coEvery { leaderboard("e1", 1) } returns page(1, 50, listOf(entry(1), entry(2)))
        val vm = rankingVm()
        advanceUntilIdle()

        assertNull(vm.state.value.myEntry)
        assertNull(vm.state.value.myAttemptId)
        coVerify(exactly = 0) { leaderboard("e1", 2) }
    }

    @Test fun `my rank search stops after five pages`() = runTest(dispatcher) {
        coEvery { findMyAttempt("e1", true) } returns ApiResponse.Success(attempt("a1", AttemptMode.LIVE, endAt = "2026-11-09T14:00:00"))
        coEvery { leaderboard("e1", any()) } answers { val p = secondArg<Int>(); page(p, 1000, listOf(entry(p * 2 - 1), entry(p * 2))) }
        val vm = rankingVm()
        advanceUntilIdle()

        assertEquals(5, vm.state.value.page)
        assertNull(vm.state.value.myEntry)
    }

    @Test fun `a 404 means the ranking isn't built yet`() = runTest(dispatcher) {
        coEvery { findMyAttempt("e1", true) } returns ApiResponse.Success(null)
        coEvery { leaderboard("e1", 1) } returns ApiResponse.Failure("not found", "404")
        val vm = rankingVm()
        advanceUntilIdle()

        assertTrue(vm.state.value.notReady)
        assertTrue(vm.state.value.errors.isEmpty())
        assertFalse(vm.state.value.windowOpen)
    }

    @Test fun `an open window says the ranking comes later`() = runTest(dispatcher) {
        coEvery { findMyAttempt("e1", true) } returns ApiResponse.Success(null)
        coEvery { leaderboard("e1", 1) } returns ApiResponse.Failure("not found", "404")
        val vm = rankingVm(closesAt = "2026-11-10T17:00:00")
        advanceUntilIdle()
        assertTrue(vm.state.value.windowOpen)
    }

    @Test fun `past exams are the closed ones, newest first`() = runTest(dispatcher) {
        routines.slotsResult = { _, _, _ ->
            ApiResponse.Success(listOf(
                slot(id = "old", date = "2026-11-03", status = SlotMyStatus.MISSED),
                slot(id = "new", date = "2026-11-09", status = SlotMyStatus.COMPLETED).copy(examId = "e9"),
                slot(id = "open-now", status = SlotMyStatus.OPEN),
            ))
        }
        val r = FetchPastSlotsUseCase(routines)("g1", now) as ApiResponse.Success
        assertEquals(listOf("new", "old"), r.data.map { it.id })
        assertEquals(Triple(null, "2026-11-10", "g1"), routines.slotsAsked.single())

        val vm = GroupPastExamsViewModel(
            "g1", FetchGroupUseCase(groups), FetchPastSlotsUseCase(routines), DescribeTopicGroupingUseCase(FakeQbCurriculumRepository()),
            DummyGroupExamStatsProvider(), testErrorMessages, now = { now },
        )
        advanceUntilIdle()
        assertEquals(listOf("new", "old"), vm.state.value.slots.map { it.id })
        assertEquals("only exams that exist have stats", setOf("new"), vm.state.value.stats.keys)
    }

    @Test fun `dummy stats are the same every time for an exam`() {
        val p = DummyGroupExamStatsProvider()
        assertEquals(p.statsFor("e1", 0, 40), p.statsFor("e1", 0, 40))
        assertEquals(10, p.statsFor("e1", 0, 40).histogram.size)
        assertEquals("real participants are kept", 12, p.statsFor("e1", 12, 40).submittedCount)
    }
}
