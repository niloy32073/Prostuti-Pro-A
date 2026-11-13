package com.prostuti.student.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.prostuti.student.R
import com.prostuti.student.ui.theme.BanglaTextFontFamily
import com.prostuti.student.ui.theme.ProstutiTypography

/** Outlined secure password field with a show/hide trailing toggle. */
@Composable
fun PasswordField(
    state: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    errorText: String? = null,
    supportingText: String? = null
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedSecureTextField(
        state = state,
        modifier = modifier.fillMaxWidth(),
        textStyle = ProstutiTypography.bnBodyMedium.copy(fontFamily = BanglaTextFontFamily),
        labelPosition = TextFieldLabelPosition.Above(),
        label = { Text(text = label, style = ProstutiTypography.bnLabelLarge) },
        placeholder = if (placeholder.isNotEmpty()) {
            {
                Text(
                    text = placeholder,
                    style = ProstutiTypography.bnBodyMedium.copy(fontFamily = BanglaTextFontFamily)
                )
            }
        } else null,
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = stringResource(if (visible) R.string.password_hide else R.string.password_show),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        supportingText = if (isError && !errorText.isNullOrBlank()) {
            { Text(text = errorText, style = ProstutiTypography.bnLabelMedium) }
        } else if (!supportingText.isNullOrBlank()) {
            { Text(text = supportingText, style = ProstutiTypography.bnLabelMedium) }
        } else null,
        isError = isError,
        textObfuscationMode = if (visible) TextObfuscationMode.Visible else TextObfuscationMode.Hidden,
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
