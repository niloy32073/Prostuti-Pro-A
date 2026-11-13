package com.prostuti.student.ui.screen.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.screen.home.HomeGroupExam
import com.prostuti.student.ui.screen.routine.slot.phase
import com.prostuti.student.ui.screen.routine.slot.slotStatusText
import com.prostuti.student.ui.theme.ProstutiTypography
import java.time.Instant

const val TAG_HOME_GROUP_EXAM = "home_group_exam"

/** Home's real group-exam card, the same size as [ExamPreviewCardView]: "{group} · {title}" and the window. */
@Composable
fun GroupExamPreviewCard(exam: HomeGroupExam, now: Instant, onClick: () -> Unit) {
    val slot = exam.slot
    Column(
        modifier = Modifier.width(260.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(16.dp).testTag(TAG_HOME_GROUP_EXAM),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.home_group_exam_today_badge), style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            listOfNotNull(exam.groupName, slot.title).joinToString(" · "), style = ProstutiTypography.bnTitleMedium,
            color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        Text(slotStatusText(slot, slot.phase(now), now), style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
