package com.prostuti.student.ui.screen.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.util.formatMinuteOfDayBn
import com.prostuti.student.util.toBengaliDigits
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val SAVE_ERROR_TAG = "SAVE_ROUTINE"

@Composable
fun GroupRoutineWindowScreen(
    groupId: String,
    subjectIds: List<String>,
    chapterIds: List<String>,
    topicIds: List<String>,
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: GroupRoutineWindowViewModel = koinViewModel(parameters = { parametersOf(groupId, subjectIds, chapterIds, topicIds) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // This ViewModel keeps one save error instead of a map; show it in the same error overlay.
    val errors = state.errorBn?.let { mapOf(SAVE_ERROR_TAG to it) } ?: emptyMap()

    val locked = state.locked
    val alpha = if (locked) 0.45f else 1f

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = errors,
            errorCleaner = { viewModel.clearError() },
            overlayPadding = padding
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    BackButton(onClick = onBack)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "সময়-উইন্ডো ও নিয়ম", style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text(
                            text = "গ্রুপ রুটিন তৈরি · ${topicIds.size}টি টপিক · ধাপ ৩/৩", style = ProstutiTypography.enLabelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.tertiaryContainer).padding(12.dp)
                    ) {
                        Text(
                            text = "একবার সেভ করলে এই নিয়মগুলো আর পরিবর্তন করা যাবে না — এটাই হবে গ্রুপের রুটিন, এবং প্রতিটা নির্ধারিত এক্সাম-দিনে এই কনফিগ অনুযায়ী গ্রুপ এক্সাম স্বয়ংক্রিয়ভাবে লাইভ হয়ে যাবে।",
                            style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    Column(modifier = Modifier.alpha(alpha), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(text = "কত দিনের রুটিন বানাবে?", style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        StepperRow(
                            label = "${state.routineDurationDays.toBengaliDigits()} দিনের", onDec = { viewModel.changeDurationDays(-7) },
                            onInc = { viewModel.changeDurationDays(7) }, enabled = !locked
                        )
                    }

                    Column(modifier = Modifier.alpha(alpha), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "সপ্তাহের কোন দিনগুলোতে গ্রুপ এক্সাম হবে?", style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            weekDayLabelsBn.forEachIndexed { index, label ->
                                val active = index in state.examWeekDays
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow)
                                        .clickable(enabled = !locked) { viewModel.toggleWeekDay(index) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label, style = ProstutiTypography.bnBodyMedium,
                                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Text(
                            text = "বাকি দিনগুলোতে কোনো এক্সাম থাকবে না — ওই দিনের টপিক শুধু রেফারেন্স হিসেবে ক্যালেন্ডারে দেখাবে।",
                            style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(modifier = Modifier.alpha(alpha), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "প্রতিটা নির্ধারিত এক্সাম-দিনে এই সময়েই উইন্ডো খুলবে-বন্ধ হবে", style = ProstutiTypography.bnBodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LabeledRow(label = "উইন্ডো শুরু") {
                            StepperRow(label = formatMinuteOfDayBn(state.windowStartMinuteOfDay), onDec = { viewModel.changeWindowStart(-30) }, onInc = { viewModel.changeWindowStart(30) }, enabled = !locked)
                        }
                        LabeledRow(label = "উইন্ডো শেষ") {
                            StepperRow(label = formatMinuteOfDayBn(state.windowEndMinuteOfDay), onDec = { viewModel.changeWindowEnd(-30) }, onInc = { viewModel.changeWindowEnd(30) }, enabled = !locked)
                        }
                        LabeledRow(label = "সময়সীমা (মিনিট)") {
                            StepperRow(label = "${state.examDurationMinutes.toBengaliDigits()}", onDec = { viewModel.changeExamDuration(-5) }, onInc = { viewModel.changeExamDuration(5) }, enabled = !locked)
                        }
                    }

                    Column(modifier = Modifier.alpha(alpha), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(text = "প্রতি এক্সাম-দিনের জন্য কনফিগ", style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        LabeledRow(label = "প্রশ্নসংখ্যা") {
                            StepperRow(label = "${state.questionCount.toBengaliDigits()}", onDec = { viewModel.changeQuestionCount(-5) }, onInc = { viewModel.changeQuestionCount(5) }, enabled = !locked)
                        }
                        LabeledRow(label = "প্রতি প্রশ্নের মান") {
                            StepperRow(label = formatMarkBn(state.markPerQuestion), onDec = { viewModel.changeMarkPerQuestion(-0.25f) }, onInc = { viewModel.changeMarkPerQuestion(0.25f) }, enabled = !locked)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "নেগেটিভ মার্কিং", style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Switch(checked = state.negativeMarkingEnabled, onCheckedChange = { viewModel.toggleNegativeMarking() }, enabled = !locked)
                        }
                        if (state.negativeMarkingEnabled) {
                            LabeledRow(label = "নেগেটিভ মার্ক, প্রতি ভুল উত্তরে") {
                                StepperRow(label = formatMarkBn(state.negativeMark), onDec = { viewModel.changeNegativeMark(-0.25f) }, onInc = { viewModel.changeNegativeMark(0.25f) }, enabled = !locked)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "মোট মার্ক", style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Text(text = formatMarkBn(state.totalMarks), style = ProstutiTypography.enTitleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (locked) {
                        Text(
                            text = "লক করা হয়েছে — গ্রুপ রুটিন সক্রিয়, এখন থেকে প্রতিটা এক্সাম-দিনে গ্রুপ এক্সাম অটো তৈরি হবে",
                            style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        PrimaryButton(text = "গ্রুপ ডিটেইলে যাও", onClick = onDone)
                    } else {
                        PrimaryButton(
                            text = "রুটিন সেভ করো ও সক্রিয় করো", loading = state.isSaving, enabled = state.canSave,
                            onClick = viewModel::save
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
    
        }
    }
}

@Composable
private fun LabeledRow(label: String, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
private fun StepperRow(label: String, onDec: () -> Unit, onInc: () -> Unit, enabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StepperButton(symbol = "−", onClick = onDec, enabled = enabled)
        Text(
            text = label, style = ProstutiTypography.enBodyLarge, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        StepperButton(symbol = "+", onClick = onInc, enabled = enabled)
    }
}

@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit, enabled: Boolean) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = symbol, style = ProstutiTypography.enTitleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatMarkBn(value: Float): String {
    val plain = if (value == value.toInt().toFloat()) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')
    return plain.toBengaliDigits()
}
