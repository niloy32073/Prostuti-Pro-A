package com.prostuti.student.domain

import com.prostuti.student.data.repository.SavedGraphRepository

class SaveGraphUseCase(private val repository: SavedGraphRepository) {
    /** Returns the saved graph's id (new for a first save, the same one afterwards). */
    suspend operator fun invoke(id: Long?, name: String, content: String, preview: String, isSpace: Boolean): Long =
        repository.save(id, name.trim(), content, preview, isSpace)
}
