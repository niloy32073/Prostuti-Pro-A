package com.prostuti.student.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTypography

/** Default reasons offered in [McqReportDialog]. */
val mcqReportReasons = listOf(
    "উত্তর ভুল আছে",
    "প্রশ্নের তথ্য ভুল",
    "বানান বা ব্যাকরণ ভুল",
    "অন্যান্য সমস্যা"
)

/**
 * Shared "report this MCQ" dialog — a reason radio-group plus an optional free-text
 * description. Used everywhere an MCQ is shown: exam attempts (practice/live/group) and the
 * question-bank read-only screens.
 */
@Composable
fun McqReportDialog(
    onSubmit: (reason: String, description: String) -> Unit,
    onDismiss: () -> Unit,
    reasons: List<String> = mcqReportReasons
) {
    var selectedReason by rememberSaveable { mutableStateOf(reasons.first()) }
    val description = rememberTextFieldState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.mcq_report_dialog_title), style = ProstutiTypography.bnTitleMedium) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.mcq_report_reason_label),
                    style = ProstutiTypography.bnLabelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Column(modifier = Modifier.selectableGroup()) {
                    reasons.forEach { reason ->
                        // The whole row is the tap target; the radio itself only shows the state.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = selectedReason == reason,
                                    onClick = { selectedReason = reason },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedReason == reason, onClick = null)
                            Text(
                                text = reason,
                                style = ProstutiTypography.bnBodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    state = description,
                    label = {
                        Text(
                            text = stringResource(R.string.mcq_report_description_label),
                            style = ProstutiTypography.bnLabelMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 2, maxHeightInLines = 4),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedReason, description.text.toString()) }) {
                Text(text = stringResource(R.string.mcq_report_submit), style = ProstutiTypography.bnBodyMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.mcq_report_cancel), style = ProstutiTypography.bnBodyMedium)
            }
        }
    )
}
