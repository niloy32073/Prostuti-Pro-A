package com.prostuti.student.ui.screen.analysis

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.AttemptAnalysisData
import com.prostuti.student.data.model.response.TopicAnalysisItem
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.data.repository.fake.FakeQbCurriculumRepository
import com.prostuti.student.domain.GetAttemptAnalysisUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AttemptAnalysisViewModelTest {

    private lateinit var repo: FakeExamAttemptRepositoryForEngineTest

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = FakeExamAttemptRepositoryForEngineTest()
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun vm() = AttemptAnalysisViewModel("attempt1", GetAttemptAnalysisUseCase(repo, FakeQbCurriculumRepository()))

    @Test
    fun `weakest is the first topic with a wrong answer`() = runTest {
        repo.analysisResult = ApiResponse.Success(
            AttemptAnalysisData(
                "attempt1", "exam1",
                topics = listOf(
                    TopicAnalysisItem("t1", 2, 2, 0, 0),
                    TopicAnalysisItem("t2", 3, 1, 2, 0)
                )
            )
        )
        val s = vm().state.value
        assertFalse(s.isLoading)
        assertEquals("t2", s.weakest?.topicId)
        assertEquals(5, s.totalQuestions)
    }

    @Test
    fun `no wrong answers means no weakest card`() = runTest {
        repo.analysisResult = ApiResponse.Success(AttemptAnalysisData("attempt1", "exam1", topics = listOf(TopicAnalysisItem("t1", 2, 2, 0, 0))))
        assertNull(vm().state.value.weakest)
    }

    @Test
    fun `failure populates LOAD error`() = runTest {
        repo.analysisResult = ApiResponse.Failure("This attempt has not been submitted yet.", "ATTEMPT_NOT_FINALIZED")
        assertEquals("This attempt has not been submitted yet.", vm().state.value.errors["LOAD"])
    }
}
