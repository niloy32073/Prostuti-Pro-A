package com.prostuti.student.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import com.prostuti.student.data.model.response.CurriculumName

/** The app's language code (e.g. "bn", "en"), for picking one side of a [CurriculumName]. */
@Composable
@ReadOnlyComposable
fun appLanguage(): String = LocalConfiguration.current.locales[0].language

/** This name in the app's current language. */
@Composable
@ReadOnlyComposable
fun CurriculumName.localized(): String = inLanguage(appLanguage())

/** [bn] or [en] by the app's language; a missing or blank [en] falls back to [bn]. */
@Composable
@ReadOnlyComposable
fun localized(bn: String, en: String?): String = CurriculumName(bn, en.orEmpty()).localized()
