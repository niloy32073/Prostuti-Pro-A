package com.prostuti.student.ui.screen.examengine

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.domain.GetExamAttemptUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Fakes return immediately, so the init load settles inside construction; tests assert the
 * settled state rather than the conflated transient isLoading=true emission. */
@OptIn(ExperimentalCoroutinesApi::class)
class ExamResultViewModelTest {

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

    private fun buildViewModel(attemptId: String = "attempt1") =
        ExamResultViewModel(attemptId = attemptId, getExamAttemptUseCase = GetExamAttemptUseCase(fakeRepo))

    @Test
    fun `load success exposes the score breakdown`() = runTest {
        fakeRepo.getAttemptResult = ApiResponse.Success(FakeExamAttemptRepositoryForEngineTest.defaultResult())
        val viewModel = buildViewModel()
        advanceUntilIdle()

        val loaded = viewModel.state.value
        assertFalse(loaded.isLoading)
        assertEquals(3, loaded.result?.rightAnswerCount)
        assertEquals(1, loaded.result?.wrongAnswerCount)
        assertEquals(1, loaded.result?.skipCount)
        assertEquals(3.0, loaded.result?.totalScore)
        assertEquals(2, loaded.result?.rank)
    }

    @Test
    fun `load failure populates LOAD error`() = runTest {
        fakeRepo.getAttemptResult = ApiResponse.Failure(message = "Result unavailable", errorCode = "404")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        val errorState = viewModel.state.value
        assertFalse(errorState.isLoading)
        assertEquals("Result unavailable", errorState.errors["LOAD"])
    }

    @Test
    fun `clearError removes only the targeted tag`() = runTest {
        fakeRepo.getAttemptResult = ApiResponse.Failure(message = "boom", errorCode = "500")
        val viewModel = buildViewModel()
        advanceUntilIdle()
        assertEquals("boom", viewModel.state.value.errors["LOAD"])

        viewModel.clearError("LOAD")
        assertTrue(viewModel.state.value.errors.isEmpty())
    }
}
