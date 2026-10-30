package com.prostuti.student.domain

import com.prostuti.student.data.repository.TimedExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Returns (windowStartEpochMillis, windowEndEpochMillis). */
class FetchTimedExamWindowUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(examId: String): Pair<Long, Long> =
        withContext(Dispatchers.IO) { repository.resolveWindow(examId) }
}
