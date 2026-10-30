package com.prostuti.student.ui.screen.live

import app.cash.turbine.test
import com.prostuti.student.data.model.enums.TimedExamState
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MyTimedExamSubmission
import com.prostuti.student.data.repository.fake.FakeLiveExamRepositoryForTest
import com.prostuti.student.data.repository.fake.FakeTimedExamRepositoryForTest
import com.prostuti.student.data.repository.fake.fakeTimedExam
import com.prostuti.student.domain.FetchLiveExamListUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class LiveExamListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeLiveExamRepository = FakeLiveExamRepositoryForTest()
    private val fakeTimedExamRepository = FakeTimedExamRepositoryForTest()
    private val fakeTimeProvider = FakeTimeProvider(now = 10_000L)

    private fun viewModel() = LiveExamListViewModel(
        FetchLiveExamListUseCase(fakeLiveExamRepository),
        FetchMyTimedSubmissionUseCase(fakeTimedExamRepository),
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
    fun initialStateIsLoadingWithNoRows() {
        val state = viewModel().state.value
        assertTrue(state.isLoading)
        assertTrue(state.rows.isEmpty())
    }

    @Test
    fun loadSuccessDerivesStatePerExamFromWindowAndSubmission() = runTest {
        val upcoming = fakeTimedExam(examId = "e_upcoming", windowStartEpochMillis = 20_000L, windowEndEpochMillis = 30_000L)
        val openNotSubmitted = fakeTimedExam(examId = "e_open", windowStartEpochMillis = 0L, windowEndEpochMillis = 20_000L)
        val closed = fakeTimedExam(examId = "e_closed", windowStartEpochMillis = 0L, windowEndEpochMillis = 5_000L)
        fakeLiveExamRepository.liveExamListResult = ApiResponse.Success(listOf(upcoming, openNotSubmitted, closed))
        fakeTimedExamRepository.mySubmissionResult = null

        val viewModel = viewModel()
        viewModel.state.test {
            assertTrue(awaitItem().isLoading) // initial
            val loaded = awaitItem()
            assertTrue(!loaded.isLoading)
            assertEquals(3, loaded.rows.size)
            assertEquals(TimedExamState.UPCOMING, loaded.rows.first { it.exam.examId == "e_upcoming" }.state)
            assertEquals(TimedExamState.OPEN_NOT_SUBMITTED, loaded.rows.first { it.exam.examId == "e_open" }.state)
            assertEquals(TimedExamState.CLOSED, loaded.rows.first { it.exam.examId == "e_closed" }.state)
        }
    }

    @Test
    fun loadSuccessWithSubmissionDerivesOpenSubmitted() = runTest {
        val exam = fakeTimedExam(examId = "e1", windowStartEpochMillis = 0L, windowEndEpochMillis = 20_000L)
        fakeLiveExamRepository.liveExamListResult = ApiResponse.Success(listOf(exam))
        fakeTimedExamRepository.mySubmissionResult = MyTimedExamSubmission(
            submittedAtEpochMillis = 9_000L, timeTakenSeconds = 120, autoSubmitted = false
        )

        val viewModel = viewModel()
        viewModel.state.test {
            skipItems(1) // initial loading
            val loaded = awaitItem()
            assertEquals(TimedExamState.OPEN_SUBMITTED, loaded.rows.single().state)
        }
    }

    @Test
    fun loadFailurePopulatesErrorsMap() = runTest {
        fakeLiveExamRepository.liveExamListResult = ApiResponse.Failure(message = "নেটওয়ার্ক সমস্যা", errorCode = "NETWORK_ERROR")

        val viewModel = viewModel()
        viewModel.state.test {
            assertTrue(awaitItem().isLoading)
            val result = awaitItem()
            assertTrue(!result.isLoading)
            assertTrue(result.rows.isEmpty())
            assertEquals("নেটওয়ার্ক সমস্যা", result.errors["LOAD"])
        }
    }

    @Test
    fun clearErrorRemovesTheTag() = runTest {
        fakeLiveExamRepository.liveExamListResult = ApiResponse.Failure(message = "err", errorCode = "NETWORK_ERROR")
        val viewModel = viewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.errors.containsKey("LOAD"))

        viewModel.clearError("LOAD")
        assertTrue(!viewModel.state.value.errors.containsKey("LOAD"))
    }

    @Test
    fun refreshReloadsTheList() = runTest {
        fakeLiveExamRepository.liveExamListResult = ApiResponse.Success(emptyList())
        val viewModel = viewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.rows.isEmpty())

        fakeLiveExamRepository.liveExamListResult = ApiResponse.Success(listOf(fakeTimedExam(examId = "new")))
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.rows.size)
        assertEquals("new", viewModel.state.value.rows.single().exam.examId)
    }
}
