package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.enums.ThemeMode
import com.prostuti.student.data.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory theme choice; [setThemeMode] updates observers like the real SharedPreferences one. */
class FakeThemeRepository(initial: ThemeMode = ThemeMode.SYSTEM) : ThemeRepository {
    val mode = MutableStateFlow(initial)
    override fun observeThemeMode(): Flow<ThemeMode> = mode
    override fun setThemeMode(mode: ThemeMode) { this.mode.value = mode }
}
