package com.prostuti.student.domain

import com.prostuti.student.data.repository.TimedExamRepository

class FetchTimedExamDurationUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(examId: String): Int =
        repository.examDurationMinutes(examId)
}
