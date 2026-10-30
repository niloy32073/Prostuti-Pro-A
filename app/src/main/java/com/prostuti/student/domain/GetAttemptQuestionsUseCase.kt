package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.repository.MCQExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAttemptQuestionsUseCase(private val repository: MCQExamRepository) {
    suspend operator fun invoke(attemptId: String): ApiResponse<ExamAttemptData> =
        withContext(Dispatchers.IO) { repository.getAttemptQuestions(attemptId) }
}
