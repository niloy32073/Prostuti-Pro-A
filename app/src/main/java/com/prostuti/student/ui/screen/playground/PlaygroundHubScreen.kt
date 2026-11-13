package com.prostuti.student.ui.screen.playground

import androidx.compose.foundation.background
import android.text.format.DateUtils
import com.prostuti.student.ui.screen.playground.graph.share.GraphShareImage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostuti.student.data.service.local.SavedGraphEntity
import org.koin.compose.viewmodel.koinViewModel
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

/** The Playground: a home for hands-on tools (the graph builder first) and the student's saved graphs. */
@Composable
fun PlaygroundHubScreen(
    onBack: () -> Unit,
    onOpenGraphBuilder: (savedGraphId: Long?) -> Unit,
    viewModel: PlaygroundHubViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var openId by remember { mutableStateOf<Long?>(null) }
    val open = dropUnlessResumed { onOpenGraphBuilder(openId) }
    PlaygroundHubContent(
        state = state,
        onBack = onBack,
        onOpenGraph = { id -> openId = id; open() },
        onRename = viewModel::startRename,
        onDelete = viewModel::startDelete,
        onRenameConfirm = viewModel::rename,
        onDeleteConfirm = viewModel::confirmDelete,
        onDismissDialog = viewModel::dismissDialogs,
    )
}

@Composable
fun PlaygroundHubContent(
    state: PlaygroundHubUiState,
    onBack: () -> Unit,
    onOpenGraph: (Long?) -> Unit,
    onRename: (SavedGraphEntity) -> Unit = {},
    onDelete: (SavedGraphEntity) -> Unit = {},
    onRenameConfirm: (String) -> Unit = {},
    onDeleteConfirm: () -> Unit = {},
    onDismissDialog: () -> Unit = {},
) {
    state.renaming?.let { graph -> RenameDialog(graph.name, onRenameConfirm, onDismissDialog) }
    state.deleting?.let { graph ->
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = { Text(stringResource(R.string.graph_delete_confirm_title)) },
            text = { Text(stringResource(R.string.graph_delete_confirm_body, graph.name), style = ProstutiTypography.bnBodyMedium) },
            confirmButton = {
                TextButton(onClick = onDeleteConfirm, modifier = Modifier.testTag("confirm_delete_graph")) {
                    Text(stringResource(R.string.graph_delete_line), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = onDismissDialog) { Text(stringResource(R.string.graph_cancel)) } },
        )
    }
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
                item(key = "graph_builder") { GraphBuilderCard { onOpenGraph(null) } }
                item(key = "more", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.playground_more_soon),
                        style = ProstutiTypography.bnBodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                item(key = "my_graphs", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.playground_my_graphs),
                        style = ProstutiTypography.bnTitleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                if (!state.isLoading && state.graphs.isEmpty()) {
                    item(key = "my_graphs_empty", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            stringResource(R.string.playground_my_graphs_empty),
                            style = ProstutiTypography.bnBodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("my_graphs_empty"),
                        )
                    }
                }
                items(state.graphs, key = { "g${it.id}" }) { graph ->
                    SavedGraphCard(graph, onOpen = { onOpenGraph(graph.id) }, onRename = { onRename(graph) }, onDelete = { onDelete(graph) })
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
    ProstutiTheme { PlaygroundHubContent(PlaygroundHubUiState(isLoading = false), onBack = {}, onOpenGraph = {}) }
}

@Composable
private fun SavedGraphCard(graph: SavedGraphEntity, onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onOpen)
            .testTag("saved_graph_${graph.id}")
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Text(
                stringResource(if (graph.isSpace) R.string.graph_mode_3d else R.string.graph_mode_2d),
                style = ProstutiTypography.bnLabelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(graph.name, style = ProstutiTypography.bnBodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                GraphShareImage.pretty(graph.preview), style = ProstutiTypography.bnBodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                savedWhen(graph.updatedAt),
                style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.testTag("saved_graph_menu_${graph.id}")) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.graph_more))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.graph_rename)) }, onClick = { menu = false; onRename() })
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.graph_delete_line), color = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; onDelete() },
                    modifier = Modifier.testTag("delete_saved_graph"),
                )
            }
        }
    }
}

@Composable
internal fun RenameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.graph_rename)) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                label = { Text(stringResource(R.string.graph_save_name)) },
                modifier = Modifier.testTag("rename_field"),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank(), modifier = Modifier.testTag("confirm_rename")) {
                Text(stringResource(R.string.graph_apply))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.graph_cancel)) } },
    )
}

/** "Just now" for the first minute, then Android's own "5 minutes ago", "Yesterday", … */
@Composable
private fun savedWhen(updatedAt: Long): String {
    val now = System.currentTimeMillis()
    return if (now - updatedAt < DateUtils.MINUTE_IN_MILLIS) {
        stringResource(R.string.playground_saved_just_now)
    } else {
        DateUtils.getRelativeTimeSpanString(updatedAt, now, DateUtils.MINUTE_IN_MILLIS).toString()
    }
}
