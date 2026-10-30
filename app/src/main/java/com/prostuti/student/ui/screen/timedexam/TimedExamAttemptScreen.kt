package com.prostuti.student.ui.screen.timedexam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.response.Option
import com.prostuti.student.data.model.response.Question
import com.prostuti.student.ui.component.MathText
import com.prostuti.student.ui.component.ObserveErrors
import com.prostuti.student.ui.theme.ProstutiTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val mcqOptionLabels = listOf("ক", "খ", "গ", "ঘ")

@Composable
fun TimedExamAttemptScreen(
    examId: String,
    kind: TimedExamKind,
    onSubmitted: () -> Unit,
    viewModel: TimedExamAttemptViewModel = koinViewModel(parameters = { parametersOf(examId, kind) })
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.feature_coming_soon)
    var showLeaveDialog by remember { mutableStateOf(false) }

    ObserveErrors(errors = state.errors, snackbarHostState = snackbarHostState, onConsumed = viewModel::clearError)

    LaunchedEffect(state.timeRemainingSeconds, state.isSubmitted, state.isLoading) {
        if (!state.isLoading && state.timeRemainingSeconds > 0 && !state.isSubmitted) {
            delay(1000L)
            viewModel.tickTimer()
        }
    }

    LaunchedEffect(state.isSubmitted) {
        if (state.isSubmitted) onSubmitted()
    }

    BackHandler(enabled = !state.isLoading && !state.isSubmitted) { showLeaveDialog = true }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("এক্সাম ছেড়ে যাবে?", style = ProstutiTypography.bnTitleMedium) },
            text = { Text("যতটুকু উত্তর দিয়েছ তা দিয়েই এখন সাবমিট হয়ে যাবে — আর ফেরা যাবে না।", style = ProstutiTypography.bnBodyMedium) },
            confirmButton = {
                TextButton(onClick = { showLeaveDialog = false; viewModel.submitExam() }) {
                    Text("সাবমিট করে ছেড়ে যাও", style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text("চালিয়ে যাও", style = ProstutiTypography.bnBodyMedium) } }
        )
    }

    if (state.showSubmitDialog) {
        val unanswered = state.questions.count { it.mcqId !in state.answers }
        AlertDialog(
            onDismissRequest = viewModel::dismissSubmitDialog,
            title = { Text("এক্সাম সাবমিট করবে?", style = ProstutiTypography.bnTitleMedium) },
            text = {
                Text(
                    if (unanswered > 0) "${unanswered}টি প্রশ্নের উত্তর এখনও দেওয়া হয়নি। সাবমিটের পর আর ফেরা যাবে না।"
                    else "সবগুলো প্রশ্নের উত্তর দেওয়া হয়েছে। সাবমিটের পর আর ফেরা যাবে না।",
                    style = ProstutiTypography.bnBodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::submitExam, enabled = !state.isSubmitting) {
                    Text("সাবমিট করো", style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissSubmitDialog) { Text("বাতিল", style = ProstutiTypography.bnBodyMedium) } }
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        val progress = if (state.totalDurationSeconds > 0) state.timeRemainingSeconds.toFloat() / state.totalDurationSeconds else 0f
        val minutes = state.timeRemainingSeconds / 60
        val seconds = state.timeRemainingSeconds % 60
        val timeText = "%d:%02d".format(minutes, seconds)
        val kindLabel = if (kind == TimedExamKind.LIVE) "লাইভ এক্সাম" else "গ্রুপ এক্সাম"

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${state.answers.size.toString()}/${state.questions.size} উত্তর দেওয়া হয়েছে",
                        style = ProstutiTypography.enLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = timeText, style = ProstutiTypography.enTitleMedium, color = MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(4.dp),
                    color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$kindLabel · ১বারই সুযোগ — সাবমিটের পর ফেরা যাবে না",
                        style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { scope.launch { snackbarHostState.showSnackbar(comingSoon) } }) {
                        Icon(Icons.Outlined.Calculate, contentDescription = "ক্যালকুলেটর ও স্ক্র্যাচ ক্যানভাস খোলো", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(state.questions, key = { it.mcqId }) { question ->
                    val index = state.questions.indexOf(question)
                    TimedMcqCard(index = index, question = question, selectedOptionId = state.answers[question.mcqId], onOptionSelected = { viewModel.selectAnswer(question.mcqId, it) })
                }
            }

            Button(
                onClick = viewModel::showSubmitDialog,
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp)
            ) {
                Text(text = "এক্সাম সাবমিট করো", style = ProstutiTypography.bnBodyLarge)
            }
        }
    }
}

@Composable
private fun TimedMcqCard(index: Int, question: Question, selectedOptionId: String?, onOptionSelected: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp)
    ) {
        Text(text = "প্রশ্ন ${index + 1}", style = ProstutiTypography.enLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        if (!question.questionText.isNullOrBlank()) {
            MathText(text = question.questionText, style = ProstutiTypography.bnMcqQuestion, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 10.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            question.options.sortedBy { it.order }.forEachIndexed { optIndex, option ->
                TimedMcqOption(
                    label = mcqOptionLabels.getOrElse(optIndex) { (optIndex + 1).toString() },
                    option = option, isSelected = option.id == selectedOptionId, onClick = { onOptionSelected(option.id) }
                )
            }
        }
    }
}

@Composable
private fun TimedMcqOption(label: String, option: Option, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val badgeBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val badgeText = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(badgeBg), contentAlignment = Alignment.Center) {
            Text(text = label, style = ProstutiTypography.bnLabelMedium, color = badgeText)
        }
        Spacer(modifier = Modifier.width(10.dp))
        if (!option.optionText.isNullOrBlank()) {
            MathText(text = option.optionText, style = ProstutiTypography.bnMcqOption, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), horizontalPadding = 0.dp)
        }
    }
}
