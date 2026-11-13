package com.prostuti.student.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.prostuti.student.ui.theme.BanglaTextFontFamily
import com.prostuti.student.ui.theme.ProstutiTypography

/**
 * The outlined field used across the auth flow: label above, rounded bordered box,
 * optional trailing content (e.g. a password-visibility toggle) and an error/supporting
 * message below.
 * Font follows content: free text keeps [BanglaTextFontFamily]; a field whose value is
 * inherently Latin/numeric should pass [com.prostuti.student.ui.theme.MonoFontFamily].
 */
@Composable
fun ProstutiTextField(
    state: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    errorText: String? = null,
    /** Shown in neutral color below the field when there is no error — e.g. password rules. */
    supportingText: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
    fontFamily: FontFamily = BanglaTextFontFamily
) {
    OutlinedTextField(
        state = state,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = ProstutiTypography.bnBodyMedium.copy(fontFamily = fontFamily),
        labelPosition = TextFieldLabelPosition.Above(),
        label = { Text(text = label, style = ProstutiTypography.bnLabelLarge) },
        placeholder = if (placeholder.isNotEmpty()) {
            {
                Text(
                    text = placeholder,
                    style = ProstutiTypography.bnBodyMedium.copy(fontFamily = fontFamily)
                )
            }
        } else null,
        trailingIcon = trailingIcon,
        supportingText = if (isError && !errorText.isNullOrBlank()) {
            { Text(text = errorText, style = ProstutiTypography.bnLabelMedium) }
        } else if (!supportingText.isNullOrBlank()) {
            { Text(text = supportingText, style = ProstutiTypography.bnLabelMedium) }
        } else null,
        isError = isError,
        keyboardOptions = keyboardOptions,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            errorBorderColor = MaterialTheme.colorScheme.error,
            // The label sits above the field and keeps the old fixed color in every state.
            focusedLabelColor = MaterialTheme.colorScheme.onSurface,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
            errorLabelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
