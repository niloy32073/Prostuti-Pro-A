package com.prostuti.student.domain

import com.prostuti.student.data.repository.StorageRepository
import kotlinx.coroutines.flow.Flow

class GetStringFromDataStoreUseCase(private val repository: StorageRepository) {
    operator fun invoke(key: String): Flow<String?> = repository.getString(key)
}
