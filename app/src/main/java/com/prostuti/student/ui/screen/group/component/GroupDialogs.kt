package com.prostuti.student.ui.screen.group.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTypography

/** A yes/no confirmation for group actions. [destructive] paints the confirm button in the error color. */
@Composable
fun GroupConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    /** When set, the user must type this text exactly before confirming (used for deleting a group). */
    typeToConfirm: String? = null,
) {
    var typed by rememberSaveable { mutableStateOf("") }
    val canConfirm = typeToConfirm == null || typed.trim() == typeToConfirm.trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = ProstutiTypography.bnTitleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(body, style = ProstutiTypography.bnBodyMedium)
                if (typeToConfirm != null) {
                    OutlinedTextField(
                        value = typed, onValueChange = { typed = it }, singleLine = true,
                        label = { Text(stringResource(R.string.group_type_name_to_confirm), style = ProstutiTypography.bnBodyMedium) },
                        modifier = Modifier.testTag(TAG_TYPE_TO_CONFIRM)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = canConfirm, modifier = Modifier.testTag(TAG_CONFIRM)) {
                Text(confirmLabel, color = if (destructive && canConfirm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.group_cancel)) } },
    )
}

const val TAG_CONFIRM = "group_confirm"
const val TAG_TYPE_TO_CONFIRM = "group_type_to_confirm"
