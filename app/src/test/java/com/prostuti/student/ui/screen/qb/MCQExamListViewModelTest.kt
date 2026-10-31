package com.prostuti.student.ui.screen.qb

import com.prostuti.student.data.model.enums.ExamTypes
import com.prostuti.student.data.model.enums.Level
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.model.response.MCQExam
import com.prostuti.student.data.model.response.MetaData
import com.prostuti.student.data.repository.fake.FakeQbMcqExamRepository
import com.prostuti.student.domain.StartOrResumeExamAttemptUseCase
import com.prostuti.student.domain.FetchMCQExamsUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class MCQExamListViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun fakeExam(id: String, examType: ExamTypes = ExamTypes.BOARD) = MCQExam(
        id = id, duration = 60, level = Level.ACADEMIC, examType = examType, examYear = 2024,
        markPerQuestion = 1.0, negativeMarkPerWrongAnswer = 0.25, questionCount = 10,
        showExplanationAfter = "submission", shuffleOptions = false, shuffleQuestions = false,
        titleBn = "mock-$id", titleEn = "mock-$id"
    )

    private fun fakeAttempt(examId: String) = ExamAttemptData(
        attemptId = "attempt-$examId", duration = 60, examId = examId, questions = emptyList(), startAt = "2025-01-01"
    )

    private fun viewModel(repo: FakeQbMcqExamRepository, subjectId: String = "phy") =
        MCQExamListViewModel(subjectId, FetchMCQExamsUseCase(repo), StartOrResumeExamAttemptUseCase(repo))

    @Test
    fun `init loads both board and college exams`() = runTest {
        val repo = FakeQbMcqExamRepository(
            fetchResult = { type -> ApiResponse.Success(listOf(fakeExam("${type.name.lowercase()}1", type))) }
        )
        val vm = viewModel(repo)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(1, state.boardExams.size)
        assertEquals(1, state.collegeExams.size)
        assertEquals(2, repo.fetchCallCount)
    }

    @Test
    fun `load failure populates both BOARD_LOAD and COLLEGE_LOAD error tags`() = runTest {
        val repo = FakeQbMcqExamRepository(fetchResult = { ApiResponse.Failure("লোড ব্যর্থ", "LOAD_FAIL") })
        val vm = viewModel(repo)
        advanceUntilIdle()

        assertEquals("লোড ব্যর্থ", vm.state.value.errors["BOARD_LOAD"])
        assertEquals("লোড ব্যর্থ", vm.state.value.errors["COLLEGE_LOAD"])
    }

    @Test
    fun `loadMoreBoard appends page 2 while hasMore, then stops once exhausted`() = runTest {
        var boardCalls = 0
        val repo = FakeQbMcqExamRepository(fetchResult = { type ->
            if (type == ExamTypes.BOARD) {
                boardCalls++
                ApiResponse.Success(
                    data = listOf(fakeExam("b$boardCalls")),
                    metaData = MetaData(currentPage = boardCalls, pageLimit = 1, totalItems = 2, totalPages = 2)
                )
            } else {
                ApiResponse.Success(emptyList())
            }
        })
        val vm = viewModel(repo)
        advanceUntilIdle()

        assertEquals(1, vm.state.value.boardExams.size)
        assertTrue(vm.state.value.boardHasMore)

        vm.loadMoreBoard()
        advanceUntilIdle()
        assertEquals(2, vm.state.value.boardExams.size)
        assertFalse(vm.state.value.boardHasMore)

        vm.loadMoreBoard() // hasMore is now false — must no-op
        advanceUntilIdle()
        assertEquals(2, vm.state.value.boardExams.size)
        assertEquals(2, boardCalls)
    }

    @Test
    fun `startExam success stores the attempt pair for navigation`() = runTest {
        val exam = fakeExam("e1")
        val repo = FakeQbMcqExamRepository(attemptResult = ApiResponse.Success(fakeAttempt("e1")))
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.startExam(exam)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isAttempting)
        assertEquals("attempt-e1", state.navigateToExam?.first?.attemptId)
        assertEquals(exam, state.navigateToExam?.second)
    }

    @Test
    fun `startExam failure populates ATTEMPT error tag and clears isAttempting`() = runTest {
        val repo = FakeQbMcqExamRepository(attemptResult = ApiResponse.Failure("শুরু করা যায়নি", "ATTEMPT_FAIL"))
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.startExam(fakeExam("e1"))
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isAttempting)
        assertNull(state.navigateToExam)
        assertEquals("শুরু করা যায়নি", state.errors["ATTEMPT"])
    }

    @Test
    fun `consumeNavigation clears the pending navigation pair`() = runTest {
        val repo = FakeQbMcqExamRepository(attemptResult = ApiResponse.Success(fakeAttempt("e1")))
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.startExam(fakeExam("e1"))
        advanceUntilIdle()

        vm.consumeNavigation()
        assertNull(vm.state.value.navigateToExam)
    }

    @Test
    fun `selectTab updateSearch and selectExam update state directly`() = runTest {
        val repo = FakeQbMcqExamRepository()
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.selectTab(MCQListTab.COLLEGE)
        assertEquals(MCQListTab.COLLEGE, vm.state.value.selectedTab)

        vm.updateSearch("পদার্থ")
        assertEquals("পদার্থ", vm.state.value.searchQuery)

        val exam = fakeExam("e9")
        vm.selectExam(exam)
        assertEquals(exam, vm.state.value.selectedExam)
    }

    @Test
    fun `clearError removes only the given tag`() = runTest {
        val repo = FakeQbMcqExamRepository(fetchResult = { ApiResponse.Failure("err", "FAIL") })
        val vm = viewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.state.value.errors.containsKey("BOARD_LOAD"))

        vm.clearError("BOARD_LOAD")
        assertFalse(vm.state.value.errors.containsKey("BOARD_LOAD"))
        assertTrue(vm.state.value.errors.containsKey("COLLEGE_LOAD"))
    }
}
