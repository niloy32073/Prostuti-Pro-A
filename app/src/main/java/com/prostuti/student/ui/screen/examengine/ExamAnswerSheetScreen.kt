package com.prostuti.student.ui.screen.examengine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prostuti.student.data.model.response.MCQReviewQuestionResponse
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.MathText
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val reviewOptionLabels = listOf("ক", "খ", "গ", "ঘ")

@Composable
fun ExamAnswerSheetScreen(
    attemptId: String,
    titleBn: String,
    onBack: () -> Unit,
    viewModel: ExamAnswerSheetViewModel = koinViewModel(parameters = { parametersOf(attemptId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors,
            errorCleaner = viewModel::clearError,
            overlayPadding = padding
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton(onClick = onBack)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "অ্যান্সার শিট",
                            style = ProstutiTypography.bnTitleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = titleBn,
                            style = ProstutiTypography.enLabelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    return@Column
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnswerSheetFilterChip(
                        label = "সব (${state.questions.size})",
                        selected = state.filter == AnswerSheetFilter.ALL,
                        onClick = { viewModel.setFilter(AnswerSheetFilter.ALL) }
                    )
                    AnswerSheetFilterChip(
                        label = "সঠিক (${state.correctCount})",
                        selected = state.filter == AnswerSheetFilter.CORRECT,
                        onClick = { viewModel.setFilter(AnswerSheetFilter.CORRECT) }
                    )
                    AnswerSheetFilterChip(
                        label = "ভুল (${state.wrongCount})",
                        selected = state.filter == AnswerSheetFilter.WRONG,
                        onClick = { viewModel.setFilter(AnswerSheetFilter.WRONG) }
                    )
                    AnswerSheetFilterChip(
                        label = "স্কিপ (${state.skippedCount})",
                        selected = state.filter == AnswerSheetFilter.SKIPPED,
                        onClick = { viewModel.setFilter(AnswerSheetFilter.SKIPPED) }
                    )
                }

                val filtered = state.filteredQuestions
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "এই ফিল্টারে কোনো প্রশ্ন নেই",
                            style = ProstutiTypography.bnBodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val columns = adaptiveColumnCount()
                    val hPadding = if (columns > 1) 8.dp else 16.dp
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(columns),
                        state = rememberLazyStaggeredGridState(),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = hPadding, end = hPadding, bottom = 16.dp),
                        verticalItemSpacing = 10.dp,
                        horizontalArrangement = Arrangement.spacedBy(if (columns > 1) 8.dp else 0.dp)
                    ) {
                        items(filtered, key = { it.mcqId }) { question ->
                            AnswerSheetItem(
                                question = question,
                                isExpanded = state.expandedMcqId == question.mcqId,
                                onToggle = { viewModel.toggleExpanded(question.mcqId) }
                            )
                        }
                    }
                }
            }
    
        }
    }
}

@Composable
private fun AnswerSheetFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = ProstutiTypography.bnLabelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AnswerSheetItem(
    question: MCQReviewQuestionResponse,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val badgeBg = when (question.isCorrect) {
        true -> MaterialTheme.colorScheme.secondaryContainer
        false -> MaterialTheme.colorScheme.errorContainer
        null -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val badgeText = when (question.isCorrect) {
        true -> MaterialTheme.colorScheme.onSecondaryContainer
        false -> MaterialTheme.colorScheme.onErrorContainer
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val selectedOption = question.options.find { it.id == question.selectedOptionId }
    val correctOption = question.options.find { it.isCorrect }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggle)
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = question.order.toString(), style = ProstutiTypography.enLabelMedium, color = badgeText)
            }
            Spacer(modifier = Modifier.width(12.dp))
            MathText(
                text = question.questionText.orEmpty(),
                style = ProstutiTypography.bnBodyMedium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                horizontalPadding = 0.dp,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (isExpanded) 90f else 0f)
            )
        }

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (question.isCorrect == false) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "তোমার উত্তর", style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    MathText(
                        text = selectedOption?.let { "${reviewOptionLabels.getOrElse(it.order - 1) { "?" }}) ${it.optionText.orEmpty()}" } ?: "উত্তর দাওনি",
                        style = ProstutiTypography.bnLabelLarge,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        horizontalPadding = 0.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "সঠিক উত্তর", style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    MathText(
                        text = correctOption?.let { "${reviewOptionLabels.getOrElse(it.order - 1) { "?" }}) ${it.optionText.orEmpty()}" } ?: "—",
                        style = ProstutiTypography.bnLabelLarge,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.End,
                        horizontalPadding = 0.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (!question.explanationText.isNullOrBlank()) {
                    MathText(
                        text = question.explanationText,
                        style = ProstutiTypography.bnBodyMedium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
