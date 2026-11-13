package com.prostuti.student.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.prostuti.student.R
import com.prostuti.student.ui.theme.MonoFontFamily
import com.prostuti.student.ui.theme.ProstutiTypography

private const val OTP_LENGTH = 6

/** Rejects any edit that would put a non-digit into the code. */
private object DigitsOnly : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        if (!asCharSequence().all(Char::isDigit)) revertAllChanges()
    }
}

/** The 6-digit verification code field — centered, mono, wide letter-spacing. */
@Composable
fun OtpCodeField(
    state: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.otp_placeholder),
    isError: Boolean = false,
    errorText: String? = null
) {
    OutlinedTextField(
        state = state,
        modifier = modifier.fillMaxWidth(),
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = TextStyle(
            fontFamily = MonoFontFamily,
            fontSize = 20.sp,
            letterSpacing = 0.3.em,
            textAlign = TextAlign.Center
        ),
        labelPosition = TextFieldLabelPosition.Above(),
        label = { Text(text = label, style = ProstutiTypography.bnLabelLarge) },
        placeholder = {
            Text(
                text = placeholder,
                fontFamily = MonoFontFamily,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        inputTransformation = InputTransformation.maxLength(OTP_LENGTH).then(DigitsOnly),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        isError = isError,
        supportingText = if (isError && !errorText.isNullOrBlank()) {
            { Text(text = errorText, style = ProstutiTypography.bnLabelMedium) }
        } else null,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            errorBorderColor = MaterialTheme.colorScheme.error,
            focusedLabelColor = MaterialTheme.colorScheme.onSurface,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
            errorLabelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
