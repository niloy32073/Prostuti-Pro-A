package com.prostuti.student.domain

import com.prostuti.student.data.repository.SavedGraphRepository
import com.prostuti.student.data.service.local.SavedGraphEntity

class GetSavedGraphUseCase(private val repository: SavedGraphRepository) {
    suspend operator fun invoke(id: Long): SavedGraphEntity? = repository.get(id)
}
