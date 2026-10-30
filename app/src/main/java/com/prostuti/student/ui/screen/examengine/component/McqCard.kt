package com.prostuti.student.ui.screen.examengine.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.prostuti.student.data.model.response.Option
import com.prostuti.student.data.model.response.Question
import com.prostuti.student.ui.component.MathText
import com.prostuti.student.ui.component.McqReportDialog
import com.prostuti.student.ui.theme.ProstutiTypography

private val mcqOptionLabels = listOf("ক", "খ", "গ", "ঘ")

/**
 * A single selectable MCQ, used while attempting an exam.
 *
 * Pass [showContainer] = false when this card is nested inside another
 * container (e.g. [PassageCard]) that already draws the surrounding card —
 * the question content is then rendered without its own background/padding.
 */
@Composable
fun McqCard(
    index: Int,
    question: Question,
    selectedOptionId: String?,
    onOptionSelected: (String) -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onReport: (reason: String, description: String) -> Unit,
    modifier: Modifier = Modifier,
    showContainer: Boolean = true
) {
    var showReport by remember { mutableStateOf(false) }

    if (showReport) {
        McqReportDialog(
            onSubmit = { reason, description -> onReport(reason, description); showReport = false },
            onDismiss = { showReport = false }
        )
    }

    val content: @Composable () -> Unit = {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "প্রশ্ন ${index + 1}",
                style = ProstutiTypography.enLabelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = if (isFavorite) "ফেভারিট থেকে সরাও" else "ফেভারিটে যোগ করো",
                    tint = if (isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = { showReport = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "রিপোর্ট করুন",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!question.questionText.isNullOrBlank()) {
            MathText(
                text = question.questionText,
                style = ProstutiTypography.bnMcqQuestion,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            question.options.sortedBy { it.order }.forEachIndexed { optIndex, option ->
                McqOptionItem(
                    label = mcqOptionLabels.getOrElse(optIndex) { (optIndex + 1).toString() },
                    option = option,
                    isSelected = option.id == selectedOptionId,
                    onClick = { onOptionSelected(option.id) }
                )
            }
        }
    }

    if (showContainer) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(16.dp)
        ) {
            content()
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun McqOptionItem(
    label: String,
    option: Option,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant
    val badgeBg = if (isSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.secondaryContainer
    val badgeText = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(badgeBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = ProstutiTypography.bnLabelMedium,
                color = badgeText
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (!option.optionText.isNullOrBlank()) {
            MathText(
                text = option.optionText,
                style = ProstutiTypography.bnMcqOption,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                horizontalPadding = 0.dp
            )
        }
    }
}
