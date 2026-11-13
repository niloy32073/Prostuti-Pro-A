package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamQuestionWithOptions
import com.prostuti.student.data.repository.ExamRepository

/** Every question of an exam with its options and passage — for reading, no attempt is started. */
class GetExamQuestionsWithOptionsUseCase(private val repository: ExamRepository) {
    suspend operator fun invoke(examId: String): ApiResponse<List<ExamQuestionWithOptions>> =
        repository.getExamQuestionsWithOptions(examId)
}
