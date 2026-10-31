package com.prostuti.student.domain

import com.prostuti.student.data.repository.AttemptProgressRepository

class GetLocalAttemptElapsedUseCase(private val repository: AttemptProgressRepository) {
    suspend operator fun invoke(attemptId: String): Long? = repository.getElapsedMs(attemptId)
}
