package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.repository.ExamRepository

class GetAttemptQuestionsUseCase(private val repository: ExamRepository) {
    suspend operator fun invoke(attemptId: String): ApiResponse<ExamAttemptData> =
        repository.getAttemptQuestions(attemptId)
}
