package com.prostuti.student.ui.screen.timedexam

import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.repository.fake.FakeTimedExamRepositoryForTest
import com.prostuti.student.domain.AutoSubmitAbandonedAttemptUseCase
import com.prostuti.student.domain.FetchInProgressAnswersUseCase
import com.prostuti.student.domain.FetchTimedExamDurationUseCase
import com.prostuti.student.domain.FetchTimedExamQuestionsUseCase
import com.prostuti.student.domain.FetchTimedExamWindowUseCase
import com.prostuti.student.domain.SaveTimedExamAnswerUseCase
import com.prostuti.student.domain.SubmitTimedExamUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimedExamAttemptViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repo = FakeTimedExamRepositoryForTest()

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
}
