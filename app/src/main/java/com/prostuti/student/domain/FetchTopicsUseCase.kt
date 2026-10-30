package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Topic
import com.prostuti.student.data.repository.CurriculumRepository
import kotlinx.coroutines.flow.Flow

class FetchTopicsUseCase(private val repository: CurriculumRepository) {
    operator fun invoke(chapterId: String, refresh: Boolean = false): Flow<ApiResponse<List<Topic>>> =
        repository.observeTopics(chapterId, refresh)
}
