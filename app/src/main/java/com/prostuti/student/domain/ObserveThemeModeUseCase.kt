package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.ThemeMode
import com.prostuti.student.data.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow

class ObserveThemeModeUseCase(private val repository: ThemeRepository) {
    operator fun invoke(): Flow<ThemeMode> = repository.observeThemeMode()
}
