package com.prostuti.student.ui.screen.live

import app.cash.turbine.test
import com.prostuti.student.data.model.enums.TimedExamState
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeLiveExamRepositoryForTest
import com.prostuti.student.data.repository.fake.FakeTimedExamRepositoryForTest
import com.prostuti.student.data.repository.fake.fakeTimedExam
import com.prostuti.student.domain.AdvanceDemoStateUseCase
import com.prostuti.student.domain.FetchLiveExamDetailUseCase
import com.prostuti.student.domain.FetchMyTimedSubmissionUseCase
import com.prostuti.student.util.FakeTimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** [LiveExamDetailViewModel.init] starts an infinite `while (true) { delay(30_000) }` ticker
 * alongside [LiveExamDetailViewModel.load]. Tests here deliberately never call
 * `advanceUntilIdle()` — with that ticker running on the same scheduler it would advance virtual
 * time forever and the test would hang. `runCurrent()` (runs only work already due at the
 * current virtual time, without advancing time) and Turbine's `awaitItem()` are used instead,
 * since `load()` itself has no `delay()` in it. */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveExamDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeLiveExamRepository = FakeLiveExamRepositoryForTest()
    private val fakeTimedExamRepository = FakeTimedExamRepositoryForTest()
    private val fakeTimeProvider = FakeTimeProvider(now = 10_000L)

    private fun viewModel(examId: String = "e1") = LiveExamDetailViewModel(
        examId,
        FetchLiveExamDetailUseCase(fakeLiveExamRepository),
        FetchMyTimedSubmissionUseCase(fakeTimedExamRepository),
        AdvanceDemoStateUseCase(fakeTimedExamRepository),
        fakeTimeProvider
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsLoadingWithNoExam() {
        val state = viewModel().state.value
        assertTrue(state.isLoading)
        assertEquals(null, state.exam)
    }

    @Test
    fun loadSuccessPopulatesExamAndDerivesOpenNotSubmittedState() = runTest {
        val exam = fakeTimedExam(examId = "e1", windowStartEpochMillis = 0L, windowEndEpochMillis = 20_000L)
        fakeLiveExamRepository.liveExamDetailResult = ApiResponse.Success(exam)
        fakeTimedExamRepository.mySubmissionResult = null

        val viewModel = viewModel("e1")
        viewModel.state.test {
            assertTrue(awaitItem().isLoading) // initial
            val loaded = awaitItem()
            assertTrue(!loaded.isLoading)
            assertEquals("e1", loaded.exam?.examId)
            assertEquals(TimedExamState.OPEN_NOT_SUBMITTED, loaded.examState)
        }
        assertEquals("e1", fakeLiveExamRepository.lastDetailExamId)
    }

    @Test
    fun loadFailurePopulatesErrorsMap() = runTest {
        fakeLiveExamRepository.liveExamDetailResult = ApiResponse.Failure(message = "এক্সাম পাওয়া যায়নি", errorCode = "NOT_FOUND")

        val viewModel = viewModel("missing")
        viewModel.state.test {
            assertTrue(awaitItem().isLoading)
            val result = awaitItem()
            assertTrue(!result.isLoading)
            assertEquals(null, result.exam)
            assertEquals("এক্সাম পাওয়া যায়নি", result.errors["LOAD"])
        }
    }

    @Test
    fun examStateIsClosedWhenNowIsAtOrAfterWindowEnd() = runTest {
        fakeTimeProvider.now = 20_000L
        val exam = fakeTimedExam(examId = "e1", windowStartEpochMillis = 0L, windowEndEpochMillis = 20_000L)
        fakeLiveExamRepository.liveExamDetailResult = ApiResponse.Success(exam)

        val viewModel = viewModel("e1")
        viewModel.state.test {
            skipItems(1)
            val loaded = awaitItem()
            assertEquals(TimedExamState.CLOSED, loaded.examState)
        }
    }

    @Test
    fun advanceDemoStateReloadsAfterAdvancing() = runTest {
        val exam = fakeTimedExam(examId = "e1", windowStartEpochMillis = 0L, windowEndEpochMillis = 20_000L)
        fakeLiveExamRepository.liveExamDetailResult = ApiResponse.Success(exam)

        val viewModel = viewModel("e1")
        testDispatcher.scheduler.runCurrent()
        assertTrue(!viewModel.state.value.isLoading)

        viewModel.advanceDemoState()
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, fakeTimedExamRepository.advanceDemoStateCallCount)
        assertEquals("e1", fakeTimedExamRepository.lastAdvanceDemoStateExamId)
        assertTrue(!viewModel.state.value.isDemoAdvancing)
        // liveExamDetail is re-fetched by the reload triggered from advanceDemoState()
        assertTrue(fakeLiveExamRepository.liveExamDetailCallCount >= 2)
    }

    @Test
    fun clearErrorRemovesTheTag() = runTest {
        fakeLiveExamRepository.liveExamDetailResult = ApiResponse.Failure(message = "err", errorCode = "NOT_FOUND")
        val viewModel = viewModel("e1")
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.state.value.errors.containsKey("LOAD"))

        viewModel.clearError("LOAD")
        assertTrue(!viewModel.state.value.errors.containsKey("LOAD"))
    }
}
