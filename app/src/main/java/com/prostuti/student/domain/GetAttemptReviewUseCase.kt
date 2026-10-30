package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptReviewData
import com.prostuti.student.data.repository.MCQExamRepository

class GetAttemptReviewUseCase(private val repository: MCQExamRepository) {
    suspend operator fun invoke(attemptId: String): ApiResponse<ExamAttemptReviewData> =
        repository.getAttemptReview(attemptId)
}
