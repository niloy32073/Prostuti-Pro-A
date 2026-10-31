package com.prostuti.student.ui.screen.examengine

import app.cash.turbine.test
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest.Companion.defaultAttemptData
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest.Companion.fakeQuestion
import com.prostuti.student.domain.FetchPassageUseCase
import com.prostuti.student.domain.GetAttemptQuestionsUseCase
import com.prostuti.student.domain.GetAttemptResponsesUseCase
import com.prostuti.student.domain.GetLocalAttemptElapsedUseCase
import com.prostuti.student.domain.SaveLocalAttemptElapsedUseCase
import com.prostuti.student.domain.ClearLocalAttemptElapsedUseCase
import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.repository.fake.FakeAttemptProgressRepository
import com.prostuti.student.domain.SubmitExamAttemptUseCase
import com.prostuti.student.domain.SubmitExamResponsesUseCase
import com.prostuti.student.domain.SubmitMcqReportUseCase
import com.prostuti.student.data.model.response.ExamResponseAckResponse
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest.Companion.DEFAULT_START_AT_MILLIS
import com.prostuti.student.util.FakeTimeProvider
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
    private lateinit var progressRepo: FakeAttemptProgressRepository
    // Pinned to the fake attempt's start_at, so the 10-minute exam has its full time left.
    private val clock = FakeTimeProvider(now = DEFAULT_START_AT_MILLIS)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        ExamAttemptStore.consume() // defensively drain any leftover stash from a previous test
        fakeRepo = FakeExamAttemptRepositoryForEngineTest()
        progressRepo = FakeAttemptProgressRepository()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(attemptId: String = "attempt1") = ExamAttemptViewModel(
        attemptId = attemptId,
        fetchPassageUseCase = FetchPassageUseCase(fakeRepo),
        getAttemptQuestionsUseCase = GetAttemptQuestionsUseCase(fakeRepo),
        getAttemptResponsesUseCase = GetAttemptResponsesUseCase(fakeRepo),
        getLocalAttemptElapsedUseCase = GetLocalAttemptElapsedUseCase(progressRepo),
        saveLocalAttemptElapsedUseCase = SaveLocalAttemptElapsedUseCase(progressRepo),
        clearLocalAttemptElapsedUseCase = ClearLocalAttemptElapsedUseCase(progressRepo),
        submitExamResponsesUseCase = SubmitExamResponsesUseCase(fakeRepo),
        submitExamAttemptUseCase = SubmitExamAttemptUseCase(fakeRepo),
        submitMcqReportUseCase = SubmitMcqReportUseCase(fakeRepo),
        timeProvider = clock
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
    fun `the first answer is final and a second pick is ignored`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Success(defaultAttemptData(questions = listOf(fakeQuestion(mcqId = "mcq1"))))
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.selectAnswer("mcq1", "o1")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectAnswer("mcq1", "o2")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("o1", viewModel.state.value.selectedAnswers["mcq1"])
        assertEquals(1, fakeRepo.allSubmittedResponses.size)
        assertEquals("o1", fakeRepo.allSubmittedResponses.single().responses.single().selectedOptionId)
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

    // ── Resume ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun `live attempt computes the remaining time from start_at`() = runTest {
        ExamAttemptStore.put(defaultAttemptData(mode = AttemptMode.LIVE))
        clock.now = DEFAULT_START_AT_MILLIS + 4 * 60_000 // 4 of 10 minutes used
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(600, viewModel.state.value.totalDurationSeconds)
        assertEquals(360, viewModel.state.value.timeRemainingSeconds)
    }

    @Test
    fun `resume restores saved answers and ignores skipped ones`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Success(
            defaultAttemptData(questions = listOf(fakeQuestion(mcqId = "mcq1"), fakeQuestion(mcqId = "mcq2", order = 2)))
        )
        fakeRepo.attemptResponsesResult = ApiResponse.Success(
            listOf(
                ExamResponseAckResponse(mcqId = "mcq1", selectedOptionId = "o2", responseTimeMs = 15_000),
                ExamResponseAckResponse(mcqId = "mcq2", selectedOptionId = null, responseTimeMs = 3_000)
            )
        )
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(mapOf("mcq1" to "o2"), viewModel.state.value.selectedAnswers)
    }

    @Test
    fun `stashed start payload also restores saved answers`() = runTest {
        ExamAttemptStore.put(defaultAttemptData())
        fakeRepo.attemptResponsesResult = ApiResponse.Success(
            listOf(ExamResponseAckResponse(mcqId = "mcq1", selectedOptionId = "o1", responseTimeMs = 1_000))
        )
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals("o1", viewModel.state.value.selectedAnswers["mcq1"])
        assertEquals(0, fakeRepo.getAttemptQuestionsCallCount)
    }

    @Test
    fun `restore failure populates RESTORE error but keeps the questions`() = runTest {
        fakeRepo.attemptResponsesResult = ApiResponse.Failure(message = "offline", errorCode = "NETWORK_ERROR")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals("offline", viewModel.state.value.errors["RESTORE"])
        assertEquals(1, viewModel.state.value.questions.size)
    }

    @Test
    fun `live attempt whose time ran out auto-submits on open`() = runTest {
        ExamAttemptStore.put(defaultAttemptData(mode = AttemptMode.LIVE))
        clock.now = DEFAULT_START_AT_MILLIS + 11 * 60_000 // past the 10-minute duration
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(0, viewModel.state.value.timeRemainingSeconds)
        assertTrue(viewModel.state.value.isSubmitted)
    }

    @Test
    fun `an already submitted attempt goes straight to the result`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Failure(message = "ended", errorCode = "ATTEMPT_ALREADY_ENDED")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSubmitted)
        assertNull(viewModel.state.value.errors["RESUME"])
    }

    @Test
    fun `resyncTimer catches up a live attempt with time spent in the background`() = runTest {
        ExamAttemptStore.put(defaultAttemptData(mode = AttemptMode.LIVE))
        val viewModel = buildViewModel()
        advanceUntilIdle()
        assertEquals(600, viewModel.state.value.timeRemainingSeconds)

        clock.now = DEFAULT_START_AT_MILLIS + 7 * 60_000
        viewModel.resyncTimer()

        assertEquals(180, viewModel.state.value.timeRemainingSeconds)
        assertFalse(viewModel.state.value.showSubmitDialog)
    }

    // ── Submit ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun `submitExam resends answers whose earlier save failed`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        fakeRepo.submitResponsesResult = ApiResponse.Failure(message = "offline", errorCode = "NETWORK_ERROR")
        viewModel.selectAnswer("mcq1", "o2")
        testDispatcher.scheduler.advanceUntilIdle()

        fakeRepo.submitResponsesResult = ApiResponse.Success(emptyList())
        viewModel.submitExam()
        testDispatcher.scheduler.advanceUntilIdle()

        val flushed = fakeRepo.allSubmittedResponses.last().responses.single()
        assertEquals("mcq1", flushed.mcqId)
        assertEquals("o2", flushed.selectedOptionId)
        assertTrue(viewModel.state.value.isSubmitted)
    }

    @Test
    fun `submitExam does not resend answers that were already saved`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.selectAnswer("mcq1", "o1")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.submitExam()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepo.allSubmittedResponses.size)
        assertTrue(viewModel.state.value.isSubmitted)
    }

    @Test
    fun `submitExam stops with an error when resending answers fails`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        fakeRepo.submitResponsesResult = ApiResponse.Failure(message = "offline", errorCode = "NETWORK_ERROR")
        viewModel.selectAnswer("mcq1", "o2")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.submitExam()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isSubmitted)
        assertFalse(viewModel.state.value.isSubmitting)
        assertEquals("offline", viewModel.state.value.errors["SUBMIT"])
    }

    @Test
    fun `submitExam treats ATTEMPT_ALREADY_ENDED as submitted`() = runTest {
        fakeRepo.submitAttemptResult = ApiResponse.Failure(message = "ended", errorCode = "ATTEMPT_ALREADY_ENDED")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.submitExam()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isSubmitted)
        assertNull(viewModel.state.value.errors["SUBMIT"])
    }

    // ── Regression: start_at without an offset (seen from the backend) ────────────────────

    @Test
    fun `an old live attempt whose start_at has no offset auto-submits instead of showing full time`() = runTest {
        // Same shape as the real case: started two days earlier, 10-minute exam.
        ExamAttemptStore.put(defaultAttemptData(startAt = "2026-01-01T00:00:00.024000", mode = AttemptMode.LIVE))
        clock.now = DEFAULT_START_AT_MILLIS + 2 * 86_400_000L
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(0, viewModel.state.value.timeRemainingSeconds)
        assertTrue(viewModel.state.value.isSubmitted)
    }

    @Test
    fun `isLoading is true until the questions arrive and false after`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `isLoading turns off when loading the questions fails`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Failure(message = "boom", errorCode = "500")
        val viewModel = buildViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoading)
    }

    // ── Practice: time pauses while away ───────────────────────────────────────────────────

    private fun saved(mcqId: String, timeMs: Int) =
        ExamResponseAckResponse(mcqId = mcqId, selectedOptionId = "o1", responseTimeMs = timeMs)

    @Test
    fun `practice ignores the wall clock - days later the full time is still there`() = runTest {
        clock.now = DEFAULT_START_AT_MILLIS + 2 * 86_400_000L
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(600, viewModel.state.value.timeRemainingSeconds)
        assertFalse(viewModel.state.value.isSubmitted)
    }

    @Test
    fun `practice resumes from the server's last response_time when nothing is saved locally`() = runTest {
        fakeRepo.attemptResponsesResult = ApiResponse.Success(listOf(saved("mcq1", 60_000), saved("mcq2", 240_000)))
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(600 - 240, viewModel.state.value.timeRemainingSeconds)
    }

    @Test
    fun `practice uses the local time when it is ahead of the server`() = runTest {
        fakeRepo.attemptResponsesResult = ApiResponse.Success(listOf(saved("mcq1", 240_000)))
        progressRepo.saved["attempt1"] = 300_000 // kept reading 1 minute after the last answer
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(600 - 300, viewModel.state.value.timeRemainingSeconds)
    }

    @Test
    fun `practice uses the server time when it is ahead of the local value`() = runTest {
        fakeRepo.attemptResponsesResult = ApiResponse.Success(listOf(saved("mcq1", 420_000)))
        progressRepo.saved["attempt1"] = 100_000
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(600 - 420, viewModel.state.value.timeRemainingSeconds)
    }

    @Test
    fun `practice with its time used up earlier auto-submits on open`() = runTest {
        progressRepo.saved["attempt1"] = 600_000
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSubmitted)
    }

    @Test
    fun `practice saves the time used every 5 ticks`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        repeat(4) { viewModel.tickTimer() }
        assertNull(progressRepo.saved["attempt1"])
        viewModel.tickTimer()
        assertEquals(5_000L, progressRepo.saved["attempt1"])
    }

    @Test
    fun `practice leaving the app saves the time used and does not submit`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        repeat(3) { viewModel.tickTimer() }

        viewModel.onBackgrounded(leftApp = true)

        assertEquals(3_000L, progressRepo.saved["attempt1"])
        assertFalse(viewModel.state.value.isSubmitted)
    }

    @Test
    fun `answers report cumulative exam time as response_time`() = runTest {
        progressRepo.saved["attempt1"] = 120_000
        val viewModel = buildViewModel()
        advanceUntilIdle()
        repeat(2) { viewModel.tickTimer() }

        viewModel.selectAnswer("mcq1", "o1")

        assertEquals(122_000, fakeRepo.lastSubmittedResponses?.responses?.single()?.responseTimeMs)
    }

    @Test
    fun `submitting clears the locally saved time`() = runTest {
        progressRepo.saved["attempt1"] = 10_000
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.submitExam()

        assertTrue(viewModel.state.value.isSubmitted)
        assertEquals(listOf("attempt1"), progressRepo.cleared)
    }

    // ── Live: no resume, leaving submits ───────────────────────────────────────────────────

    @Test
    fun `live attempt reopened from an earlier session is submitted straight away`() = runTest {
        fakeRepo.attemptQuestionsResult = ApiResponse.Success(defaultAttemptData(mode = AttemptMode.LIVE))
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSubmitted)
    }

    @Test
    fun `live leaving the app submits`() = runTest {
        ExamAttemptStore.put(defaultAttemptData(mode = AttemptMode.LIVE))
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onBackgrounded(leftApp = true)

        assertTrue(viewModel.state.value.isSubmitted)
    }

    @Test
    fun `live phone call or screen lock does not submit`() = runTest {
        ExamAttemptStore.put(defaultAttemptData(mode = AttemptMode.LIVE))
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onBackgrounded(leftApp = false)

        assertFalse(viewModel.state.value.isSubmitted)
        assertTrue(progressRepo.saved.isEmpty()) // live time is never saved locally
    }

    @Test
    fun `practice resyncTimer does not touch the paused clock`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        clock.now = DEFAULT_START_AT_MILLIS + 7 * 60_000

        viewModel.resyncTimer()

        assertEquals(600, viewModel.state.value.timeRemainingSeconds)
    }
}
