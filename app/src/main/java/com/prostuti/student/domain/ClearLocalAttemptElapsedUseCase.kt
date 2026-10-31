package com.prostuti.student.domain

import com.prostuti.student.data.repository.AttemptProgressRepository

class ClearLocalAttemptElapsedUseCase(private val repository: AttemptProgressRepository) {
    suspend operator fun invoke(attemptId: String) = repository.clear(attemptId)
}
