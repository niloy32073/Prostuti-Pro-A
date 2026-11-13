package com.prostuti.student.ui.screen.group

import com.prostuti.student.ui.screen.routine.DayMark
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.SlotKind
import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutineWithSlots
import com.prostuti.student.data.repository.fake.FakeGroupRepository
import com.prostuti.student.data.repository.fake.FakeGroupRepository.Companion.group
import com.prostuti.student.data.repository.fake.FakeQbCurriculumRepository
import com.prostuti.student.data.repository.fake.FakeRoutineRepository
import com.prostuti.student.data.repository.fake.FakeRoutineRepository.Companion.routine
import com.prostuti.student.data.repository.fake.FakeRoutineRepository.Companion.slot
import com.prostuti.student.domain.group.CreateGroupUseCase
import com.prostuti.student.domain.group.FetchGroupMembersUseCase
import com.prostuti.student.domain.group.FetchGroupUseCase
import com.prostuti.student.domain.group.FetchJoinRequestsUseCase
import com.prostuti.student.domain.group.FetchMyGroupsUseCase
import com.prostuti.student.domain.group.JoinGroupByCodeUseCase
import com.prostuti.student.domain.group.LeaveGroupUseCase
import com.prostuti.student.domain.routine.DeleteAdhocExamUseCase
import com.prostuti.student.domain.routine.DeleteRoutineUseCase
import com.prostuti.student.domain.routine.DescribeTopicGroupingUseCase
import com.prostuti.student.domain.routine.FetchActiveRoutineUseCase
import com.prostuti.student.domain.routine.FetchSlotsUseCase
import com.prostuti.student.domain.routine.RescheduleSlotUseCase
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.ui.screen.home.HomeGroupExamViewModel
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest.Companion.defaultAttemptData
import com.prostuti.student.domain.routine.OpenScheduledExamUseCase
import com.prostuti.student.domain.routine.ScheduledExamOutcome
import com.prostuti.student.ui.screen.routine.slot.ExamLaunch
import com.prostuti.student.ui.screen.routine.slot.SlotAction
import com.prostuti.student.data.repository.DummyGroupExamStatsProvider
import com.prostuti.student.domain.routine.FetchPastSlotsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import java.time.LocalDate

