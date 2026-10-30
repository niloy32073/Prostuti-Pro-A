package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.data.repository.MCQExamRepository

class ListMyExamAttemptsUseCase(private val repository: MCQExamRepository) {
    suspend operator fun invoke(examId: String? = null, page: Int = 1, limit: Int = 20): ApiResponse<List<ExamAttemptResultResponse>> =
        repository.listMyAttempts(examId, page, limit)
}
