package com.prostuti.student.domain

import com.prostuti.student.data.repository.StorageRepository

class SaveBooleanToDataStoreUseCase(private val repository: StorageRepository) {
    suspend operator fun invoke(key: String, value: Boolean) = repository.putBoolean(key, value)
}
