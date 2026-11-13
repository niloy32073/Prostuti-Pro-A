package com.prostuti.student.ui.screen.timedexam

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.response.Question
import com.prostuti.student.data.model.response.TimedExamAnswerSheet
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.MathText
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.util.toBengaliDigits
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val mcqOptionLabels = listOf("ক", "খ", "গ", "ঘ")

@Composable
fun TimedAnswerSheetScreen(
    examId: String,
    kind: TimedExamKind,
    onBack: () -> Unit,
    viewModel: TimedAnswerSheetViewModel = koinViewModel(parameters = { parametersOf(examId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors,
            errorCleaner = viewModel::clearError,
            overlayPadding = padding
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    ProstutiSpinner()
                }
                return@PullToRefreshErrorOverlayWrapper
            }
            val sheet = state.sheet ?: return@PullToRefreshErrorOverlayWrapper

            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)) {
                    BackButton(onClick = onBack)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "অ্যান্সার শিট", style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onBackground)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { SummaryRow(sheet) }
                    items(sheet.questions, key = { it.mcqId }) { question ->
                        val index = sheet.questions.indexOf(question)
                        AnswerSheetCard(index = index, question = question, sheet = sheet)
                    }
                }
            }
    
        }
    }
}

@Composable
private fun SummaryRow(sheet: TimedExamAnswerSheet) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryTile(value = sheet.correctCount.toBengaliDigits(), label = "সঠিক", color = MaterialTheme.colorScheme.secondaryContainer, onColor = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.weight(1f))
        SummaryTile(value = sheet.wrongCount.toBengaliDigits(), label = "ভুল", color = MaterialTheme.colorScheme.errorContainer, onColor = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
        SummaryTile(value = sheet.unansweredCount.toBengaliDigits(), label = "স্কিপ", color = MaterialTheme.colorScheme.surfaceContainerLow, onColor = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        SummaryTile(value = "%.2f".format(sheet.totalMarks), label = "মার্ক", color = MaterialTheme.colorScheme.primaryContainer, onColor = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SummaryTile(value: String, label: String, color: androidx.compose.ui.graphics.Color, onColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(color).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = ProstutiTypography.enTitleMedium, color = onColor)
        Text(text = label, style = ProstutiTypography.bnLabelMedium, color = onColor, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun AnswerSheetCard(index: Int, question: Question, sheet: TimedExamAnswerSheet) {
    val myAnswer = sheet.myAnswers[question.mcqId]
    val correctAnswer = sheet.correctAnswers[question.mcqId]
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp)) {
        Text(text = "প্রশ্ন ${index + 1}", style = ProstutiTypography.enLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        if (!question.questionText.isNullOrBlank()) {
            MathText(text = question.questionText, style = ProstutiTypography.bnMcqQuestion, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 10.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            question.options.sortedBy { it.order }.forEachIndexed { optIndex, option ->
                val isCorrect = option.id == correctAnswer
                val isMine = option.id == myAnswer
                val (bg, border) = when {
                    isCorrect -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
                    isMine && !isCorrect -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outlineVariant
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(bg)
                        .border(1.5.dp, border, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(border), contentAlignment = Alignment.Center) {
                        Text(
                            text = mcqOptionLabels.getOrElse(optIndex) { (optIndex + 1).toString() },
                            style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.surface
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    if (!option.optionText.isNullOrBlank()) {
                        MathText(text = option.optionText, style = ProstutiTypography.bnMcqOption, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), horizontalPadding = 0.dp)
                    }
                    if (isMine && !isCorrect) {
                        Text(text = "তোমার উত্তর", style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        sheet.explanations[question.mcqId]?.takeIf { it.isNotBlank() }?.let { explanation ->
            Text(
                text = explanation, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
