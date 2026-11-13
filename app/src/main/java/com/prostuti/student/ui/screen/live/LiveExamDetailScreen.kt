package com.prostuti.student.ui.screen.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.prostuti.student.BuildConfig
import com.prostuti.student.data.model.enums.TimedExamState
import com.prostuti.student.data.model.response.TimedExam
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.util.formatAbsoluteDateTimeBn
import com.prostuti.student.util.toBengaliDigits
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.prostuti.student.ui.component.localized

@Composable
fun LiveExamDetailScreen(
    liveExamId: String,
    onBack: () -> Unit,
    onNavigateToAttempt: (examId: String) -> Unit,
    onNavigateToLockedResult: (examId: String) -> Unit,
    onNavigateToRanking: (examId: String) -> Unit,
    viewModel: LiveExamDetailViewModel = koinViewModel(parameters = { parametersOf(liveExamId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors,
            errorCleaner = viewModel::clearError,
            overlayPadding = padding,
            pullToRefreshEnabled = true,
            isRefreshing = state.isLoading && state.exam != null,
            onRefresh = viewModel::refresh
        ) {
            if (state.isLoading && state.exam == null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    ProstutiSpinner()
                }
                return@PullToRefreshErrorOverlayWrapper
            }
            val exam = state.exam ?: return@PullToRefreshErrorOverlayWrapper

            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)) {
                    BackButton(onClick = onBack)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = localized(exam.titleBn, exam.titleEn), style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text(text = localized(exam.subtitleBn, exam.subtitleEn), style = ProstutiTypography.enLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (BuildConfig.DEBUG) {
                        Text(
                            text = if (state.isDemoAdvancing) "…" else "ডেমো: অবস্থা পাল্টাও",
                            style = ProstutiTypography.enLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .clickable(enabled = !state.isDemoAdvancing, onClick = viewModel::advanceDemoState)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(12.dp)) {
                        Text(
                            text = "এই এক্সামের সময়, প্রশ্নসংখ্যা ও মার্কিং স্কিম অ্যাডমিন প্যানেল (ওয়েব) থেকে প্রি-কনফিগার করা — অ্যাপে এডিট করার কোনো অপশন নেই।",
                            style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    InfoTable(exam = exam)

                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.tertiaryContainer).padding(12.dp)) {
                        Text(
                            text = "একবার সাবমিট করলে আর ঢোকা যাবে না। উইন্ডো শেষ না হওয়া পর্যন্ত শুধু \"সাবমিট হয়েছে\" দেখবে — প্রকৃত উত্তর ও স্কোর তখনও লকড থাকবে।",
                            style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    when (state.examState) {
                        TimedExamState.UPCOMING -> {
                            Text(
                                text = "এখনও উইন্ডো শুরু হয়নি — শুরু হলে \"শুরু করুন\" বাটন সক্রিয় হবে।",
                                style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PrimaryButton(text = "শুরু করুন", enabled = false, onClick = {})
                        }
                        TimedExamState.OPEN_NOT_SUBMITTED -> PrimaryButton(text = "শুরু করুন", onClick = { onNavigateToAttempt(exam.examId) })
                        TimedExamState.OPEN_SUBMITTED -> PrimaryButton(text = "তুমি সাবমিট করেছো — দেখো", onClick = { onNavigateToLockedResult(exam.examId) })
                        TimedExamState.CLOSED -> {
                            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(text = "উইন্ডো শেষ", style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            PrimaryButton(text = "র‍্যাংকিং ও অ্যান্সার শিট দেখো", onClick = { onNavigateToRanking(exam.examId) })
                        }
                        null -> {}
                    }

                    Spacer(modifier = Modifier.padding(bottom = 8.dp))
                }
            }
    
        }
    }
}

@Composable
private fun InfoTable(exam: TimedExam) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        InfoRow("উইন্ডো শুরু", formatAbsoluteDateTimeBn(exam.windowStartEpochMillis))
        InfoRow("উইন্ডো শেষ", formatAbsoluteDateTimeBn(exam.windowEndEpochMillis))
        InfoRow("প্রশ্ন সংখ্যা", "${exam.questionCount.toBengaliDigits()}টি")
        InfoRow("মার্কিং স্কিম", "প্রতি প্রশ্নে ১, নেগেটিভ ${formatMark(exam.negativeMark)}")
        InfoRow("সময়সীমা", "${exam.examDurationMinutes.toBengaliDigits()} মিনিট")
        InfoRow("অংশগ্রহণ", "১বারই সুযোগ")
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(text = value, style = ProstutiTypography.enBodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatMark(value: Float): String {
    val plain = if (value == value.toInt().toFloat()) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')
    return plain.toBengaliDigits()
}
