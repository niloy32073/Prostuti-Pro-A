package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Chapter
import com.prostuti.student.data.repository.CurriculumRepository
import kotlinx.coroutines.flow.Flow

class FetchChaptersUseCase(private val repository: CurriculumRepository) {
    operator fun invoke(subjectId: String, refresh: Boolean = false): Flow<ApiResponse<List<Chapter>>> =
        repository.observeChapters(subjectId, refresh)
}
