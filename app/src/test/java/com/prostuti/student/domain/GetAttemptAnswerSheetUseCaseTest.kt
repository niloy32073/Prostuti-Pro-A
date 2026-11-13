package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetAttemptAnswerSheetUseCaseTest {

    @Test
    fun `a passage named only by ID is fetched and attached`() = runTest {
        val repo = FakeExamAttemptRepositoryForEngineTest(
            fetchPassageResult = ApiResponse.Success(FetchPassageData("p1", passageText = "উদ্দীপক", passageImage = "https://cdn/p.png"))
        )
        repo.questionsWithOptionsResult = ApiResponse.Success(
            FakeExamAttemptRepositoryForEngineTest.defaultQuestionsWithOptions().map {
                if (it.mcqId == "mcq2") it.copy(passageId = "p1") else it
            }
        )

        val result = GetAttemptAnswerSheetUseCase(repo, repo)("attempt1") as ApiResponse.Success

        val q2 = result.data.first { it.review.mcqId == "mcq2" }
        assertEquals("উদ্দীপক", q2.review.passage?.passageText)
        assertEquals("https://cdn/p.png", q2.review.passage?.passageImage)
        assertNull(result.data.first { it.review.mcqId == "mcq1" }.review.passage)
    }

    @Test
    fun `a failed passage fetch still returns the question without it`() = runTest {
        val repo = FakeExamAttemptRepositoryForEngineTest(fetchPassageResult = ApiResponse.Failure("gone", "NOT_FOUND"))
        repo.questionsWithOptionsResult = ApiResponse.Success(
            FakeExamAttemptRepositoryForEngineTest.defaultQuestionsWithOptions().map { it.copy(passageId = "p1") }
        )

        val result = GetAttemptAnswerSheetUseCase(repo, repo)("attempt1") as ApiResponse.Success

        assertEquals(3, result.data.size)
        assertNull(result.data.first().review.passage)
    }
}
