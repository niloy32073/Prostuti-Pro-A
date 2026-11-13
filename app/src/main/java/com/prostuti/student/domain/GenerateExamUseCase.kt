package com.prostuti.student.domain

import com.prostuti.student.data.model.request.ExamGenerateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.repository.ExamRepository

class GenerateExamUseCase(private val repository: ExamRepository) {
    suspend operator fun invoke(request: ExamGenerateRequest): ApiResponse<ExamAttemptData> =
        repository.generateExam(request)
}
