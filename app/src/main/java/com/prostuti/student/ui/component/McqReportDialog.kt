package com.prostuti.student.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var description by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "সমস্যা রিপোর্ট করুন", style = ProstutiTypography.bnTitleMedium) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "সমস্যার ধরন বেছে নিন",
                    style = ProstutiTypography.bnLabelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(
                            text = reason,
                            style = ProstutiTypography.bnBodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = {
                        Text(
                            text = "বিস্তারিত লিখুন (ঐচ্ছিক)",
                            style = ProstutiTypography.bnLabelMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedReason, description) }, shape = RoundedCornerShape(50.dp)) {
                Text(text = "সাবমিট", style = ProstutiTypography.bnBodyMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "বাতিল", style = ProstutiTypography.bnBodyMedium)
            }
        }
    )
}
