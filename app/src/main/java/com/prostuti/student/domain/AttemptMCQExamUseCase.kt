package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.repository.MCQExamRepository

class AttemptMCQExamUseCase(private val repository: MCQExamRepository) {
    suspend operator fun invoke(examId: String): ApiResponse<ExamAttemptData> =
        repository.attemptMCQExam(examId)
}
