package com.prostuti.student.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTypography

/** Asks before the app closes from a top-level tab: [onExit] closes it, [onDismiss] stays. */
@Composable
fun ExitAppDialog(onExit: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.main_exit_title),
                style = ProstutiTypography.bnTitleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = stringResource(R.string.main_exit_message),
                style = ProstutiTypography.bnBodyMedium,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(onClick = onExit) {
                Text(text = stringResource(R.string.main_exit_confirm), style = ProstutiTypography.bnLabelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.main_exit_stay), style = ProstutiTypography.bnLabelLarge)
            }
        }
    )
}
