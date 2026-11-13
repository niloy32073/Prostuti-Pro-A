package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow

class ObserveBookmarkedIdsUseCase(private val repository: BookmarkRepository) {
    operator fun invoke(type: BookmarkResourceType): Flow<Set<String>> = repository.observeBookmarkedIds(type)
}
