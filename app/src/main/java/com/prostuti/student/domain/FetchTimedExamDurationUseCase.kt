package com.prostuti.student.domain

import com.prostuti.student.data.repository.TimedExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FetchTimedExamDurationUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(examId: String): Int =
        withContext(Dispatchers.IO) { repository.examDurationMinutes(examId) }
}
