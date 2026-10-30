package com.prostuti.student.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

const val LANGUAGE_BANGLA = "bn"
const val LANGUAGE_ENGLISH = "en"

private const val PREFS_NAME = "app_locale_prefs"
private const val KEY_LANGUAGE = "language_code"

/**
 * Forces the app's language independently of the device's system locale — this app's own
 * default is always Bangla, regardless of what language the phone is set to, until the user
 * explicitly switches. Backed by plain SharedPreferences (not the DataStore used elsewhere)
 * because [Activity.attachBaseContext] runs synchronously, before Koin/DataStore are usable.
 */
object LocaleManager {
    fun getLanguage(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, LANGUAGE_BANGLA) ?: LANGUAGE_BANGLA

    fun setLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    /** Wrap a base [Context] with the saved language before it's used to load resources. */
    fun wrapContext(context: Context): Context {
        val locale = Locale.Builder().setLanguage(getLanguage(context)).build()
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    /** Persists the new language and recreates [activity] so its resources reload with it. */
    fun changeLanguage(activity: Activity, languageCode: String) {
        setLanguage(activity, languageCode)
        activity.recreate()
    }
}
