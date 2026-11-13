package com.prostuti.student.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTypography

/** "Didn't get the code? XX seconds left" — disabled while [secondsLeft] > 0, tappable at 0. */
@Composable
fun ResendCodeRow(secondsLeft: Int, onResend: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.otp_resend_question),
            style = ProstutiTypography.bnBodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        TextButton(
            onClick = onResend,
            enabled = secondsLeft <= 0,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            )
        ) {
            Text(
                text = if (secondsLeft > 0) {
                    stringResource(R.string.otp_resend_countdown, secondsLeft)
                } else {
                    stringResource(R.string.otp_resend_action)
                },
                style = ProstutiTypography.bnLabelMedium
            )
        }
    }
}
