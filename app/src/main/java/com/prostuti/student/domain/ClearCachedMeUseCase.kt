package com.prostuti.student.domain

import com.prostuti.student.data.repository.UserRepository

class ClearCachedMeUseCase(private val repository: UserRepository) {
    suspend operator fun invoke() = repository.clearCachedMe()
}
