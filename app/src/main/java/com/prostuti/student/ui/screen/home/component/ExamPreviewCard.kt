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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.ui.screen.home.ExamPreviewCard
import com.prostuti.student.ui.theme.ProstutiTypography

@Composable
fun ExamPreviewCardView(card: ExamPreviewCard, onClick: () -> Unit) {
    val containerColor = if (card.isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow
    val onColor = if (card.isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val variantColor = if (card.isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .width(260.dp)
            .background(containerColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(card.badgeRes),
            style = ProstutiTypography.enLabelMedium,
            color = variantColor
        )
        Text(
            text = stringResource(card.titleRes),
            style = ProstutiTypography.bnTitleMedium,
            color = onColor
        )
        Text(
            text = stringResource(card.subtitleRes),
            style = ProstutiTypography.bnLabelMedium,
            color = variantColor
        )
    }
}
