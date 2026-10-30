package com.prostuti.student.domain

import com.prostuti.student.data.repository.StorageRepository
import kotlinx.coroutines.flow.Flow

class GetBooleanFromDataStoreUseCase(private val repository: StorageRepository) {
    operator fun invoke(key: String): Flow<Boolean?> = repository.getBoolean(key)
}
