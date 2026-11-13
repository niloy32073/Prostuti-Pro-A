package com.prostuti.student.ui.screen.examengine.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTypography
import androidx.lifecycle.compose.dropUnlessResumed

/**
 * Shown when the exam time runs out. The attempt submits on its own, so this dialog can't be
 * dismissed: it says the answers are being submitted, then offers the result once [isSubmitted]
 * (or a retry when the automatic submit failed).
 */
@Composable
fun TimeUpDialog(
    isSubmitting: Boolean,
    isSubmitted: Boolean,
    onViewResult: () -> Unit,
    onRetry: () -> Unit
) {
    val isFailed = !isSubmitting && !isSubmitted
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(text = stringResource(R.string.exam_time_up_title), style = ProstutiTypography.bnTitleMedium) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isSubmitting) ProstutiSpinner(modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(
                        when {
                            isSubmitted -> R.string.exam_time_up_submitted
                            isFailed -> R.string.exam_time_up_failed
                            else -> R.string.exam_time_up_submitting
                        }
                    ),
                    style = ProstutiTypography.bnBodyMedium
                )
            }
        },
        confirmButton = {
            if (isSubmitted) {
                Button(onClick = dropUnlessResumed { onViewResult() }, shape = RoundedCornerShape(50.dp)) {
                    Text(text = stringResource(R.string.exam_time_up_view_result), style = ProstutiTypography.bnBodyMedium)
                }
            } else if (isFailed) {
                Button(onClick = onRetry, shape = RoundedCornerShape(50.dp)) {
                    Text(text = stringResource(R.string.exam_time_up_retry), style = ProstutiTypography.bnBodyMedium)
                }
            }
        }
    )
}
