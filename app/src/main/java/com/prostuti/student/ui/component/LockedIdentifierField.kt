package com.prostuti.student.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTypography

/**
 * The read-only "identifier already chosen" row shown on Sign in/up and Reset — a locked value
 * plus a "change" link back to [onChangeClick].
 */
@Composable
fun LockedIdentifierField(
    label: String,
    value: String,
    onChangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Read-only, so the state only ever holds [value]; the label-above layout needs the state-based field.
    val textState = remember(value) { TextFieldState(value) }
    OutlinedTextField(
        state = textState,
        modifier = modifier.fillMaxWidth(),
        readOnly = true,
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = ProstutiTypography.bnBodyMedium,
        labelPosition = TextFieldLabelPosition.Above(),
        label = { Text(text = label, style = ProstutiTypography.bnLabelLarge) },
        trailingIcon = {
            TextButton(
                onClick = onChangeClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text(
                    text = stringResource(R.string.change),
                    style = ProstutiTypography.bnLabelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        },
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor = MaterialTheme.colorScheme.onSurface,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
