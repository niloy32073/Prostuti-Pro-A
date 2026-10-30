package com.prostuti.student.ui.screen.qb.component

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prostuti.student.ui.component.MathText
import com.prostuti.student.ui.component.McqReportDialog
import com.prostuti.student.ui.screen.qb.DummyQuestion
import com.prostuti.student.ui.theme.ProstutiTypography

private val readOnlyMcqOptionLabels = listOf("ক", "খ", "গ", "ঘ")

/** A read-only MCQ card (no attempt) — used by the question reader and chapter browse screens. */
@Composable
fun ReadOnlyMcqCard(
    index: Int,
    question: DummyQuestion,
    isAnswerRevealed: Boolean,
    isFavorite: Boolean,
    onToggleAnswer: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSubmitReport: (reason: String, detail: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showExplanation by rememberSaveable { mutableStateOf(false) }
    var showReport by rememberSaveable { mutableStateOf(false) }

    if (showExplanation) {
        ReadOnlyMcqExplanationDialog(question = question, onDismiss = { showExplanation = false })
    }
    if (showReport) {
        McqReportDialog(
            onSubmit = { reason, detail -> onSubmitReport(reason, detail); showReport = false },
            onDismiss = { showReport = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp)
    ) {
        // ── Top: question number + favorite + report ─────────────────────────
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

        // ── Question text ────────────────────────────────────────────────────
        MathText(
            text = question.text,
            style = ProstutiTypography.bnMcqQuestion,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // ── Options ──────────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            question.options.forEachIndexed { optIndex, optText ->
                val isCorrect = isAnswerRevealed && optIndex == question.correctIndex
                val bgColor = if (isCorrect) MaterialTheme.colorScheme.primaryContainer
                              else MaterialTheme.colorScheme.surface
                val borderColor = if (isCorrect) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.outlineVariant
                val badgeBg = if (isCorrect) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.secondaryContainer
                val badgeText = if (isCorrect) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSecondaryContainer

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
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = readOnlyMcqOptionLabels.getOrElse(optIndex) { (optIndex + 1).toString() },
                            style = ProstutiTypography.bnLabelMedium,
                            color = badgeText
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    MathText(
                        text = optText,
                        style = ProstutiTypography.bnMcqOption,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        horizontalPadding = 0.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Bottom action row ─────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (question.explanation.isNotBlank() || question.explanationImageUrl != null) {
                TextButton(
                    onClick = { showExplanation = true },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ব্যাখ্যা দেখুন",
                        style = ProstutiTypography.bnLabelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = onToggleAnswer,
                shape = RoundedCornerShape(50.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isAnswerRevealed) "উত্তর লুকাও" else "উত্তর দেখাও",
                    style = ProstutiTypography.bnLabelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ReadOnlyMcqExplanationDialog(question: DummyQuestion, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "ব্যাখ্যা", style = ProstutiTypography.bnTitleMedium) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (question.explanationImageUrl != null) {
                    // TODO: replace with AsyncImage(coil3) once Coil is added as a dependency
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ছবি লোড হচ্ছে…",
                            style = ProstutiTypography.bnLabelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (question.explanation.isNotBlank()) {
                    MathText(
                        text = question.explanation,
                        style = ProstutiTypography.bnBodyMedium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "এই প্রশ্নের কোনো ব্যাখ্যা নেই।",
                        style = ProstutiTypography.bnBodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "বন্ধ করো", style = ProstutiTypography.bnBodyMedium)
            }
        }
    )
}

