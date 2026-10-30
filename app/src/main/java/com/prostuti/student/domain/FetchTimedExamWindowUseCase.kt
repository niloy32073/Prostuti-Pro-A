package com.prostuti.student.domain

import com.prostuti.student.data.repository.TimedExamRepository

/** Returns (windowStartEpochMillis, windowEndEpochMillis). */
class FetchTimedExamWindowUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(examId: String): Pair<Long, Long> =
        repository.resolveWindow(examId)
}
