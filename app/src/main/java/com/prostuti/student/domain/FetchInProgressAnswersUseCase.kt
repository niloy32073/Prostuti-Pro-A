package com.prostuti.student.domain

import com.prostuti.student.data.repository.TimedExamRepository

class FetchInProgressAnswersUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(examId: String): Map<String, String> =
        repository.inProgressAnswers(examId)
}
