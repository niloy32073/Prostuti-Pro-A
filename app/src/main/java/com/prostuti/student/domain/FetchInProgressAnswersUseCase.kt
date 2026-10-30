package com.prostuti.student.domain

import com.prostuti.student.data.repository.TimedExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FetchInProgressAnswersUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(examId: String): Map<String, String> =
        withContext(Dispatchers.IO) { repository.inProgressAnswers(examId) }
}
