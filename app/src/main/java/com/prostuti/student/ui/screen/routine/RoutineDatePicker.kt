package com.prostuti.student.ui.screen.routine

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import java.time.LocalDate

private const val DAY_MILLIS = 86_400_000L

/**
 * A Material date picker limited to [min]..[max] (Dhaka dates; [max] null = no end). The picker works
 * in UTC-midnight millis, so LocalDate <-> epoch day keeps it free of time zones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDatePickerDialog(initial: LocalDate, min: LocalDate, max: LocalDate? = null, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toEpochDay() * DAY_MILLIS,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val day = utcTimeMillis / DAY_MILLIS
                return day >= min.toEpochDay() && (max == null || day <= max.toEpochDay())
            }
            override fun isSelectableYear(year: Int) = year >= min.year && (max == null || year <= max.year)
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(LocalDate.ofEpochDay(it / DAY_MILLIS)) }
                onDismiss()
            }) { Text(stringResource(R.string.routine_date_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.routine_date_cancel)) } }
    ) {
        DatePicker(state = state, title = { Text(stringResource(R.string.routine_pick_date), modifier = Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}
