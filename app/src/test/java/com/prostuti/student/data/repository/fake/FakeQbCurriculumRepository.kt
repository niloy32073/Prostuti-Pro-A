package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Chapter
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.model.response.Topic
import com.prostuti.student.data.repository.CurriculumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Test-only fake for [CurriculumRepository], scoped to the qb (question bank) feature's tests. */
class FakeQbCurriculumRepository(
    private val subjectsResult: ApiResponse<List<Subject>> = ApiResponse.Success(emptyList())
) : CurriculumRepository {
    var observeSubjectsCallCount = 0
    var lastRefreshValue: Boolean? = null

    override fun observeSubjects(refresh: Boolean): Flow<ApiResponse<List<Subject>>> {
        observeSubjectsCallCount++
        lastRefreshValue = refresh
        return flowOf(subjectsResult)
    }

    override fun observeChapters(subjectId: String, refresh: Boolean): Flow<ApiResponse<List<Chapter>>> =
        flowOf(ApiResponse.Success(emptyList()))

    override fun observeTopics(chapterId: String, refresh: Boolean): Flow<ApiResponse<List<Topic>>> =
        flowOf(ApiResponse.Success(emptyList()))
}
