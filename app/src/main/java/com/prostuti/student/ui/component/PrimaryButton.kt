package com.prostuti.student.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prostuti.student.ui.theme.ProstutiTypography

/**
 * The one high-emphasis, filled pill action a screen gets (per the design system's Button
 * guidance). While [loading] the button keeps its brand color and swaps its label for a spinner;
 * while disabled (and not loading) it drops to the neutral disabled look at 50% opacity.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    // A loading button is not clickable, so it uses the disabled colors — kept at the brand color.
    val colors = if (enabled) {
        ButtonDefaults.buttonColors(
            containerColor = colorScheme.primary,
            contentColor = colorScheme.onPrimary,
            disabledContainerColor = colorScheme.primary,
            disabledContentColor = colorScheme.onPrimary
        )
    } else {
        ButtonDefaults.buttonColors(
            disabledContainerColor = colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
            disabledContentColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = colors
    ) {
        if (loading) {
            ProstutiSpinner(modifier = Modifier.size(22.dp), color = colorScheme.onPrimary)
        } else {
            Text(text = text, style = ProstutiTypography.bnLabelLarge)
        }
    }
}

/** A secondary, outlined pill action — e.g. a "Continue with Google" button. */
@Composable
fun OutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(text = text, style = ProstutiTypography.bnLabelLarge)
    }
}
