package com.prostuti.student.ui.screen.playground

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.prostuti.student.R
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.FormFactorPreviews
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.theme.ProstutiTheme
import com.prostuti.student.ui.theme.ProstutiTypography

/** The Playground: a home for hands-on tools. The graph builder is the first one. */
@Composable
fun PlaygroundHubScreen(onBack: () -> Unit, onOpenGraphBuilder: () -> Unit) {
    PlaygroundHubContent(onBack = onBack, onOpenGraphBuilder = dropUnlessResumed { onOpenGraphBuilder() })
}

@Composable
fun PlaygroundHubContent(onBack: () -> Unit, onOpenGraphBuilder: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 10.dp),
            ) {
                BackButton(onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.playground_title), style = ProstutiTypography.bnTitleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        stringResource(R.string.playground_subtitle),
                        style = ProstutiTypography.bnBodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(adaptiveColumnCount()),
                state = rememberLazyGridState(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "graph_builder") { GraphBuilderCard(onOpenGraphBuilder) }
                item(key = "more", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.playground_more_soon),
                        style = ProstutiTypography.bnBodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GraphBuilderCard(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .testTag("playground_graph_builder")
            .padding(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.playground_graph_builder),
                style = ProstutiTypography.bnTitleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                stringResource(R.string.playground_graph_builder_desc),
                style = ProstutiTypography.bnBodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@FormFactorPreviews
@Composable
private fun PlaygroundHubPreview() {
    ProstutiTheme { PlaygroundHubContent(onBack = {}, onOpenGraphBuilder = {}) }
}
