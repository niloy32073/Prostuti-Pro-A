package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamResponseAckResponse
import com.prostuti.student.data.repository.ExamRepository

/** Answers already saved on an in-progress attempt — used on resume to restore selections. */
class GetAttemptResponsesUseCase(private val repository: ExamRepository) {
    suspend operator fun invoke(attemptId: String): ApiResponse<List<ExamResponseAckResponse>> =
        repository.getAttemptResponses(attemptId)
}
