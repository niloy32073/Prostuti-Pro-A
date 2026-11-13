package com.prostuti.student.ui.screen.timedexam

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
import com.prostuti.student.data.model.response.RankRow
import com.prostuti.student.data.model.response.WindowRanking
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.util.toBengaliDigits
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun WindowRankingScreen(
    examId: String,
    kind: TimedExamKind,
    onBack: () -> Unit,
    onNavigateToMyAnswerSheet: () -> Unit,
    viewModel: WindowRankingViewModel = koinViewModel(parameters = { parametersOf(examId, kind) })
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
            val ranking = state.ranking ?: return@PullToRefreshErrorOverlayWrapper
            val kindLabel = "লাইভ এক্সাম · সম্পূর্ণ স্ট্রিম · উইন্ডো শেষ"

            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)) {
                    BackButton(onClick = onBack)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "র‍্যাংকিং", style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text(text = kindLabel, style = ProstutiTypography.enLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { StatsRow(ranking) }
                    item { Histogram(ranking) }
                    item {
                        Text(
                            text = "সবার র‍্যাংক", style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                        )
                    }
                    items(ranking.rows, key = { it.rank }) { row -> RankRowItem(row = row, onClick = if (row.isMe) onNavigateToMyAnswerSheet else null) }
                }
            }
    
        }
    }
}

@Composable
private fun StatsRow(ranking: WindowRanking) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(value = "${ranking.submittedCount.toBengaliDigits()}/${ranking.totalParticipants.toBengaliDigits()}", label = "সাবমিট করেছে", modifier = Modifier.weight(1f))
        StatTile(value = "${ranking.averagePercent.toBengaliDigits()}%", label = "গড় স্কোর", modifier = Modifier.weight(1f))
        StatTile(value = ranking.myRank?.let { "#${it.toBengaliDigits()}" } ?: "—", label = "তোমার র‍্যাংক", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, style = ProstutiTypography.enTitleMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun Histogram(ranking: WindowRanking) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(text = "স্কোর বিস্তার", style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        Row(modifier = Modifier.fillMaxWidth().height(70.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
            ranking.histogramBars.forEachIndexed { i, value ->
                Box(
                    modifier = Modifier.weight(1f).height((value.coerceIn(4, 100) * 0.7f).dp).clip(RoundedCornerShape(3.dp))
                        .background(if (i == ranking.myHistogramBarIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(text = "০%", style = ProstutiTypography.enLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(text = "১০০%", style = ProstutiTypography.enLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RankRowItem(row: RankRow, onClick: (() -> Unit)?) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (row.isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape)
                    .background(if (row.rank <= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${row.rank.toBengaliDigits()}", style = ProstutiTypography.enLabelMedium,
                    color = if (row.rank <= 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (row.isMe) "${row.name} (তুমি)" else row.name, style = ProstutiTypography.bnBodyMedium,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)
            )
            Text(text = row.scoreLabel, style = ProstutiTypography.enLabelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        if (row.isMe) {
            Text(
                text = "সঠিক/ভুল দেখতে ট্যাপ করো", style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}
