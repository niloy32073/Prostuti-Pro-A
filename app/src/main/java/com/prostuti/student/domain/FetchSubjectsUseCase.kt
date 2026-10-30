package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.repository.CurriculumRepository
import kotlinx.coroutines.flow.Flow

class FetchSubjectsUseCase(private val repository: CurriculumRepository) {
    operator fun invoke(refresh: Boolean = false): Flow<ApiResponse<List<Subject>>> =
        repository.observeSubjects(refresh)
}
