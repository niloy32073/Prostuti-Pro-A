package com.prostuti.student.ui.screen.timedexam

import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MyTimedExamSubmission
import com.prostuti.student.data.repository.fake.FakeTimedExamRepositoryForTest
import com.prostuti.student.domain.AutoSubmitAbandonedAttemptUseCase
import com.prostuti.student.domain.FetchInProgressAnswersUseCase
import com.prostuti.student.domain.FetchTimedExamDurationUseCase
import com.prostuti.student.domain.FetchTimedExamQuestionsUseCase
import com.prostuti.student.domain.FetchTimedExamWindowUseCase
import com.prostuti.student.domain.SaveTimedExamAnswerUseCase
import com.prostuti.student.domain.SubmitTimedExamUseCase
import com.prostuti.student.util.FakeTimeProvider
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimedExamAttemptViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repo = FakeTimedExamRepositoryForTest()
    private val clock = FakeTimeProvider(now = 0L)

    @Before fun setup() = Dispatchers.setMain(testDispatcher)

    @After fun teardown() = Dispatchers.resetMain()

    private fun buildViewModel() = TimedExamAttemptViewModel(
        examId = "exam1",
        kind = TimedExamKind.LIVE,
        fetchTimedExamQuestionsUseCase = FetchTimedExamQuestionsUseCase(repo),
        fetchInProgressAnswersUseCase = FetchInProgressAnswersUseCase(repo),
        saveTimedExamAnswerUseCase = SaveTimedExamAnswerUseCase(repo),
        submitTimedExamUseCase = SubmitTimedExamUseCase(repo),
        autoSubmitAbandonedAttemptUseCase = AutoSubmitAbandonedAttemptUseCase(repo),
        fetchTimedExamDurationUseCase = FetchTimedExamDurationUseCase(repo),
        fetchTimedExamWindowUseCase = FetchTimedExamWindowUseCase(repo),
        timeProvider = clock,
    )

    @Test
    fun `the first answer is final and a second pick is ignored`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.selectAnswer("mcq1", "o1")
        advanceUntilIdle()
        viewModel.selectAnswer("mcq1", "o2")
        advanceUntilIdle()

        assertEquals("o1", viewModel.state.value.answers["mcq1"])
        assertEquals(listOf("mcq1" to "o1"), repo.savedAnswers)
    }

    @Test
    fun `different questions can each be answered once`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.selectAnswer("mcq1", "o1")
        viewModel.selectAnswer("mcq2", "o3")
        advanceUntilIdle()

        assertEquals(mapOf("mcq1" to "o1", "mcq2" to "o3"), viewModel.state.value.answers)
    }

    // ── Time up ────────────────────────────────────────────────────────────────────────────

    private val submission = MyTimedExamSubmission(submittedAtEpochMillis = 0L, timeTakenSeconds = 3600, autoSubmitted = true)

    @Test
    fun `the clock runs on real time, so time spent away still counts`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        assertEquals(3600, viewModel.state.value.timeRemainingSeconds)

        clock.now = 25 * 60_000L // screen locked for 25 minutes
        viewModel.tickTimer()

        assertEquals(35 * 60, viewModel.state.value.timeRemainingSeconds)
        assertFalse(viewModel.state.value.isTimeUp)
    }

    @Test
    fun `reaching zero submits on its own and shows the time-up state`() = runTest(testDispatcher) {
        repo.submitResult = ApiResponse.Success(submission)
        val viewModel = buildViewModel()
        advanceUntilIdle()

        clock.now = 90 * 60_000L // well past the 60-minute exam
        viewModel.tickTimer()
        advanceUntilIdle()

        val s = viewModel.state.value
        assertEquals(0, s.timeRemainingSeconds)
        assertTrue(s.isTimeUp)
        assertTrue(s.isSubmitted)
        assertEquals(1, repo.submitCallCount)
    }

    @Test
    fun `answers are locked once the time is up`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        clock.now = 61 * 60_000L
        viewModel.tickTimer()
        advanceUntilIdle()
        viewModel.selectAnswer("mcq1", "o1")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.answers.isEmpty())
        assertTrue(repo.savedAnswers.isEmpty())
    }

    @Test
    fun `a failed time-up submit keeps the time-up state so it can be retried`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        clock.now = 61 * 60_000L
        viewModel.tickTimer()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isTimeUp)
        assertFalse(viewModel.state.value.isSubmitted)

        repo.submitResult = ApiResponse.Success(submission)
        viewModel.submitExam()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSubmitted)
        assertEquals(2, repo.submitCallCount)
    }

    @Test
    fun `an exam whose window already closed submits as soon as it opens`() = runTest(testDispatcher) {
        repo.submitResult = ApiResponse.Success(submission)
        repo.window = 0L to 0L
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isTimeUp)
        assertTrue(viewModel.state.value.isSubmitted)
    }
}
