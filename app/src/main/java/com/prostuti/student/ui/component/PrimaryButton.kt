package com.prostuti.student.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.prostuti.student.ui.theme.ProstutiTypography

private val PillShape = RoundedCornerShape(percent = 50)

/**
 * The one high-emphasis, filled pill action a screen gets (per the design system's Button
 * guidance). While [loading] the button keeps its brand color and swaps its label for a spinner;
 * while disabled (and not loading) it drops to the neutral disabled look at 50% opacity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val clickable = enabled && !loading
    val containerColor: Color
    val contentColor: Color
    val buttonAlpha: Float
    if (enabled) {
        containerColor = MaterialTheme.colorScheme.primary
        contentColor = MaterialTheme.colorScheme.onPrimary
        buttonAlpha = 1f
    } else {
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        buttonAlpha = 0.5f
    }

    Surface(
        onClick = onClick,
        enabled = clickable,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .alpha(buttonAlpha),
        shape = PillShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            if (loading) {
                ProstutiSpinner(
                    modifier = Modifier.size(22.dp),
                    color = contentColor
                )
            } else {
                Text(text = text, style = ProstutiTypography.bnLabelLarge, color = contentColor)
            }
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
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = PillShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
            brush = SolidColor(MaterialTheme.colorScheme.outlineVariant)
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                leadingIcon?.invoke()
                Text(text = text, style = ProstutiTypography.bnLabelLarge)
            }
        }
    }
}
