package com.prostuti.student.domain.group

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.repository.GroupRepository

/** Joins a public or private group with its code; membership is immediate. */
class JoinGroupByCodeUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(code: String): ApiResponse<StudyGroup> = repository.joinByCode(code.trim().uppercase())
}
