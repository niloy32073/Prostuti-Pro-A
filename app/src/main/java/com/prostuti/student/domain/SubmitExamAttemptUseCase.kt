package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.data.repository.ExamRepository

class SubmitExamAttemptUseCase(private val repository: ExamRepository) {
    suspend operator fun invoke(attemptId: String): ApiResponse<ExamAttemptResultResponse> =
        repository.submitAttempt(attemptId)
}
