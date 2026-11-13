package com.prostuti.student.ui.screen.bookmark.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.data.model.response.McqDetail
import com.prostuti.student.data.model.response.McqDetailOption
import com.prostuti.student.data.service.local.BookmarkEntity
import com.prostuti.student.ui.component.MathText
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.util.formatApiDateTime

const val BOOKMARK_CARD_TAG = "bookmark_card_"
const val BOOKMARK_REMOVE_TAG = "bookmark_remove_"
const val BOOKMARK_ANSWER_TOGGLE_TAG = "bookmark_answer_toggle_"

private val optionLabels = listOf("ক", "খ", "গ", "ঘ")

/** Same pink as the heart on the attempt and question-bank cards. */
private val BookmarkHeartColor = Color(0xFFE91E63)

/**
 * One bookmarked MCQ. [mcq] is null while its question downloads ([failed] = the download
 * failed). "Show answer" grows the card in place with the correct option and the explanation.
 */
@Composable
fun BookmarkedMcqCard(
    index: Int,
    bookmark: BookmarkEntity,
    mcq: McqDetail?,
    failed: Boolean,
    isAnswerRevealed: Boolean,
    onToggleAnswer: () -> Unit,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .animateContentSize()
            .padding(16.dp)
            .testTag(BOOKMARK_CARD_TAG + bookmark.resourceId)
    ) {
        CardHeader(index = index, bookmark = bookmark, onRemove = onRemove)
        Spacer(modifier = Modifier.height(8.dp))
        when {
            mcq != null -> McqBody(mcq = mcq, isAnswerRevealed = isAnswerRevealed, onToggleAnswer = onToggleAnswer, mcqId = bookmark.resourceId)
            failed -> LoadFailed(onRetry = onRetry)
            else -> Loading()
        }
        if (!bookmark.comments.isNullOrBlank()) {
            Text(
                text = bookmark.comments,
                style = ProstutiTypography.bnBodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun CardHeader(index: Int, bookmark: BookmarkEntity, onRemove: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.bookmark_question_number, index + 1),
                style = ProstutiTypography.bnLabelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatApiDateTime(bookmark.createdAt, locale) ?: bookmark.createdAt,
                style = ProstutiTypography.enLabelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.testTag(BOOKMARK_REMOVE_TAG + bookmark.resourceId)) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = stringResource(R.string.bookmark_remove),
                tint = BookmarkHeartColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun McqBody(mcq: McqDetail, isAnswerRevealed: Boolean, onToggleAnswer: () -> Unit, mcqId: String) {
    if (!mcq.questionText.isNullOrBlank()) {
        MathText(
            text = mcq.questionText,
            style = ProstutiTypography.bnMcqQuestion,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        mcq.options.sortedBy { it.order }.forEachIndexed { i, option ->
            OptionRow(
                label = optionLabels.getOrElse(i) { (i + 1).toString() },
                option = option,
                highlightCorrect = isAnswerRevealed && option.isCorrect
            )
        }
    }

    AnimatedVisibility(visible = isAnswerRevealed && !mcq.explanation.isNullOrBlank()) {
        Column(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(R.string.bookmark_explanation),
                style = ProstutiTypography.bnLabelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            MathText(
                text = mcq.explanation.orEmpty(),
                style = ProstutiTypography.bnBodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                horizontalPadding = 0.dp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
        OutlinedButton(
            onClick = onToggleAnswer,
            shape = RoundedCornerShape(50.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            modifier = Modifier.testTag(BOOKMARK_ANSWER_TOGGLE_TAG + mcqId)
        ) {
            Text(
                text = stringResource(if (isAnswerRevealed) R.string.bookmark_hide_answer else R.string.bookmark_show_answer),
                style = ProstutiTypography.bnLabelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun OptionRow(label: String, option: McqDetailOption, highlightCorrect: Boolean) {
    val bgColor = if (highlightCorrect) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val borderColor = if (highlightCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val badgeBg = if (highlightCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val badgeText = if (highlightCorrect) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(badgeBg),
            contentAlignment = Alignment.Center
        ) {
            Text(text = label, style = ProstutiTypography.bnLabelMedium, color = badgeText)
        }
        Spacer(modifier = Modifier.width(10.dp))
        MathText(
            text = option.optionText.orEmpty(),
            style = ProstutiTypography.bnMcqOption,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            horizontalPadding = 0.dp
        )
    }
}

@Composable
private fun Loading() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ProstutiSpinner(modifier = Modifier.size(18.dp))
        Text(
            text = stringResource(R.string.bookmark_question_loading),
            style = ProstutiTypography.bnBodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.bookmark_question_failed),
            style = ProstutiTypography.bnBodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry) {
            Text(text = stringResource(R.string.bookmark_retry), style = ProstutiTypography.bnLabelLarge)
        }
    }
}
