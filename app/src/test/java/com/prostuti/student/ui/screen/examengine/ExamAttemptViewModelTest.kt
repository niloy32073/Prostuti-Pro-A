package com.prostuti.student.ui.screen.examengine

import app.cash.turbine.test
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest.Companion.defaultAttemptData
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest.Companion.fakeQuestion
import com.prostuti.student.domain.FetchPassageUseCase
import com.prostuti.student.domain.GetAttemptQuestionsUseCase
import com.prostuti.student.domain.SubmitExamAttemptUseCase
import com.prostuti.student.domain.SubmitExamResponsesUseCase
import com.prostuti.student.domain.SubmitMcqReportUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/** Fakes return immediately, so the init load settles inside construction; tests assert the
 * settled state rather than the conflated transient emissions. */
@OptIn(ExperimentalCoroutinesApi::class)
class ExamAttemptViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeExamAttemptRepositoryForEngineTest

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        ExamAttemptStore.consume() // defensively drain any leftover stash from a previous test
        fakeRepo = FakeExamAttemptRepositoryForEngineTest()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(attemptId: String = "attempt1") = ExamAttemptViewModel(
        attemptId = attemptId,
        fetchPassageUseCase = FetchPassageUseCase(fakeRepo),
        getAttemptQuestionsUseCase = GetAttemptQuestionsUseCase(fakeRepo),
        submitExamResponsesUseCase = SubmitExamResponsesUseCase(fakeRepo),
        submitExamAttemptUseCase = SubmitExamAttemptUseCase(fakeRepo),
        submitMcqReportUseCase = SubmitMcqReportUseCase(fakeRepo)
    )

    @Test
    fun `resumeAttempt success loads questions and duration when nothing is stashed`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Success(defaultAttemptData(questions = listOf(fakeQuestion())))
        val viewModel = buildViewModel()
        advanceUntilIdle()

        val loaded = viewModel.state.value
        assertEquals(1, loaded.questions.size)
        assertEquals(600, loaded.totalDurationSeconds) // duration=10min -> 600s
        assertEquals(600, loaded.timeRemainingSeconds)
        assertEquals(1, fakeRepo.getAttemptQuestionsCallCount)
    }

    @Test
    fun `resumeAttempt failure populates RESUME error`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Failure(message = "Attempt not found", errorCode = "404")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals("Attempt not found", viewModel.state.value.errors["RESUME"])
    }

    @Test
    fun `stashed attempt data from ExamAttemptStore is applied without calling the repository`() = runTest {
        ExamAttemptStore.put(defaultAttemptData(questions = listOf(fakeQuestion(mcqId = "stashed"))))
        val viewModel = buildViewModel()

        viewModel.state.test {
            val loaded = awaitItem()
            assertEquals("stashed", loaded.questions.first().mcqId)
        }
        assertEquals(0, fakeRepo.getAttemptQuestionsCallCount)
    }

    @Test
    fun `selectAnswer records the answer locally and syncs it to the repository`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Success(defaultAttemptData(questions = listOf(fakeQuestion(mcqId = "mcq1"))))
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.selectAnswer("mcq1", "o1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("o1", viewModel.state.value.selectedAnswers["mcq1"])
        assertEquals(1, fakeRepo.lastSubmittedResponses?.responses?.size)
        assertEquals("mcq1", fakeRepo.lastSubmittedResponses?.responses?.first()?.mcqId)
        assertEquals("o1", fakeRepo.lastSubmittedResponses?.responses?.first()?.selectedOptionId)
    }

    @Test
    fun `tickTimer decrements remaining time and opens the submit dialog at zero`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Success(defaultAttemptData())
        val viewModel = buildViewModel()
        advanceUntilIdle()

        // Drive the timer to zero one tick at a time using the real duration (10 min = 600s).
        repeat(600) { viewModel.tickTimer() }

        val finalState = viewModel.state.value
        assertEquals(0, finalState.timeRemainingSeconds)
        assertTrue(finalState.showSubmitDialog)
    }

    @Test
    fun `tickTimer never goes below zero`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Success(defaultAttemptData())
        val viewModel = buildViewModel()
        advanceUntilIdle()

        repeat(650) { viewModel.tickTimer() } // 50 extra ticks past zero
        assertEquals(0, viewModel.state.value.timeRemainingSeconds)
    }

    @Test
    fun `submitExam success marks the attempt submitted and clears the submit dialog`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Success(defaultAttemptData())
        fakeRepo.submitAttemptResult = ApiResponse.Success(FakeExamAttemptRepositoryForEngineTest.defaultResult())
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.showSubmitDialog()

        viewModel.submitExam()
        testDispatcher.scheduler.advanceUntilIdle()

        val finalState = viewModel.state.value
        assertTrue(finalState.isSubmitted)
        assertFalse(finalState.isSubmitting)
        assertFalse(finalState.showSubmitDialog)
    }

    @Test
    fun `submitExam failure populates SUBMIT error and allows retry`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Success(defaultAttemptData())
        fakeRepo.submitAttemptResult = ApiResponse.Failure(message = "Network error", errorCode = "NETWORK_ERROR")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.submitExam()
        testDispatcher.scheduler.advanceUntilIdle()

        val finalState = viewModel.state.value
        assertFalse(finalState.isSubmitted)
        assertFalse(finalState.isSubmitting)
        assertEquals("Network error", finalState.errors["SUBMIT"])
    }

    @Test
    fun `submitExam is a no-op once already submitted`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Success(defaultAttemptData())
        fakeRepo.submitAttemptResult = ApiResponse.Success(FakeExamAttemptRepositoryForEngineTest.defaultResult())
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.submitExam()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isSubmitted)

        fakeRepo.submitAttemptResult = ApiResponse.Failure(message = "should never be hit", errorCode = "X")
        viewModel.submitExam() // guarded no-op
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isSubmitted)
        assertNull(viewModel.state.value.errors["SUBMIT"])
    }

    @Test
    fun `toggleFavorite adds then removes an mcq id`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Success(defaultAttemptData())
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.toggleFavorite("mcq1")
        assertTrue("mcq1" in viewModel.state.value.favoriteMcqIds)

        viewModel.toggleFavorite("mcq1")
        assertFalse("mcq1" in viewModel.state.value.favoriteMcqIds)
    }

    @Test
    fun `submitReport success sets a confirmation message`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Success(defaultAttemptData())
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.submitReport("mcq1", reason = "wrong_answer", description = "")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("প্রশ্নটি রিপোর্ট করা হয়েছে।", viewModel.state.value.reportResultMessage)

        viewModel.clearReportResultMessage()
        assertNull(viewModel.state.value.reportResultMessage)
    }

    @Test
    fun `submitReport failure populates REPORT error`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Success(defaultAttemptData())
        fakeRepo.reportMcqResult = ApiResponse.Failure(message = "Report failed", errorCode = "ERR")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.submitReport("mcq1", reason = "wrong_answer", description = "")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Report failed", viewModel.state.value.errors["REPORT"])
    }

    @Test
    fun `clearError removes only the targeted tag`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Failure(message = "boom", errorCode = "404")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals("boom", viewModel.state.value.errors["RESUME"])
        viewModel.clearError("RESUME")
        assertNull(viewModel.state.value.errors["RESUME"])
    }

    @Test
    fun `prefetchPassages loads passages for questions that have a passage id`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Success(
            defaultAttemptData(questions = listOf(fakeQuestion(mcqId = "mcq1", passageId = "p1")))
        )
        fakeRepo.fetchPassageResult = ApiResponse.Success(
            com.prostuti.student.data.model.response.FetchPassageData(id = "p1", passageText = "passage body")
        )
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals("passage body", viewModel.state.value.passages["p1"]?.passageText)
        assertEquals(1, viewModel.state.value.questions.size)
    }

    @Test
    fun `prefetchPassages failure is scoped to that passage's own error tag`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Success(
            defaultAttemptData(questions = listOf(fakeQuestion(mcqId = "mcq1", passageId = "p1")))
        )
        fakeRepo.fetchPassageResult = ApiResponse.Failure(message = "passage missing", errorCode = "404")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals("passage missing", viewModel.state.value.errors["PASSAGE_p1"])
        assertEquals(1, viewModel.state.value.questions.size)
    }
}
