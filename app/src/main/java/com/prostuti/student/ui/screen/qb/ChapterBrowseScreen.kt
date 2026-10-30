package com.prostuti.student.ui.screen.qb

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.screen.qb.component.ReadOnlyMcqCard
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ChapterBrowseScreen(
    subjectId: String,
    nameBn: String,
    onBack: () -> Unit,
    viewModel: ChapterBrowseViewModel = koinViewModel(parameters = { parametersOf(subjectId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Header ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onClick = onBack)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nameBn,
                        style = ProstutiTypography.bnTitleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "অধ্যায়-ভিত্তিক প্রশ্ন",
                        style = ProstutiTypography.bnLabelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // ── Source filter chips ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                ChapterSourceFilter.entries.forEach { filter ->
                    val selected = state.sourceFilter == filter
                    val label = when (filter) {
                        ChapterSourceFilter.ALL -> "সব"
                        ChapterSourceFilter.BOARD -> "বোর্ড"
                        ChapterSourceFilter.COLLEGE -> "কলেজ"
                    }
                    Button(
                        onClick = { viewModel.selectSourceFilter(filter) },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary
                                            else androidx.compose.ui.graphics.Color.Transparent,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = null
                    ) {
                        Text(text = label, style = ProstutiTypography.bnLabelLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Chapter chips (horizontal scroll) ──────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.chapters.forEach { chapter ->
                    val selected = chapter.id == state.selectedChapterId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            .clickable { viewModel.selectChapter(chapter.id) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = chapter.nameBn,
                            style = ProstutiTypography.bnLabelLarge,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Questions ───────────────────────────────────────────────────
            val questions = state.questions
            if (questions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
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
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = hPadding, end = hPadding, bottom = 24.dp),
                    verticalItemSpacing = 14.dp,
                    horizontalArrangement = Arrangement.spacedBy(if (columns > 1) 8.dp else 0.dp)
                ) {
                    itemsIndexed(questions, key = { _, question -> question.id }) { index, question ->
                        ReadOnlyMcqCard(
                            index = index,
                            question = question,
                            isAnswerRevealed = question.id in state.revealedAnswers,
                            isFavorite = question.id in state.favoriteIds,
                            onToggleAnswer = { viewModel.toggleAnswer(question.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(question.id) },
                            onSubmitReport = { reason, detail -> viewModel.submitReport(question.id, reason, detail) }
                        )
                    }
                }
            }
        }
    }
}
