package com.prostuti.student.ui.screen.routine.builder

import androidx.lifecycle.SavedStateHandle
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.Weekday
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Chapter
import com.prostuti.student.data.model.response.RoutineWithSlots
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.model.response.Topic
import com.prostuti.student.data.repository.CurriculumRepository
import com.prostuti.student.data.repository.fake.FakeGroupRepository
import com.prostuti.student.data.repository.fake.FakeRoutineRepository
import com.prostuti.student.domain.FetchChaptersUseCase
import com.prostuti.student.domain.FetchSubjectsUseCase
import com.prostuti.student.domain.FetchTopicsUseCase
import com.prostuti.student.domain.group.FetchGroupUseCase
import com.prostuti.student.domain.routine.CreateAdhocExamUseCase
import com.prostuti.student.domain.routine.CreateRoutineUseCase
import com.prostuti.student.domain.routine.FetchActiveRoutineUseCase
import com.prostuti.student.domain.routine.PreviewRoutineUseCase
import com.prostuti.student.R
import com.prostuti.student.ui.screen.group.GroupNoticeBus
import com.prostuti.student.ui.screen.group.testErrorMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

/** Curriculum from [BuilderFixtures]: physics (ch1: t1 t2, ch2: t3) and chemistry (ch3: t4 t5). */
private class FixtureCurriculum : CurriculumRepository {
    private val groups = BuilderFixtures.groups
    override fun observeSubjects(refresh: Boolean): Flow<ApiResponse<List<Subject>>> = flowOf(ApiResponse.Success(groups.map {
        Subject(id = it.subjectId, nameBn = it.subjectNameBn, nameEn = it.subjectId, createdAt = "", createdBy = "")
    }))
    override fun observeChapters(subjectId: String, refresh: Boolean): Flow<ApiResponse<List<Chapter>>> =
        flowOf(ApiResponse.Success(groups.first { it.subjectId == subjectId }.chapters.map { it.chapter }))
    override fun observeTopics(chapterId: String, refresh: Boolean): Flow<ApiResponse<List<Topic>>> =
        flowOf(ApiResponse.Success(groups.flatMap { it.chapters }.first { it.chapter.id == chapterId }.topics))
}

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineBuilderViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val groups = FakeGroupRepository().apply { groupResult = ApiResponse.Success(FakeGroupRepository.group(role = GroupRole.ADMIN)) }
    private val routines = FakeRoutineRepository()
    private val bus = GroupNoticeBus()
    private val curriculum = FixtureCurriculum()
    /** 8 Nov 2026, 16:00 in Dhaka. */
    private val now = Instant.parse("2026-11-08T10:00:00Z")

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(
        owner: RoutineOwner = RoutineOwner.Group("g1"),
        mode: RoutineBuilderMode = RoutineBuilderMode.ROUTINE,
        saved: SavedStateHandle = SavedStateHandle(),
    ) = RoutineBuilderViewModel(
        owner, mode, saved,
        FetchSubjectsUseCase(curriculum), FetchChaptersUseCase(curriculum), FetchTopicsUseCase(curriculum),
        FetchGroupUseCase(groups), FetchActiveRoutineUseCase(routines), PreviewRoutineUseCase(routines),
        CreateRoutineUseCase(routines), CreateAdhocExamUseCase(routines), bus, testErrorMessages, now = { now },
    )

    /** Ticks both subjects and goes to step 2 (everything ticked). */
    private fun RoutineBuilderViewModel.pickBoth() {
        toggleSubject("phy")
        toggleSubject("chem")
        next()
    }

    @Test fun `a group routine goes through all four steps and is saved`() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        assertFalse(vm.state.value.isChecking)

        vm.pickBoth()
        advanceUntilIdle()
        assertEquals("starts with whole books", BuilderFixtures.allTopics, vm.state.value.selectedTopicIds)
        vm.toggleChapterAll("ch2")
        vm.next()
        assertEquals(BuilderStep.CONFIG, vm.state.value.step)
        vm.toggleWeekday(Weekday.MON)
        vm.next()
        advanceUntilIdle()

        assertEquals(BuilderStep.PREVIEW, vm.state.value.step)
        val sent = routines.previews.single()
        assertEquals("g1", sent.groupId)
        assertEquals(listOf("phy", "chem"), sent.subjectIds)
        assertEquals(listOf("ch1", "ch3"), sent.chapterIds)
        assertEquals(listOf(Weekday.MON, Weekday.WED, Weekday.SAT), sent.weekdays)
        assertEquals(19 * 60 to 23 * 60, sent.windowOpenMinute to sent.windowCloseMinute)
        assertEquals("2026-11-08", sent.startDate)
        assertEquals(0.25, sent.negativeMarkValue, 0.0)

        vm.next()
        advanceUntilIdle()
        assertEquals(sent, routines.creates.single())
        assertTrue(vm.state.value.isFinished)
        assertEquals(R.string.routine_created_group, bus.take("g1")?.res)
    }

    @Test fun `a personal routine has no group and the whole day`() = runTest(dispatcher) {
        val vm = vm(owner = RoutineOwner.Personal)
        advanceUntilIdle()
        assertFalse(vm.state.value.showsWindow)
        assertEquals(listOf<String?>(null), routines.activeAsked)

        vm.pickBoth()
        advanceUntilIdle()
        vm.next()
        vm.setNegativeMarking(false)
        vm.next()
        advanceUntilIdle()

        val sent = routines.previews.single()
        assertNull(sent.groupId)
        assertEquals(0 to 1440, sent.windowOpenMinute to sent.windowCloseMinute)
        assertEquals("negative off sends 0", 0.0, sent.negativeMarkValue, 0.0)
    }

    @Test fun `a one-off exam is saved from step 3 without a preview`() = runTest(dispatcher) {
        val vm = vm(mode = RoutineBuilderMode.ADHOC)
        advanceUntilIdle()
        assertEquals(3, vm.state.value.stepCount)
        assertTrue("an active routine doesn't block one-off exams", routines.activeAsked.isEmpty())

        vm.pickBoth()
        advanceUntilIdle()
        vm.next()
        vm.setExamDate(java.time.LocalDate.of(2026, 11, 12))
        vm.setTitle("  ")
        vm.next()
        advanceUntilIdle()

        assertTrue(routines.previews.isEmpty())
        val sent = routines.adhocs.single()
        assertEquals("2026-11-12", sent.examDate)
        assertNull("a blank title isn't sent", sent.title)
        assertEquals("g1", sent.groupId)
        assertTrue(vm.state.value.isFinished)
    }

    @Test fun `a one-off exam can't be sent for a window that already closed today`() = runTest(dispatcher) {
        val vm = vm(mode = RoutineBuilderMode.ADHOC)
        advanceUntilIdle()
        vm.pickBoth()
        advanceUntilIdle()
        vm.next()
        repeat(14) { vm.changeWindowClose(-1) }  // 23:00 → 16:00, which is now
        repeat(10) { vm.changeWindowOpen(-1) }   // 19:00 → 14:00

        assertTrue(RoutineBuilderForm.Problem.WINDOW_ALREADY_OVER in vm.state.value.formProblems)
        vm.next()
        advanceUntilIdle()
        assertTrue(routines.adhocs.isEmpty())
    }

    @Test fun `a non-admin is sent back`() = runTest(dispatcher) {
        groups.groupResult = ApiResponse.Success(FakeGroupRepository.group(role = GroupRole.MEMBER))
        val vm = vm()
        advanceUntilIdle()
        assertTrue(vm.state.value.isFinished)
    }

    @Test fun `an active routine blocks a new one`() = runTest(dispatcher) {
        routines.activeResult = ApiResponse.Success(RoutineWithSlots(FakeRoutineRepository.routine("g1")))
        val vm = vm()
        advanceUntilIdle()
        assertTrue(vm.state.value.routineExists)
    }

    @Test fun `a preview failure points to the step that fixes it`() = runTest(dispatcher) {
        routines.previewResult = ApiResponse.Failure("none", "NO_TOPICS_TO_SCHEDULE")
        val vm = vm()
        advanceUntilIdle()
        vm.pickBoth()
        advanceUntilIdle()
        vm.next()
        vm.next()
        advanceUntilIdle()

        val problem = vm.state.value.previewProblem!!
        assertEquals("mapped:NO_TOPICS_TO_SCHEDULE", problem.message)
        assertEquals(BuilderStep.CHAPTERS, problem.backTo)
        vm.goTo(problem.backTo)
        assertEquals(BuilderStep.CHAPTERS, vm.state.value.step)
        assertNull(vm.state.value.previewProblem)
    }

    @Test fun `ROUTINE_EXISTS on save shows the active-routine notice`() = runTest(dispatcher) {
        routines.createResult = ApiResponse.Failure("exists", "ROUTINE_EXISTS")
        val vm = vm()
        advanceUntilIdle()
        vm.pickBoth()
        advanceUntilIdle()
        vm.next()
        vm.next()
        advanceUntilIdle()
        vm.next()
        advanceUntilIdle()

        assertTrue(vm.state.value.routineExists)
        assertFalse(vm.state.value.isFinished)
    }

    @Test fun `back goes one step at a time and keeps the picks`() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.pickBoth()
        advanceUntilIdle()
        vm.toggleTopic("t1")

        assertTrue(vm.back())
        assertEquals(BuilderStep.SUBJECTS, vm.state.value.step)
        assertFalse("step 1 leaves the builder", vm.back())
        vm.next()
        advanceUntilIdle()
        assertFalse("same subjects: picks kept", "t1" in vm.state.value.selectedTopicIds)

        vm.back()
        vm.toggleSubject("chem")
        vm.next()
        advanceUntilIdle()
        assertEquals(setOf("t2", "t3"), vm.state.value.selectedTopicIds)
    }

    @Test fun `at most 20 subjects`() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        repeat(21) { vm.toggleSubject("s$it") }
        assertEquals(20, vm.state.value.selectedSubjectIds.size)
    }

    @Test fun `picks and form survive process death`() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val first = vm(saved = saved)
        advanceUntilIdle()
        first.pickBoth()
        advanceUntilIdle()
        first.toggleTopic("t5")
        first.next()
        first.changeQuestions(2)
        advanceUntilIdle()

        val restored = vm(saved = saved)
        advanceUntilIdle()

        assertEquals(BuilderStep.CONFIG, restored.state.value.step)
        assertEquals(35, restored.state.value.form.questionsCount)
        assertEquals(BuilderFixtures.allTopics - "t5", restored.state.value.selectedTopicIds)
    }
}
