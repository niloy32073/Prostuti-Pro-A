package com.prostuti.student.ui.screen.examengine

import app.cash.turbine.test
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.domain.GetExamLeaderboardUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class ExamLeaderboardViewModelTest {

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

    private fun buildViewModel(examId: String = "exam1") =
        ExamLeaderboardViewModel(examId = examId, getExamLeaderboardUseCase = GetExamLeaderboardUseCase(fakeRepo))

    @Test
    fun `load success exposes entries in server rank order and requests page 1`() = runTest {
        val viewModel = buildViewModel()

        viewModel.state.test {
            val initial = awaitItem()
            assertTrue(initial.isLoading)

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertFalse(loaded.notReadyYet)
            assertEquals(listOf(1, 2, 3), loaded.leaderboard?.entries?.map { it.rank })
            assertEquals(1, fakeRepo.getExamLeaderboardLastPage)
        }
    }

    @Test
    fun `a 404 is surfaced as notReadyYet, not as a real error`() = runTest {
        fakeRepo.getExamLeaderboardResult = ApiResponse.Failure(message = "Not found", errorCode = "404")
        val viewModel = buildViewModel()

        viewModel.state.test {
            awaitItem() // initial loading
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.notReadyYet)
            assertTrue(state.errors.isEmpty())
        }
    }

    @Test
    fun `a non-404 failure is surfaced as a real LOAD error, not notReadyYet`() = runTest {
        fakeRepo.getExamLeaderboardResult = ApiResponse.Failure(message = "Server error", errorCode = "500")
        val viewModel = buildViewModel()

        viewModel.state.test {
            awaitItem() // initial loading
            val state = awaitItem()
            assertFalse(state.notReadyYet)
            assertEquals("Server error", state.errors["LOAD"])
        }
    }

    @Test
    fun `clearError removes only the targeted tag`() = runTest {
        fakeRepo.getExamLeaderboardResult = ApiResponse.Failure(message = "boom", errorCode = "500")
        val viewModel = buildViewModel()
        viewModel.state.test { awaitItem(); awaitItem() }

        viewModel.clearError("LOAD")
        assertTrue(viewModel.state.value.errors.isEmpty())
    }
}
