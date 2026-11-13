package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.ThemeMode
import com.prostuti.student.data.repository.ThemeRepository

class SetThemeModeUseCase(private val repository: ThemeRepository) {
    operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
