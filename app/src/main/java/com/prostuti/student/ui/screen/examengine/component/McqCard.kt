package com.prostuti.student.ui.screen.examengine.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.prostuti.student.R
import com.prostuti.student.data.model.response.Option
import com.prostuti.student.data.model.response.Question
import com.prostuti.student.ui.component.MathText
import com.prostuti.student.ui.component.McqReportDialog
import com.prostuti.student.ui.theme.ProstutiTypography

private val mcqOptionLabels = listOf("ক", "খ", "গ", "ঘ")

/** Options the student can no longer pick are faded. */
internal const val DISABLED_OPTION_ALPHA = 0.45f

/**
 * A single selectable MCQ, used while attempting an exam.
 *
 * Pass [showContainer] = false when this card is nested inside another
 * container (e.g. [PassageCard]) that already draws the surrounding card —
 * the question content is then rendered without its own background/padding.
 *
 * Pass [readOnly] = true to only show the question (e.g. the question reader):
 * options are not tappable and are not faded.
 *
 * Answer row (reader only): pass [onToggleAnswer] to show the "উত্তর দেখাও" button;
 * while [isAnswerRevealed], options with `isCorrect == true` are highlighted.
 * Pass [onShowExplanation] to show the "ব্যাখ্যা দেখুন" button.
 *
 * [isReported]: the user already reported this MCQ — the flag shows red and can't be tapped
 * again (reports are final; moderators handle them).
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
    showContainer: Boolean = true,
    readOnly: Boolean = false,
    isAnswerRevealed: Boolean = false,
    onToggleAnswer: (() -> Unit)? = null,
    onShowExplanation: (() -> Unit)? = null,
    isReported: Boolean = false
) {
    var showReport by rememberSaveable { mutableStateOf(false) }

    if (showReport && !isReported) {
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
            IconButton(
                onClick = { showReport = true },
                enabled = !isReported,
                modifier = Modifier.size(32.dp).testTag(MCQ_REPORT_BUTTON_TAG + question.mcqId)
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = stringResource(if (isReported) R.string.mcq_reported else R.string.mcq_report),
                    tint = if (isReported) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
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
        if (!question.questionImageLink.isNullOrBlank()) {
            McqImage(
                imageUrl = question.questionImageLink,
                contentDescription = "প্রশ্নের ছবি",
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .testTag(MCQ_QUESTION_IMAGE_TAG)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            question.options.sortedBy { it.order }.forEachIndexed { optIndex, option ->
                McqOptionItem(
                    label = mcqOptionLabels.getOrElse(optIndex) { (optIndex + 1).toString() },
                    option = option,
                    isSelected = option.id == selectedOptionId || (isAnswerRevealed && option.isCorrect == true),
                    // One pick per question: once answered, every option is locked.
                    enabled = !readOnly && selectedOptionId == null,
                    fadeWhenDisabled = !readOnly,
                    onClick = { onOptionSelected(option.id) }
                )
            }
        }

        if (isAnswerRevealed && question.options.none { it.isCorrect == true }) {
            Text(
                text = stringResource(R.string.qb_reader_answer_unavailable),
                style = ProstutiTypography.bnLabelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (onToggleAnswer != null || onShowExplanation != null) {
            McqAnswerActions(
                mcqId = question.mcqId,
                isAnswerRevealed = isAnswerRevealed,
                onToggleAnswer = onToggleAnswer,
                onShowExplanation = onShowExplanation
            )
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
    enabled: Boolean,
    fadeWhenDisabled: Boolean,
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
            .alpha(if (enabled || isSelected || !fadeWhenDisabled) 1f else DISABLED_OPTION_ALPHA)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
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

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!option.optionText.isNullOrBlank()) {
                MathText(
                    text = option.optionText,
                    style = ProstutiTypography.bnMcqOption,
                    color = MaterialTheme.colorScheme.onSurface,
                    horizontalPadding = 0.dp
                )
            }
            if (!option.optionImageLink.isNullOrBlank()) {
                McqImage(
                    imageUrl = option.optionImageLink,
                    contentDescription = "অপশন $label-এর ছবি",
                    modifier = Modifier.testTag(MCQ_OPTION_IMAGE_TAG)
                )
            }
        }
    }
}

/** Bottom row of a reader card: explanation button on the left, answer toggle on the right. */
@Composable
private fun McqAnswerActions(
    mcqId: String,
    isAnswerRevealed: Boolean,
    onToggleAnswer: (() -> Unit)?,
    onShowExplanation: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onShowExplanation != null) {
            TextButton(
                onClick = onShowExplanation,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.testTag(MCQ_EXPLANATION_BUTTON_TAG + mcqId)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.qb_reader_show_explanation),
                    style = ProstutiTypography.bnLabelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (onToggleAnswer != null) {
            OutlinedButton(
                onClick = onToggleAnswer,
                shape = RoundedCornerShape(50.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.testTag(MCQ_ANSWER_TOGGLE_TAG + mcqId)
            ) {
                Text(
                    text = stringResource(if (isAnswerRevealed) R.string.bookmark_hide_answer else R.string.bookmark_show_answer),
                    style = ProstutiTypography.bnLabelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** A question/option picture at full width and its own aspect ratio; a status box while it loads or if it fails. */
@Composable
internal fun McqImage(imageUrl: String, contentDescription: String, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = imageUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.FillWidth,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        loading = { McqImageStatus(text = "ছবি লোড হচ্ছে…") },
        error = { McqImageStatus(text = "ছবি লোড করা যায়নি") }
    )
}

@Composable
private fun McqImageStatus(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = ProstutiTypography.bnLabelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

const val MCQ_QUESTION_IMAGE_TAG = "mcq_question_image"
const val MCQ_OPTION_IMAGE_TAG = "mcq_option_image"
const val MCQ_ANSWER_TOGGLE_TAG = "mcq_answer_toggle_"
const val MCQ_EXPLANATION_BUTTON_TAG = "mcq_explanation_"
const val MCQ_REPORT_BUTTON_TAG = "mcq_report_"
