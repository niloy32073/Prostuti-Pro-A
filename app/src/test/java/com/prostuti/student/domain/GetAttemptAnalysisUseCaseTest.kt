package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.AttemptAnalysisData
import com.prostuti.student.data.model.response.Chapter
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.model.response.Topic
import com.prostuti.student.data.model.response.TopicAnalysisItem
import com.prostuti.student.data.repository.CurriculumRepository
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.prostuti.student.data.model.response.CurriculumName

class GetAttemptAnalysisUseCaseTest {

    /** Default answer sheet: mcq1 → t1, mcq2 → t2, mcq3 → no topic; all chapter c1 of subject s1. */
    private val examRepo = FakeExamAttemptRepositoryForEngineTest(
        analysisResult = ApiResponse.Success(
            AttemptAnalysisData(
                attemptId = "attempt1", examId = "exam1",
                topics = listOf(
                    TopicAnalysisItem("t2", totalQuestions = 1, correct = 0, wrong = 1, skipped = 0),
                    TopicAnalysisItem("t1", totalQuestions = 1, correct = 1, wrong = 0, skipped = 0),
                    TopicAnalysisItem(null, totalQuestions = 1, correct = 0, wrong = 0, skipped = 1)
                )
            )
        )
    )

    private class Curriculum(private val failTopics: Boolean = false) : CurriculumRepository {
        val requestedChapters = mutableListOf<String>()
        override fun observeSubjects(refresh: Boolean): Flow<ApiResponse<List<Subject>>> = flowOf(ApiResponse.Success(emptyList()))
        override fun observeChapters(subjectId: String, refresh: Boolean): Flow<ApiResponse<List<Chapter>>> =
            flowOf(ApiResponse.Success(listOf(Chapter("c1", subjectId, "গতিবিদ্যা", "Kinematics", 1, "2026-01-01", createdBy = "x"))))
        override fun observeTopics(chapterId: String, refresh: Boolean): Flow<ApiResponse<List<Topic>>> {
            requestedChapters += chapterId
            return if (failTopics) flowOf(ApiResponse.Failure("offline", "NETWORK_ERROR"))
            else flowOf(
                ApiResponse.Success(
                    listOf(
                        Topic("t1", chapterId, "সমত্বরণ", "Acceleration", 1, "2026-01-01", createdBy = "x"),
                        Topic("t2", chapterId, "প্রক্ষেপণ", "Projectile", 2, "2026-01-01", createdBy = "x")
                    )
                )
            )
        }
    }

    @Test
    fun `resolves topic and chapter names and keeps server order`() = runTest {
        val curriculum = Curriculum()
        val result = GetAttemptAnalysisUseCase(examRepo, curriculum)("attempt1")

        result as ApiResponse.Success
        assertEquals(listOf("t2", "t1", null), result.data.map { it.topicId })
        assertEquals(CurriculumName("প্রক্ষেপণ", "Projectile"), result.data[0].topicName)
        assertEquals(CurriculumName("গতিবিদ্যা", "Kinematics"), result.data[0].chapterName)
        assertNull(result.data[2].topicName)
        assertEquals(listOf("c1"), curriculum.requestedChapters)   // each chapter fetched once
    }

    @Test
    fun `curriculum failure still returns the analysis without names`() = runTest {
        val result = GetAttemptAnalysisUseCase(examRepo, Curriculum(failTopics = true))("attempt1")

        result as ApiResponse.Success
        assertEquals(3, result.data.size)
        assertTrue(result.data.all { it.topicName == null })
    }

    @Test
    fun `analysis failure is returned as is`() = runTest {
        examRepo.analysisResult = ApiResponse.Failure("This exam was missed", "ATTEMPT_MISSED")

        val result = GetAttemptAnalysisUseCase(examRepo, Curriculum())("attempt1")

        assertEquals("ATTEMPT_MISSED", (result as ApiResponse.Failure).errorCode)
    }

    @Test
    fun `correctPercent rounds down and is 0 for an empty topic`() {
        assertEquals(66, TopicAnalysisRow("t", null, null, total = 3, correct = 2, wrong = 1, skipped = 0).correctPercent)
        assertEquals(0, TopicAnalysisRow("t", null, null, total = 0, correct = 0, wrong = 0, skipped = 0).correctPercent)
    }
}
