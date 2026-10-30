package com.prostuti.student.ui.screen.qb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.screen.qb.component.ReadOnlyMcqCard
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun QuestionReaderScreen(
    titleBn: String,
    onBack: () -> Unit,
    viewModel: QuestionReaderViewModel = koinViewModel()
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
                        text = titleBn,
                        style = ProstutiTypography.bnTitleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${state.questions.size}টি প্রশ্ন",
                        style = ProstutiTypography.enLabelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Global answer toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "সব উত্তর",
                        style = ProstutiTypography.bnLabelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = state.showAllAnswers,
                        onCheckedChange = { viewModel.toggleAllAnswers() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // ── Questions list ─────────────────────────────────────────────
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
                itemsIndexed(state.questions, key = { _, question -> question.id }) { index, question ->
                    ReadOnlyMcqCard(
                        index = index,
                        question = question,
                        isAnswerRevealed = state.showAllAnswers || question.id in state.revealedAnswers,
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
