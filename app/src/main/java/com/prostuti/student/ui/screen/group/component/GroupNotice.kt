package com.prostuti.student.ui.screen.group.component

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.prostuti.student.util.DhakaTime
import com.prostuti.student.util.LANGUAGE_BANGLA
import com.prostuti.student.util.toBengaliDigits
import java.time.format.DateTimeFormatter

/**
 * A one-shot confirmation a group ViewModel asks the screen to show in a snackbar, as a string
 * resource plus its argument — ViewModels hold no Context, so they can't resolve text themselves.
 */
data class GroupNotice(@param:StringRes val res: Int, val arg: String? = null)

/** "2 Nov, 4:00 PM" / "২ নভেম্বর, ৪:০০ PM" in Dhaka time for an API timestamp; the raw text if it can't be read. */
@Composable
fun groupDateTimeText(apiTimestamp: String): String {
    val locale = LocalConfiguration.current.locales[0]
    val instant = DhakaTime.parseInstant(apiTimestamp) ?: return apiTimestamp
    val text = DateTimeFormatter.ofPattern("d MMMM, h:mm a", locale).withZone(DhakaTime.ZONE).format(instant)
    return if (locale.language == LANGUAGE_BANGLA) text.toBengaliDigits() else text
}
