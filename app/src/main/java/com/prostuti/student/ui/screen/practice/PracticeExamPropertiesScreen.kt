package com.prostuti.student.ui.screen.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.prostuti.student.data.model.enums.Level
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val PRACTICE_EXAM_TITLE = "প্র্যাকটিস এক্সাম"

private val sourceLabels = mapOf(
    PracticeQuestionSource.BOARD to "বোর্ড প্রশ্ন",
    PracticeQuestionSource.COLLEGE to "কলেজ প্রশ্ন",
    PracticeQuestionSource.ADMISSION to "ভর্তি প্রশ্ন",
    PracticeQuestionSource.ORIGINAL to "প্রস্তুতি অরিজিনাল"
)

@Composable
fun PracticeExamPropertiesScreen(
    subjectIds: List<String>,
    chapterIds: List<String>,
    topicIds: List<String>,
    onBack: () -> Unit,
    onExamGenerated: (attemptId: String, titleBn: String) -> Unit,
    viewModel: PracticeExamPropertiesViewModel = koinViewModel(parameters = { parametersOf(subjectIds, chapterIds, topicIds) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.generatedAttemptId) {
        state.generatedAttemptId?.let { onExamGenerated(it, PRACTICE_EXAM_TITLE) }
    }

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
                            text = "এক্সাম প্রপার্টি",
                            style = ProstutiTypography.bnTitleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${topicIds.size}টি টপিক নির্বাচিত · ধাপ ৩/৩",
                            style = ProstutiTypography.enLabelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PropertySection(title = "এক্সামের ধরন") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PillToggle(
                                label = "একাডেমিক",
                                selected = state.level == Level.ACADEMIC,
                                onClick = { viewModel.setLevel(Level.ACADEMIC) }
                            )
                            PillToggle(
                                label = "ভর্তি (অ্যাডমিশন)",
                                selected = state.level == Level.ADMISSION,
                                onClick = { viewModel.setLevel(Level.ADMISSION) }
                            )
                        }
                    }

                    PropertySection(title = "প্রশ্নের উৎস") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PracticeQuestionSource.entries.forEach { source ->
                                PillToggle(
                                    label = sourceLabels[source].orEmpty(),
                                    selected = source in state.selectedSources,
                                    onClick = { viewModel.toggleSource(source) }
                                )
                            }
                        }
                    }

                    PropertySection(title = "ভার্সন") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PillToggle(label = "বাংলা ভার্সন", selected = state.version == "BN", onClick = { viewModel.setVersion("BN") })
                            PillToggle(label = "English Version", selected = state.version == "EN", onClick = { viewModel.setVersion("EN") })
                        }
                    }

                    PropertySection(title = "প্রশ্ন সংখ্যা") {
                        NumberFieldWithPresets(
                            value = state.questionCountText,
                            onValueChange = viewModel::setQuestionCountText,
                            keyboardType = KeyboardType.Number,
                            presets = QUESTION_COUNT_PRESETS,
                            onPresetClick = viewModel::pickQuestionCountPreset
                        )
                    }

                    PropertySection(title = "সময়সীমা (মিনিট)") {
                        NumberFieldWithPresets(
                            value = state.durationText,
                            onValueChange = viewModel::setDurationText,
                            keyboardType = KeyboardType.Number,
                            presets = DURATION_PRESETS,
                            onPresetClick = viewModel::pickDurationPreset
                        )
                    }

                    PropertySection(title = "প্রতি প্রশ্নের মান") {
                        NumberFieldWithPresets(
                            value = state.markPerQuestionText,
                            onValueChange = viewModel::setMarkPerQuestionText,
                            keyboardType = KeyboardType.Decimal,
                            presets = MARK_PER_QUESTION_PRESETS,
                            onPresetClick = viewModel::pickMarkPerQuestionPreset
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "নেগেটিভ মার্কিং",
                                style = ProstutiTypography.bnLabelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(checked = state.negativeMarkingEnabled, onCheckedChange = viewModel::setNegativeMarkingEnabled)
                        }
                        if (state.negativeMarkingEnabled) {
                            NumberFieldWithPresets(
                                value = state.negativeMarkText,
                                onValueChange = viewModel::setNegativeMarkText,
                                keyboardType = KeyboardType.Decimal,
                                presets = NEGATIVE_MARK_PRESETS,
                                onPresetClick = viewModel::pickNegativeMarkPreset
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
                }

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp)
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Button(
                            onClick = viewModel::generateExam,
                            enabled = !state.isGenerating,
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            if (state.isGenerating) {
                                ProstutiSpinner(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(text = "এক্সাম শুরু করো", style = ProstutiTypography.bnBodyLarge)
                            }
                        }
                    }
                }
            }
    
        }
    }
}

@Composable
private fun PropertySection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = ProstutiTypography.bnLabelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

/** A narrow text field for a number, with quick-pick preset chips in the same row — saves the
 *  vertical space a stepper or a second wrapped row of chips would take. */
@Composable
private fun NumberFieldWithPresets(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    presets: List<String>,
    onPresetClick: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            modifier = Modifier.width(76.dp)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                PillToggle(label = preset, selected = value == preset, onClick = { onPresetClick(preset) })
            }
        }
    }
}

@Composable
private fun PillToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    val textColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = label, style = ProstutiTypography.bnLabelLarge, color = textColor)
    }
}
