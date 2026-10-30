package com.prostuti.student.ui.screen.qb.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.prostuti.student.R
import com.prostuti.student.data.model.response.MCQExam
import com.prostuti.student.ui.theme.ProstutiTypography

@Composable
fun ExamDetailDialog(
    exam: MCQExam,
    isAttempting: Boolean,
    onDismiss: () -> Unit,
    onRead: () -> Unit,
    onStart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = exam.titleBn, style = ProstutiTypography.bnTitleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "প্রশ্ন: ${exam.questionCount}টি  •  সময়: ${exam.duration} মিনিট",
                    style = ProstutiTypography.bnBodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "প্রতি প্রশ্নে: ${exam.markPerQuestion} নম্বর  •  ভুলে: -${exam.negativeMarkPerWrongAnswer}",
                    style = ProstutiTypography.bnBodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "সাল: ${exam.examYear}",
                    style = ProstutiTypography.enBodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRead,
                    enabled = !isAttempting,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(text = "প্রশ্ন পড়ো", style = ProstutiTypography.bnBodyMedium)
                }
                Button(
                    onClick = onStart,
                    enabled = !isAttempting,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    if (isAttempting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(text = stringResource(R.string.qb_start_exam), style = ProstutiTypography.bnBodyMedium)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "বাতিল", style = ProstutiTypography.bnBodyMedium)
            }
        },
        properties = DialogProperties(dismissOnClickOutside = !isAttempting)
    )
}
