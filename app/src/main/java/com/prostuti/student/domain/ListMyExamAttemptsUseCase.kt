package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.data.repository.ExamRepository

class ListMyExamAttemptsUseCase(private val repository: ExamRepository) {
    suspend operator fun invoke(
        examId: String? = null,
        unfinished: Boolean = false,
        page: Int = 1,
        limit: Int = 20
    ): ApiResponse<List<ExamAttemptResultResponse>> = repository.listMyAttempts(examId, unfinished, page, limit)
}
