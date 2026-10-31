package com.prostuti.student.ui.screen.playground.graph.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.screen.playground.graph.GraphBuilderUiState
import com.prostuti.student.ui.screen.playground.graph.GraphColors


/**
 * The editable list of lines. It is one column on purpose: it is an editor panel beside/under the
 * graph, not a content list, so the adaptive multi-column grid rule does not apply here.
 */
@Composable
fun ExpressionList(state: GraphBuilderUiState, actions: ExpressionListActions, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val palette = GraphColors.palette()
    LaunchedEffect(state.focusedId) {
        val index = state.lines.indexOfFirst { it.id == state.focusedId }
        if (index >= 0) listState.animateScrollToItem(index)
    }
    LazyColumn(
        state = listState,
        modifier = modifier.testTag("expression_list"),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(state.lines, key = { _, line -> line.id }) { index, line ->
            ExpressionCard(
                index = index,
                line = line,
                status = state.status(line.id),
                color = palette[Math.floorMod(line.colorIndex, palette.size)],
                focused = state.focusedId == line.id,
                useSystemKeyboard = state.useSystemKeyboard,
                playing = state.playingId == line.id,
                onFocus = { actions.onFocus(line.id) },
                onTextChange = { text, start, end -> actions.onTextChange(line.id, text, start, end) },
                onToggleVisible = { actions.onToggleVisible(line.id) },
                onOptions = { actions.onOptions(line.id) },
                onDelete = { actions.onDelete(line.id) },
                onSliderChange = { actions.onSliderChange(line.id, it) },
                onTogglePlay = { actions.onTogglePlay(line.id) },
                onAddSlider = { name -> actions.onAddSlider(name, line.id) },
            )
        }
        item(key = "add") {
            OutlinedButton(onClick = actions.onAddLine, modifier = Modifier.fillMaxWidth().testTag("add_expression")) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.graph_add_expression), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}
