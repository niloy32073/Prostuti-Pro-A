package com.prostuti.student.ui.screen.examengine

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.domain.GetAttemptReviewUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Fakes return immediately, so the init load settles inside construction; tests assert the
 * settled state rather than the conflated transient isLoading=true emission. */
@OptIn(ExperimentalCoroutinesApi::class)
class ExamAnswerSheetViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeExamAttemptRepositoryForEngineTest

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeExamAttemptRepositoryForEngineTest()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() =
        ExamAnswerSheetViewModel(attemptId = "attempt1", getAttemptReviewUseCase = GetAttemptReviewUseCase(fakeRepo))

    @Test
    fun `load success sorts questions by order and exposes correct wrong skipped counts`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        val loaded = viewModel.state.value
        assertEquals(false, loaded.isLoading)
        assertEquals(listOf("mcq1", "mcq2", "mcq3"), loaded.questions.map { it.mcqId })
        assertEquals(1, loaded.correctCount) // mcq1
        assertEquals(1, loaded.wrongCount)    // mcq2
        assertEquals(1, loaded.skippedCount)  // mcq3 (selectedOptionId == null)
    }

    @Test
    fun `load failure populates LOAD error`() = runTest {
        fakeRepo.getAttemptReviewResult = ApiResponse.Failure(message = "Review unavailable", errorCode = "403")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        val errorState = viewModel.state.value
        assertEquals(false, errorState.isLoading)
        assertEquals("Review unavailable", errorState.errors["LOAD"])
    }

    @Test
    fun `setFilter CORRECT narrows filteredQuestions and collapses any expanded card`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.toggleExpanded("mcq1")
        assertEquals("mcq1", viewModel.state.value.expandedMcqId)

        viewModel.setFilter(AnswerSheetFilter.CORRECT)

        val state = viewModel.state.value
        assertEquals(AnswerSheetFilter.CORRECT, state.filter)
        assertEquals(listOf("mcq1"), state.filteredQuestions.map { it.mcqId })
        assertNull(state.expandedMcqId)
    }

    @Test
    fun `setFilter WRONG and SKIPPED each narrow to the right subset`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.setFilter(AnswerSheetFilter.WRONG)
        assertEquals(listOf("mcq2"), viewModel.state.value.filteredQuestions.map { it.mcqId })

        viewModel.setFilter(AnswerSheetFilter.SKIPPED)
        assertEquals(listOf("mcq3"), viewModel.state.value.filteredQuestions.map { it.mcqId })

        viewModel.setFilter(AnswerSheetFilter.ALL)
        assertEquals(listOf("mcq1", "mcq2", "mcq3"), viewModel.state.value.filteredQuestions.map { it.mcqId })
    }

    @Test
    fun `toggleExpanded opens then collapses the same card`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.toggleExpanded("mcq2")
        assertEquals("mcq2", viewModel.state.value.expandedMcqId)

        viewModel.toggleExpanded("mcq2")
        assertNull(viewModel.state.value.expandedMcqId)
    }

    @Test
    fun `clearError removes only the targeted tag`() = runTest {
        fakeRepo.getAttemptReviewResult = ApiResponse.Failure(message = "boom", errorCode = "500")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals("boom", viewModel.state.value.errors["LOAD"])
        viewModel.clearError("LOAD")
        assertNull(viewModel.state.value.errors["LOAD"])
    }
}
