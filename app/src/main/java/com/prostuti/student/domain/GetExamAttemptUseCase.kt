package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.data.repository.MCQExamRepository

class GetExamAttemptUseCase(private val repository: MCQExamRepository) {
    suspend operator fun invoke(attemptId: String): ApiResponse<ExamAttemptResultResponse> =
        repository.getAttempt(attemptId)
}
