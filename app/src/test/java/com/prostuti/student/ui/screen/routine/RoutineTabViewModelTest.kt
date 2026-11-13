package com.prostuti.student.ui.screen.routine

import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutineWithSlots
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest.Companion.defaultAttemptData
import com.prostuti.student.data.repository.fake.FakeGroupRepository
import com.prostuti.student.data.repository.fake.FakeGroupRepository.Companion.group
import com.prostuti.student.data.repository.fake.FakeQbCurriculumRepository
import com.prostuti.student.data.repository.fake.FakeRoutineRepository
import com.prostuti.student.data.repository.fake.FakeRoutineRepository.Companion.routine
import com.prostuti.student.data.repository.fake.FakeRoutineRepository.Companion.slot
import com.prostuti.student.domain.group.FetchMyGroupsUseCase
import com.prostuti.student.domain.routine.DeleteAdhocExamUseCase
import com.prostuti.student.domain.routine.DeleteRoutineUseCase
import com.prostuti.student.domain.routine.DescribeTopicGroupingUseCase
import com.prostuti.student.domain.routine.FetchActiveRoutineUseCase
import com.prostuti.student.domain.routine.FetchSlotsUseCase
import com.prostuti.student.domain.routine.OpenScheduledExamUseCase
import com.prostuti.student.domain.routine.RescheduleSlotUseCase
import com.prostuti.student.domain.routine.ScheduledExamOutcome
import com.prostuti.student.ui.screen.group.testErrorMessages
import com.prostuti.student.ui.screen.routine.slot.SlotAction
import io.mockk.coEvery
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

