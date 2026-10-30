package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.repository.MCQExamRepository

class GetAttemptQuestionsUseCase(private val repository: MCQExamRepository) {
    suspend operator fun invoke(attemptId: String): ApiResponse<ExamAttemptData> =
        repository.getAttemptQuestions(attemptId)
}
