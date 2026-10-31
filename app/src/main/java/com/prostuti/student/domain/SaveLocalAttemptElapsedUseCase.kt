package com.prostuti.student.domain

import com.prostuti.student.data.repository.AttemptProgressRepository

class SaveLocalAttemptElapsedUseCase(private val repository: AttemptProgressRepository) {
    suspend operator fun invoke(attemptId: String, elapsedMs: Long) = repository.saveElapsedMs(attemptId, elapsedMs)
}