/** "Now" is Tue 10 Nov 2026, 20:00 Dhaka. The fake routine runs 8 Nov – 7 Dec. */
@OptIn(ExperimentalCoroutinesApi::class)
class RoutineTabViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val groups = FakeGroupRepository()
    private val routines = FakeRoutineRepository()
    private val opener = mockk<OpenScheduledExamUseCase>()
    private val now = Instant.parse("2026-11-10T14:00:00Z")

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm() = RoutineTabViewModel(
        FetchActiveRoutineUseCase(routines), FetchSlotsUseCase(routines), FetchMyGroupsUseCase(groups),
        DescribeTopicGroupingUseCase(FakeQbCurriculumRepository()), RescheduleSlotUseCase(routines), DeleteAdhocExamUseCase(routines),
        DeleteRoutineUseCase(routines), opener, testErrorMessages, now = { now },
    )

    private fun personalRoutine() = RoutineWithSlots(
        routine(), slots = listOf(
            slot(id = "tue", groupId = null, status = SlotMyStatus.OPEN),
            slot(id = "fri", groupId = null, date = "2026-11-13"),
            slot(id = "next-week", groupId = null, date = "2026-11-18"),
        )
    )

    @Test fun `only a pull shows the refresh indicator, other reloads stay quiet`() = runTest(dispatcher) {
        routines.activeResult = ApiResponse.Success(personalRoutine())
        val vm = vm()
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

    @Test fun `no personal routine offers to create one`() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        assertTrue(vm.state.value.routineKnown)
        assertNull(vm.state.value.routine)
        assertEquals(listOf<String?>(null), routines.activeAsked)
    }

    @Test fun `personal routine shows the week, today and what comes next`() = runTest(dispatcher) {
        routines.activeResult = ApiResponse.Success(personalRoutine())
        val vm = vm()
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(LocalDate.of(2026, 11, 9), s.weekStart)
        assertEquals(listOf("tue"), s.slotsOn(s.selectedDate).map { it.id })
        assertEquals(listOf("tue", "fri"), s.weekSlots.map { it.id })
        assertEquals("D8: Wednesday points to Friday", "fri", s.nextExamAfter(LocalDate.of(2026, 11, 11))?.id)
        assertEquals("10 Nov to 7 Dec, both counted", 28L, s.daysLeft)
        assertTrue(s.canGoBack)
        assertTrue("routine weeks need no extra call", routines.slotsAsked.isEmpty())

        vm.showWeek(forward = true)
        assertEquals(listOf("next-week"), vm.state.value.weekSlots.map { it.id })
    }

    @Test fun `group mode lists my groups' exams only, filterable`() = runTest(dispatcher) {
        groups.myGroupsPages = mapOf(1 to ApiResponse.Success(listOf(group(id = "g1", role = GroupRole.ADMIN), group(id = "g2"))))
        routines.slotsResult = { from, _, _ ->
            if (from == "2026-11-09") ApiResponse.Success(listOf(slot(id = "a", groupId = "g1"), slot(id = "mine", groupId = null)))
            else ApiResponse.Success(listOf(slot(id = "later", groupId = "g2", date = "2026-11-17")))
        }
        val vm = vm()
        advanceUntilIdle()
        vm.selectMode(RoutineTabMode.GROUP)
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(listOf("a"), s.weekSlots.map { it.id })
        assertEquals("the week after, for a quiet day", "later", s.nextExamAfter(LocalDate.of(2026, 11, 11))?.id)
        assertTrue(s.canManage(s.weekSlots.single()))
        assertFalse(s.canManage(slot(id = "x", groupId = "g2")))
        assertTrue("my own exam is mine to manage", s.canManage(slot(id = "y", groupId = null)))

        vm.selectGroup("g2")
        advanceUntilIdle()
        assertEquals("g2", routines.slotsAsked.last().third)
    }

    @Test fun `switching modes keeps each mode's data instead of calling again`() = runTest(dispatcher) {
        routines.activeResult = ApiResponse.Success(personalRoutine())
        groups.myGroupsPages = mapOf(1 to ApiResponse.Success(listOf(group(id = "g1"))))
        routines.slotsResult = { _, _, _ -> ApiResponse.Success(listOf(slot(id = "a", groupId = "g1"))) }
        val vm = vm()
        advanceUntilIdle()
        vm.selectMode(RoutineTabMode.GROUP)
        advanceUntilIdle()
        val activeCalls = routines.activeAsked.size
        val slotCalls = routines.slotsAsked.size

        vm.selectMode(RoutineTabMode.PERSONAL)
        advanceUntilIdle()
        assertEquals("personal routine shown from memory", activeCalls, routines.activeAsked.size)
        assertEquals(listOf("tue", "fri"), vm.state.value.weekSlots.map { it.id })

        vm.selectMode(RoutineTabMode.GROUP)
        advanceUntilIdle()
        assertEquals("group week shown from memory", slotCalls, routines.slotsAsked.size)
        assertEquals(listOf("a"), vm.state.value.weekSlots.map { it.id })

        vm.pullToRefresh()
        advanceUntilIdle()
        assertTrue("a pull still asks the server", routines.slotsAsked.size > slotCalls)
    }

    @Test fun `a group week left on another week loads this week on return`() = runTest(dispatcher) {
        groups.myGroupsPages = mapOf(1 to ApiResponse.Success(listOf(group(id = "g1"))))
        val vm = vm()
        advanceUntilIdle()
        vm.selectMode(RoutineTabMode.GROUP)
        advanceUntilIdle()
        vm.showWeek(forward = true)
        advanceUntilIdle()
        vm.selectMode(RoutineTabMode.PERSONAL)
        advanceUntilIdle()
        val slotCalls = routines.slotsAsked.size

        vm.selectMode(RoutineTabMode.GROUP)
        advanceUntilIdle()
        assertTrue("the loaded week was a different one", routines.slotsAsked.size > slotCalls)
        assertEquals("2026-11-09", routines.slotsAsked[slotCalls].first)
    }

    @Test fun `opening the screen loads once, every later return reloads quietly`() = runTest(dispatcher) {
        val vm = vm()
        vm.onReturn()   // the screen's first composition
        advanceUntilIdle()
        assertEquals("one call on open", 1, routines.activeAsked.size)

        vm.onReturn()   // back from another tab
        advanceUntilIdle()
        assertEquals(2, routines.activeAsked.size)
        assertFalse("no pull indicator for it", vm.state.value.isRefreshing)
    }

    @Test fun `a group joined elsewhere shows after coming back`() = runTest(dispatcher) {
        groups.myGroupsPages = mapOf(1 to ApiResponse.Success(listOf(group(id = "g1"))))
        val vm = vm()
        vm.onReturn()
        advanceUntilIdle()
        vm.selectMode(RoutineTabMode.GROUP)
        advanceUntilIdle()
        vm.selectMode(RoutineTabMode.PERSONAL)
        advanceUntilIdle()

        // Away on another tab, the user joins g2, then comes back on the personal mode.
        groups.myGroupsPages = mapOf(1 to ApiResponse.Success(listOf(group(id = "g1"), group(id = "g2"))))
        vm.onReturn()
        advanceUntilIdle()
        vm.selectMode(RoutineTabMode.GROUP)
        advanceUntilIdle()
        assertEquals(listOf("g1", "g2"), vm.state.value.groups.map { it.id })

        groups.myGroupsPages = mapOf(1 to ApiResponse.Success(listOf(group(id = "g1"), group(id = "g2"), group(id = "g3"))))
        vm.pullToRefresh()
        advanceUntilIdle()
        assertEquals("a pull reloads my groups too", 3, vm.state.value.groups.size)
    }

    @Test fun `a personal exam's result isn't locked, a group one's is`() = runTest(dispatcher) {
        routines.activeResult = ApiResponse.Success(personalRoutine())
        coEvery { opener.take(any(), any()) } returns ApiResponse.Success(ScheduledExamOutcome.Attempt(defaultAttemptData(), "2026-11-10T17:00:00"))
        val vm = vm()
        advanceUntilIdle()

        vm.openExam("tue", SlotAction.TAKE)
        advanceUntilIdle()

        assertEquals(null, (vm.state.value.examLaunch?.outcome as ScheduledExamOutcome.Attempt).lockResultUntil)
    }

    @Test fun `deleting the personal routine says how many exams went`() = runTest(dispatcher) {
        routines.activeResult = ApiResponse.Success(personalRoutine())
        val vm = vm()
        advanceUntilIdle()
        routines.activeResult = ApiResponse.Success(null)

        vm.deleteRoutine()
        advanceUntilIdle()

        assertEquals(listOf("r1"), routines.deletedRoutines)
        assertNull(vm.state.value.routine)
        assertEquals("3", vm.state.value.notice?.arg)
    }
}
