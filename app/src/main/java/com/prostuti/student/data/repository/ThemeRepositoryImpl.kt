package com.prostuti.student.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.prostuti.student.data.model.enums.ThemeMode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

private const val PREFS_NAME = "app_theme_prefs"
private const val KEY_THEME_MODE = "theme_mode"

/**
 * Theme choice in its own SharedPreferences (like the app language in `LocaleManager`), so it
 * survives logout, which clears the DataStore.
 */
class ThemeRepositoryImpl(context: Context) : ThemeRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun read(): ThemeMode =
        prefs.getString(KEY_THEME_MODE, null)?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.SYSTEM

    override fun observeThemeMode(): Flow<ThemeMode> = callbackFlow {
        trySend(read())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_THEME_MODE) trySend(read())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }
}
