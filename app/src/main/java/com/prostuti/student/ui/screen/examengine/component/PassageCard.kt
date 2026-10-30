package com.prostuti.student.ui.screen.examengine.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.ui.component.MathText
import com.prostuti.student.ui.theme.ProstutiTypography

/**
 * Wraps one or more MCQs that share a common passage: renders the passage
 * text/image once at the top, then [content] (the grouped MCQs) below it,
 * all inside a single card.
 */
@Composable
fun PassageCard(
    passage: FetchPassageData?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (passage != null && (!passage.passageText.isNullOrBlank() || passage.passageImage != null)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "অনুচ্ছেদ",
                    style = ProstutiTypography.enLabelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (passage.passageImage != null) {
                    // TODO: replace with AsyncImage(coil3) once Coil is added as a dependency
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ছবি লোড হচ্ছে…",
                            style = ProstutiTypography.bnLabelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!passage.passageText.isNullOrBlank()) {
                    MathText(
                        text = passage.passageText,
                        style = ProstutiTypography.bnBodyMedium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        content()
    }
}
