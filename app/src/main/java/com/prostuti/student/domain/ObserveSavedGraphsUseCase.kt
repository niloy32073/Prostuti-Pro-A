package com.prostuti.student.domain

import com.prostuti.student.data.repository.SavedGraphRepository
import com.prostuti.student.data.service.local.SavedGraphEntity
import kotlinx.coroutines.flow.Flow

class ObserveSavedGraphsUseCase(private val repository: SavedGraphRepository) {
    operator fun invoke(): Flow<List<SavedGraphEntity>> = repository.observeAll()
}
