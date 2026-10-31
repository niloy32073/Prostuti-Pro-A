package com.prostuti.student.ui.screen.examengine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.domain.parseServerTimeMillis
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ExamResultScreen(
    attemptId: String,
    titleBn: String,
    onViewAnswerSheet: () -> Unit,
    onViewLeaderboard: (examId: String) -> Unit,
    onFinish: () -> Unit,
    viewModel: ExamResultViewModel = koinViewModel(parameters = { parametersOf(attemptId) })
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
            if (state.isLoading && state.result == null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                state.result?.let { result ->
                    ResultContent(
                        titleBn = titleBn,
                        result = result,
                        onViewAnswerSheet = onViewAnswerSheet,
                        onViewLeaderboard = { onViewLeaderboard(result.examId) },
                        onFinish = onFinish,
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                }
            }
    
        }
    }
}

@Composable
private fun ResultContent(
    titleBn: String,
    result: ExamAttemptResultResponse,
    onViewAnswerSheet: () -> Unit,
    onViewLeaderboard: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalQuestions = result.rightAnswerCount + result.wrongAnswerCount + result.skipCount
    val scorePercent = if (totalQuestions > 0) (result.rightAnswerCount * 100f / totalQuestions) else 0f
    val wrongPercent = if (totalQuestions > 0) (result.wrongAnswerCount * 100f / totalQuestions) else 0f
    val timeTakenText = remember(result.startAt, result.endAt) { formatTimeTaken(result.startAt, result.endAt) }

    Column(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 28.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "এক্সাম শেষ!",
                    style = ProstutiTypography.bnHeadlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = titleBn,
                    style = ProstutiTypography.bnBodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            ScoreDonut(
                correctPercent = scorePercent,
                wrongPercent = wrongPercent,
                rightCount = result.rightAnswerCount,
                total = totalQuestions
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultStatTile(
                    value = result.rightAnswerCount.toString(),
                    label = "সঠিক",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                ResultStatTile(
                    value = result.wrongAnswerCount.toString(),
                    label = "ভুল",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                ResultStatTile(
                    value = result.skipCount.toString(),
                    label = "স্কিপ",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ResultSummaryRow(label = "সময় লেগেছে", value = timeTakenText)
                ResultSummaryRow(
                    label = "চূড়ান্ত মার্ক",
                    value = "%.2f".format(result.totalScore),
                    valueColor = MaterialTheme.colorScheme.onSurface
                )
                if (result.mode == AttemptMode.LIVE && result.rank != null) {
                    ResultSummaryRow(label = "র‍্যাংক", value = "#${result.rank}")
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onViewAnswerSheet,
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(text = "অ্যান্সার শিট দেখো", style = ProstutiTypography.bnBodyLarge)
            }
            if (result.mode == AttemptMode.LIVE) {
                OutlinedButton(
                    onClick = onViewLeaderboard,
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(text = "লিডারবোর্ড দেখো", style = ProstutiTypography.bnBodyLarge)
                }
            }
            Button(
                onClick = onFinish,
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(text = "হোমে ফিরে যাও", style = ProstutiTypography.bnBodyLarge)
            }
        }
    }
}

@Composable
private fun ScoreDonut(
    correctPercent: Float,
    wrongPercent: Float,
    rightCount: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val success = MaterialTheme.colorScheme.secondary
    val error = MaterialTheme.colorScheme.error
    val track = MaterialTheme.colorScheme.surfaceContainerHighest

    val brush = remember(correctPercent, wrongPercent, success, error, track) {
        val correctStop = (correctPercent / 100f).coerceIn(0f, 1f)
        val wrongStop = ((correctPercent + wrongPercent) / 100f).coerceIn(0f, 1f)
        Brush.sweepGradient(
            0f to success,
            correctStop to success,
            correctStop to error,
            wrongStop to error,
            wrongStop to track,
            1f to track
        )
    }

    Box(
        modifier = modifier.size(176.dp).clip(CircleShape).background(brush),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$rightCount/$total",
                    style = ProstutiTypography.enHeadlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${correctPercent.toInt()}% স্কোর",
                    style = ProstutiTypography.enLabelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ResultStatTile(
    value: String,
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, style = ProstutiTypography.enTitleLarge, color = contentColor)
        Text(
            text = label,
            style = ProstutiTypography.bnLabelMedium,
            color = contentColor,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ResultSummaryRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = ProstutiTypography.enLabelLarge, color = valueColor)
    }
}

private fun formatTimeTaken(startAt: String, endAt: String?): String {
    if (endAt == null) return "—"
    val start = parseServerTimeMillis(startAt) ?: return "—"
    val end = parseServerTimeMillis(endAt) ?: return "—"
    val totalSeconds = ((end - start) / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
