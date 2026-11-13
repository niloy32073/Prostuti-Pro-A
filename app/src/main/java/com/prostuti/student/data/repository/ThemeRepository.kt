package com.prostuti.student.data.repository

import com.prostuti.student.data.model.enums.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    fun setThemeMode(mode: ThemeMode)
}
