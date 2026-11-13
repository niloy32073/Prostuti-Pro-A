package com.prostuti.student.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import com.prostuti.student.util.LANGUAGE_BANGLA
import com.prostuti.student.util.toBengaliDigits

/** [value] as text in the app language's digits: Bengali digits in Bangla, Latin in English. */
@Composable
@ReadOnlyComposable
fun localizedNumber(value: Int): String {
    val text = value.toString()
    return if (LocalConfiguration.current.locales[0].language == LANGUAGE_BANGLA) text.toBengaliDigits() else text
}
