package com.prostuti.student.domain

import com.prostuti.student.data.repository.StorageRepository

class ClearDataStoreUseCase(private val repository: StorageRepository) {
    suspend operator fun invoke() = repository.clearAll()
}