/** GroupDetail's exam section, the routine calendar, GroupList's badges and Home's group card. "Now" is 10 Nov, 20:00 Dhaka. */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupScheduleViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val groups = FakeGroupRepository()
    private val routines = FakeRoutineRepository()
    private val describe = DescribeTopicGroupingUseCase(FakeQbCurriculumRepository())
    private val now = Instant.parse("2026-11-10T14:00:00Z")
    private val opener = mockk<OpenScheduledExamUseCase>()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun detailVm() = GroupDetailViewModel(
        "g1", FetchGroupUseCase(groups), FetchGroupMembersUseCase(groups), FetchJoinRequestsUseCase(groups), LeaveGroupUseCase(groups),
        FetchActiveRoutineUseCase(routines), FetchSlotsUseCase(routines), describe, RescheduleSlotUseCase(routines),
        DeleteAdhocExamUseCase(routines), opener, FetchPastSlotsUseCase(routines), DummyGroupExamStatsProvider(), GroupNoticeBus(), testErrorMessages, now = { now },
    )

    private fun calendarVm() = GroupRoutineCalendarViewModel(
        "g1", FetchGroupUseCase(groups), FetchActiveRoutineUseCase(routines), FetchSlotsUseCase(routines), describe,
        RescheduleSlotUseCase(routines), DeleteAdhocExamUseCase(routines), DeleteRoutineUseCase(routines), opener, testErrorMessages, now = { now },
    )

    // ── GroupDetail ──

    @Test fun `detail splits today's exams from the next one`() = runTest(dispatcher) {
        groups.groupResult = ApiResponse.Success(group(role = GroupRole.MEMBER))
        routines.slotsResult = { _, _, _ ->
            ApiResponse.Success(listOf(slot(id = "later", date = "2026-11-12"), slot(id = "today", status = SlotMyStatus.OPEN), slot(id = "today2", kind = SlotKind.ROUTINE)))
        }
        val vm = detailVm()
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(listOf("today", "today2"), s.todaySlots.map { it.id })
        assertEquals("later", s.nextSlot?.id)
        assertEquals(TopicLabel.Count(1), s.labels["today"])
        assertEquals("upcoming call (the other is past exams)", Triple("2026-11-10", null, "g1"), routines.slotsAsked.single { it.first != null })
        assertEquals("members ask for the routine too", listOf<String?>("g1"), routines.activeAsked)
    }

    @Test fun `an admin may create a routine only when none is active`() = runTest(dispatcher) {
        groups.groupResult = ApiResponse.Success(group(role = GroupRole.ADMIN))
        val vm = detailVm()
        advanceUntilIdle()
        assertEquals(false, vm.state.value.hasActiveRoutine)
        assertFalse(vm.state.value.hasSchedule)

        routines.activeResult = ApiResponse.Success(RoutineWithSlots(routine("g1")))
        vm.refresh()
        advanceUntilIdle()
        assertEquals(true, vm.state.value.hasActiveRoutine)
        assertTrue(vm.state.value.hasSchedule)
        assertEquals(LocalDate.of(2026, 12, 7), vm.state.value.routineLastDate)
    }

    @Test fun `moving an exam closes the sheet, says so and reloads`() = runTest(dispatcher) {
        groups.groupResult = ApiResponse.Success(group(role = GroupRole.ADMIN))
        routines.slotsResult = { _, _, _ -> ApiResponse.Success(listOf(slot(id = "s1"))) }
        val vm = detailVm()
        advanceUntilIdle()
        vm.openSlot("s1")
        assertEquals("s1", vm.state.value.openSlot?.id)

        vm.reschedule(LocalDate.of(2026, 11, 14))
        advanceUntilIdle()

        assertEquals(listOf("s1" to "2026-11-14"), routines.rescheduled)
        assertNull(vm.state.value.openSlotId)
        assertEquals(R.string.slot_rescheduled, vm.state.value.notice?.res)
        assertEquals("reloaded", 2, routines.slotsAsked.count { it.first != null })
    }

    @Test fun `a refused delete keeps the sheet open with the reason`() = runTest(dispatcher) {
        groups.groupResult = ApiResponse.Success(group(role = GroupRole.ADMIN))
        routines.slotsResult = { _, _, _ -> ApiResponse.Success(listOf(slot(id = "s1"))) }
        routines.deleteAdhocResult = ApiResponse.Failure("taken", "SLOT_HAS_ATTEMPTS")
        val vm = detailVm()
        advanceUntilIdle()
        vm.openSlot("s1")

        vm.deleteSlot()
        advanceUntilIdle()

        assertEquals("s1", vm.state.value.openSlotId)
        assertTrue("mapped:SLOT_HAS_ATTEMPTS" in vm.state.value.errors.values)
    }

    @Test fun `an exam button opens the engine once`() = runTest(dispatcher) {
        routines.slotsResult = { _, _, _ -> ApiResponse.Success(listOf(slot(id = "s1", status = SlotMyStatus.OPEN))) }
        val outcome = ScheduledExamOutcome.Attempt(defaultAttemptData(), "2026-11-10T17:00:00")
        coEvery { opener.take(any(), any()) } returns ApiResponse.Success(outcome)
        val vm = detailVm()
        advanceUntilIdle()

        vm.openExam("s1", SlotAction.TAKE)
        assertEquals("busy at once", "s1", vm.state.value.busyExamSlotId)
        vm.openExam("s1", SlotAction.TAKE)
        advanceUntilIdle()

        assertEquals(ExamLaunch("s1", outcome), vm.state.value.examLaunch)
        assertNull(vm.state.value.busyExamSlotId)
        coVerify(exactly = 1) { opener.take(any(), now) }
        vm.consumeExamLaunch()
        assertNull(vm.state.value.examLaunch)
    }

    @Test fun `a refused exam button shows why and reloads`() = runTest(dispatcher) {
        routines.slotsResult = { _, _, _ -> ApiResponse.Success(listOf(slot(id = "s1", status = SlotMyStatus.COMPLETED))) }
        coEvery { opener.viewResult(any(), any()) } returns ApiResponse.Failure("none", "NO_ATTEMPT")
        val vm = detailVm()
        advanceUntilIdle()

        vm.openExam("s1", SlotAction.VIEW_RESULT)
        advanceUntilIdle()

        assertTrue("mapped:NO_ATTEMPT" in vm.state.value.errors.values)
        assertNull(vm.state.value.examLaunch)
        assertEquals("reloaded", 2, routines.slotsAsked.count { it.first != null })
    }

    @Test fun `detail previews the last three ended exams, newest first`() = runTest(dispatcher) {
        routines.slotsResult = { from, to, _ ->
            if (from == null && to == "2026-11-10") ApiResponse.Success(
                listOf("2026-11-05", "2026-11-06", "2026-11-07", "2026-11-08").map { d -> slot(id = d, date = d, status = SlotMyStatus.COMPLETED).copy(examId = "e$d") } +
                    slot(id = "today-open", status = SlotMyStatus.OPEN)
            ) else ApiResponse.Success(emptyList())
        }
        val vm = detailVm()
        advanceUntilIdle()

        assertEquals(listOf("2026-11-08", "2026-11-07", "2026-11-06"), vm.state.value.pastSlots.map { it.id })
        assertEquals("dummy stats for each past exam", 3, vm.state.value.pastStats.size)
    }

    @Test fun `a non-member sees the group without member or exam errors`() = runTest(dispatcher) {
        groups.groupResult = ApiResponse.Success(group(role = null, isPublic = true))
        groups.membersPages = mapOf(1 to ApiResponse.Failure("not a member", "GROUP_NOT_FOUND"))
        routines.slotsResult = { _, _, _ -> ApiResponse.Failure("not a member", "GROUP_NOT_FOUND") }
        val vm = detailVm()
        advanceUntilIdle()

        assertEquals("g1", vm.state.value.group?.id)
        assertTrue(vm.state.value.errors.isEmpty())
        assertFalse(vm.state.value.isGone)
    }

    // ── Calendar ──

    @Test fun `calendar loads once on open and reloads on every later return`() = runTest(dispatcher) {
        val vm = calendarVm()
        vm.onReturn()
        advanceUntilIdle()
        val calls = routines.slotsAsked.size

        vm.onReturn()
        advanceUntilIdle()
        assertTrue(routines.slotsAsked.size > calls)
        assertFalse("no pull indicator for it", vm.state.value.isRefreshing)
    }

    @Test fun `calendar shows the pull indicator only for a pull`() = runTest(dispatcher) {
        val vm = calendarVm()
        advanceUntilIdle()
        val seen = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { seen += it.isRefreshing } }

        vm.refresh()
        advanceUntilIdle()
        assertFalse("a background reload is not a pull", true in seen)

        vm.pullToRefresh()
        advanceUntilIdle()
        assertTrue(true in seen)
        assertFalse("the indicator goes away when the load ends", vm.state.value.isRefreshing)
    }

    @Test fun `calendar shows this week, Monday first, with today chosen`() = runTest(dispatcher) {
        routines.slotsResult = { _, _, _ -> ApiResponse.Success(listOf(slot(id = "tue"), slot(id = "thu", date = "2026-11-12"))) }
        val vm = calendarVm()
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(LocalDate.of(2026, 11, 9), s.weekStart)
        assertEquals(LocalDate.of(2026, 11, 10), s.selectedDate)
        assertEquals(Triple("2026-11-09", "2026-11-15", "g1"), routines.slotsAsked.last())
        assertEquals(listOf("tue"), s.selectedSlots.map { it.id })
        assertEquals(DayMark.EXAM, s.markOf(LocalDate.of(2026, 11, 12), now))
        assertEquals(DayMark.NONE, s.markOf(LocalDate.of(2026, 11, 11), now))
    }

    @Test fun `a day without an exam points to the next one (D8)`() = runTest(dispatcher) {
        routines.slotsResult = { _, _, _ -> ApiResponse.Success(listOf(slot(id = "thu", date = "2026-11-12"))) }
        routines.activeResult = ApiResponse.Success(RoutineWithSlots(routine("g1"), slots = listOf(slot(id = "next-week", date = "2026-11-18"))))
        val vm = calendarVm()
        advanceUntilIdle()

        vm.selectDate(LocalDate.of(2026, 11, 11))
        assertEquals("thu", vm.state.value.nextExamAfter(LocalDate.of(2026, 11, 11))?.id)
        assertEquals("next-week", vm.state.value.nextExamAfter(LocalDate.of(2026, 11, 13))?.id)
        assertTrue(vm.state.value.isInRoutine(LocalDate.of(2026, 11, 11)))
        assertFalse(vm.state.value.isInRoutine(LocalDate.of(2026, 12, 8)))
    }

    @Test fun `weeks stay inside the routine`() = runTest(dispatcher) {
        routines.activeResult = ApiResponse.Success(RoutineWithSlots(routine("g1")))  // 8 Nov – 7 Dec
        val vm = calendarVm()
        advanceUntilIdle()

        assertTrue("8 Nov is in the week before", vm.state.value.canGoBack)
        vm.showWeek(forward = false)
        advanceUntilIdle()
        assertEquals(LocalDate.of(2026, 11, 2), vm.state.value.weekStart)
        assertFalse(vm.state.value.canGoBack)
        assertEquals("same weekday stays chosen", LocalDate.of(2026, 11, 3), vm.state.value.selectedDate)
        assertEquals(Triple("2026-11-02", "2026-11-08", "g1"), routines.slotsAsked.last())

        repeat(6) { vm.showWeek(forward = true) }
        assertEquals(LocalDate.of(2026, 12, 7), vm.state.value.weekStart)
        assertFalse(vm.state.value.canGoForward)
    }

    @Test fun `deleting the routine says how many exams went`() = runTest(dispatcher) {
        groups.groupResult = ApiResponse.Success(group(role = GroupRole.ADMIN))
        routines.activeResult = ApiResponse.Success(RoutineWithSlots(routine("g1")))
        val vm = calendarVm()
        advanceUntilIdle()
        routines.activeResult = ApiResponse.Success(null)

        vm.deleteRoutine()
        advanceUntilIdle()

        assertEquals(listOf("r1"), routines.deletedRoutines)
        assertNull(vm.state.value.routine)
        assertEquals(R.string.calendar_routine_deleted, vm.state.value.notice?.res)
        assertEquals("3", vm.state.value.notice?.arg)
    }

    @Test fun `a vanished group closes the calendar`() = runTest(dispatcher) {
        groups.groupResult = ApiResponse.Failure("gone", "GROUP_NOT_FOUND")
        val vm = calendarVm()
        advanceUntilIdle()
        assertTrue(vm.state.value.isGone)
    }

    // ── GroupList ──

    @Test fun `group list matches today's slots to their groups`() = runTest(dispatcher) {
        groups.myGroupsPages = mapOf(1 to ApiResponse.Success(listOf(group(id = "g1"), group(id = "g2"))))
        routines.slotsResult = { _, _, _ ->
            ApiResponse.Success(listOf(slot(id = "a", groupId = "g1"), slot(id = "b", groupId = "g1"), slot(id = "mine", groupId = null)))
        }
        val vm = GroupListViewModel(
            FetchMyGroupsUseCase(groups), CreateGroupUseCase(groups), JoinGroupByCodeUseCase(groups), FetchSlotsUseCase(routines),
            testErrorMessages, now = { now },
        )
        advanceUntilIdle()

        assertEquals(listOf("a", "b"), vm.state.value.todaySlots["g1"]?.map { it.id })
        assertNull(vm.state.value.todaySlots["g2"])
        assertEquals("a personal slot isn't a group's", setOf("g1"), vm.state.value.todaySlots.keys)
        assertEquals(Triple("2026-11-10", "2026-11-10", null), routines.slotsAsked.single())
    }

    // ── Home ──

    @Test fun `home shows today's open group exam with its group's name`() = runTest(dispatcher) {
        groups.groupResult = ApiResponse.Success(group(id = "g2").copy(name = "Physics club"))
        routines.slotsResult = { _, _, _ ->
            ApiResponse.Success(listOf(slot(id = "up", groupId = "g1", opensAt = "2026-11-10T16:00:00"), slot(id = "open", groupId = "g2", status = SlotMyStatus.OPEN)))
        }
        val vm = HomeGroupExamViewModel(FetchSlotsUseCase(routines), FetchGroupUseCase(groups), now = { now })
        vm.refresh()
        advanceUntilIdle()

        assertEquals("open", vm.state.value.exam?.slot?.id)
        assertEquals("Physics club", vm.state.value.exam?.groupName)
    }

    @Test fun `home hides the card when there is no group exam today`() = runTest(dispatcher) {
        routines.slotsResult = { _, _, _ -> ApiResponse.Success(listOf(slot(id = "mine", groupId = null))) }
        val vm = HomeGroupExamViewModel(FetchSlotsUseCase(routines), FetchGroupUseCase(groups), now = { now })
        vm.refresh()
        advanceUntilIdle()
        assertNull(vm.state.value.exam)
    }
}
