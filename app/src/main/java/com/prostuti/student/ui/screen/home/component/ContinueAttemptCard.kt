package com.prostuti.student.ui.screen.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.response.UnfinishedAttemptSummary
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.ui.component.localized

const val CONTINUE_ATTEMPT_CARD_TAG = "home_continue_attempt_card"

/** Home's "continue" card for the newest unfinished exam attempt. */
@Composable
fun ContinueAttemptCard(
    attempt: UnfinishedAttemptSummary,
    remainingSeconds: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modeLabel = stringResource(
        if (attempt.mode == AttemptMode.LIVE) R.string.home_continue_mode_live else R.string.home_continue_mode_practice
    )
    val detail = if (attempt.otherUnfinishedCount > 0) {
        "$modeLabel · " + stringResource(R.string.home_continue_more, attempt.otherUnfinishedCount)
    } else {
        modeLabel
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(CONTINUE_ATTEMPT_CARD_TAG)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_continue_title),
                style = ProstutiTypography.bnLabelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = attempt.titleBn?.let { localized(it, attempt.titleEn) } ?: stringResource(R.string.home_continue_exam_fallback),
                style = ProstutiTypography.bnLabelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = detail,
                style = ProstutiTypography.bnLabelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (remainingSeconds != null) {
            Text(
                text = if (remainingSeconds > 0) {
                    // Round up so the last partial minute still reads "1 min left", not "0".
                    stringResource(R.string.home_continue_minutes_left, (remainingSeconds + 59) / 60)
                } else {
                    stringResource(R.string.home_continue_time_over)
                },
                style = ProstutiTypography.bnLabelMedium,
                color = if (remainingSeconds > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}
