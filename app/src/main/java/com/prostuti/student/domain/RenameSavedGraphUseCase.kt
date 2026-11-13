package com.prostuti.student.domain

import com.prostuti.student.data.repository.SavedGraphRepository

class RenameSavedGraphUseCase(private val repository: SavedGraphRepository) {
    suspend operator fun invoke(id: Long, name: String) {
        if (name.isNotBlank()) repository.rename(id, name.trim())
    }
}
