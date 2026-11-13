package com.prostuti.student.domain

import com.prostuti.student.data.repository.SavedGraphRepository

class DeleteSavedGraphUseCase(private val repository: SavedGraphRepository) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
