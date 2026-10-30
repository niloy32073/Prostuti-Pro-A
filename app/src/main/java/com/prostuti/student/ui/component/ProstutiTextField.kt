package com.prostuti.student.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.prostuti.student.ui.theme.BanglaTextFontFamily
import com.prostuti.student.ui.theme.ProstutiTypography

private val FieldShape = RoundedCornerShape(10.dp)

/**
 * The outlined 48dp field used across the auth flow: label above, rounded 1px-bordered box,
 * optional trailing content (e.g. a password-visibility toggle) and an error message below.
 * Font follows content (TextField README): free text keeps [BanglaTextFontFamily]; a field whose
 * value is inherently Latin/numeric should pass [com.prostuti.student.ui.theme.MonoFontFamily].
 */
@Composable
fun ProstutiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    errorText: String? = null,
    /** Shown in neutral color below the field when there is no error — e.g. password rules. */
    supportingText: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    fontFamily: FontFamily = BanglaTextFontFamily
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (isFocused || isError) 2.dp else 1.dp

    Column(modifier = modifier) {
        Text(
            text = label,
            style = ProstutiTypography.bnLabelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(BorderStroke(borderWidth, borderColor), FieldShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow, FieldShape)
                .alpha(if (enabled) 1f else 0.6f),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = ProstutiTypography.bnBodyMedium.copy(
                    fontFamily = fontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = keyboardOptions,
                visualTransformation = visualTransformation,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.padding(
                            PaddingValues(
                                start = 14.dp,
                                end = if (trailingIcon != null) 44.dp else 14.dp
                            )
                        ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = ProstutiTypography.bnBodyMedium.copy(fontFamily = fontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (trailingIcon != null) {
                Box(modifier = Modifier.padding(end = 4.dp).align(Alignment.CenterEnd)) {
                    trailingIcon()
                }
            }
        }
        if (isError && !errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                style = ProstutiTypography.bnLabelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else if (!supportingText.isNullOrBlank()) {
            Text(
                text = supportingText,
                style = ProstutiTypography.bnLabelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
