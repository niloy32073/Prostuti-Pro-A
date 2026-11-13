package com.prostuti.student.ui.screen.playground.graph.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTypography
import kotlinx.coroutines.launch

/**
 * Phone layout: graph on top, equation panel (list + keyboard) below. The handle between them
 * slides the panel down to a thin bar so the graph gets almost the whole screen — tap it, or drag
 * it down / up. The graph height follows the finger while dragging.
 */
@Composable
fun CollapsiblePanelLayout(
    collapsed: Boolean,
    keyboardShown: Boolean,
    equationCount: Int,
    onCollapsedChange: (Boolean) -> Unit,
    graph: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    panel: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        // Sizes, not breakpoints: how much of this screen's height the graph takes in each state.
        val total = constraints.maxHeight.toFloat()
        val handlePx = with(density) { HANDLE_HEIGHT_COLLAPSED.toPx() }
        val openGraph = total * (if (keyboardShown) GRAPH_SHARE_WITH_KEYBOARD else GRAPH_SHARE)
        val closedGraph = (total - handlePx).coerceAtLeast(openGraph)
        val target = if (collapsed) closedGraph else openGraph

        val graphHeight = remember { Animatable(target) }
        var dragging by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        LaunchedEffect(target) { if (!dragging) graphHeight.animateTo(target, PANEL_SPRING) }

        Column(Modifier.fillMaxSize()) {
            graph(Modifier.fillMaxWidth().height(with(density) { graphHeight.value.toDp() }))
            PanelHandle(
                collapsed = collapsed,
                equationCount = equationCount,
                onToggle = { onCollapsedChange(!collapsed) },
                onDrag = { delta ->
                    dragging = true
                    scope.launch { graphHeight.snapTo((graphHeight.value + delta).coerceIn(openGraph, closedGraph)) }
                },
                onDragStopped = { velocity ->
                    dragging = false
                    val collapse = when {
                        velocity > FLING_VELOCITY -> true
                        velocity < -FLING_VELOCITY -> false
                        else -> graphHeight.value > (openGraph + closedGraph) / 2
                    }
                    if (collapse != collapsed) onCollapsedChange(collapse)
                    graphHeight.animateTo(if (collapse) closedGraph else openGraph, PANEL_SPRING)
                },
            )
            // Stays composed while hidden (zero height) so the list keeps its scroll position.
            Column(Modifier.weight(1f).fillMaxWidth(), content = panel)
        }
    }
}

@Composable
private fun PanelHandle(
    collapsed: Boolean,
    equationCount: Int,
    onToggle: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragStopped: suspend (Float) -> Unit,
) {
    val description = stringResource(if (collapsed) R.string.graph_panel_show else R.string.graph_panel_hide)
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Slim while open so the list keeps its room; taller when it is the only thing left.
            .height(if (collapsed) HANDLE_HEIGHT_COLLAPSED else HANDLE_HEIGHT_OPEN)
            .background(scheme.surfaceContainerLow)
            .draggable(
                state = rememberDraggableState(onDrag),
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> onDragStopped(velocity) },
            )
            .clickable(role = Role.Button, onClickLabel = description, onClick = onToggle)
            // Collapsed, the bar's own text ("2 equations") is read; open, it is icon-only.
            .then(if (collapsed) Modifier else Modifier.semantics { contentDescription = description })
            .testTag("panel_handle"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!collapsed) HorizontalDivider()
        if (collapsed) Grip(Modifier.padding(top = 6.dp))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (collapsed) 4.dp else 8.dp),
        ) {
            if (!collapsed) Grip()
            Icon(
                if (collapsed) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            if (collapsed) {
                Text(
                    pluralStringResource(R.plurals.graph_panel_count, equationCount, equationCount),
                    style = ProstutiTypography.bnLabelLarge,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.testTag("panel_count"),
                )
            }
        }
    }
}

/** The small pill that says "this can be dragged". */
@Composable
private fun Grip(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 36.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
    )
}

private val HANDLE_HEIGHT_OPEN = 28.dp
private val HANDLE_HEIGHT_COLLAPSED = 44.dp
private const val GRAPH_SHARE = 0.56f
private const val GRAPH_SHARE_WITH_KEYBOARD = 0.36f
private const val FLING_VELOCITY = 900f
private val PANEL_SPRING = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
