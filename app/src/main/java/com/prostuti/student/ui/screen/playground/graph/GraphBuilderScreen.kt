package com.prostuti.student.ui.screen.playground.graph

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.prostuti.student.R
import com.prostuti.student.domain.graph.GraphLineKind
import com.prostuti.student.domain.graph.GraphNumberFormat
import com.prostuti.student.ui.component.FormFactorPreviews
import com.prostuti.student.ui.screen.playground.graph.component.ExamplesSheet
import com.prostuti.student.ui.screen.playground.graph.component.ExpressionList
import com.prostuti.student.ui.screen.playground.graph.component.ExpressionListActions
import com.prostuti.student.ui.screen.playground.graph.component.GraphCanvas
import com.prostuti.student.ui.screen.playground.graph.component.GraphSettingsSheet
import com.prostuti.student.ui.screen.playground.graph.component.LineSettingsSheet
import com.prostuti.student.ui.screen.playground.graph.component.MathKeyboard
import com.prostuti.student.ui.theme.ProstutiTheme
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GraphBuilderScreen(
    onBack: () -> Unit,
    viewModel: GraphBuilderViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = GraphBuilderActions(
        onBack = onBack,
        onFocus = viewModel::onFocus,
        onTextChange = viewModel::onTextChange,
        onKey = viewModel::onKey,
        onAddLine = { viewModel.addLine() },
        onDeleteLine = viewModel::deleteLine,
        onMoveLine = viewModel::moveLine,
        onToggleVisible = viewModel::toggleVisible,
        onColor = viewModel::setColor,
        onOptions = viewModel::setOptions,
        onRangeTexts = viewModel::setRangeTexts,
        onSliderRangeTexts = viewModel::setSliderRangeTexts,
        onSliderChange = viewModel::onSliderChange,
        onTogglePlay = viewModel::togglePlay,
        onAddSlider = viewModel::addSlider,
        onAddExample = viewModel::addExample,
        onCanvasSize = viewModel::onCanvasSize,
        onTransform = viewModel::onTransform,
        onTap = viewModel::onTap,
        onTraceDrag = viewModel::onTraceDrag,
        onClearTrace = viewModel::clearTrace,
        onTangentAtTrace = viewModel::tangentAtTrace,
        onZoom = viewModel::zoomBy,
        onResetView = viewModel::resetView,
        onSquareAxes = viewModel::squareAxes,
        onSettings = viewModel::updateSettings,
        onApplyRanges = viewModel::applyRangeTexts,
        onOpenSheet = viewModel::openSheet,
        onCloseSheet = viewModel::closeSheet,
    )
    // Back first closes the keyboard, then leaves the screen.
    BackHandler(enabled = state.keyboardVisible) { viewModel.onKey(MathKeyAction.Hide) }
    GraphBuilderContent(state, actions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphBuilderContent(state: GraphBuilderUiState, actions: GraphBuilderActions) {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    // Side by side when the window is wide, or too short to stack graph, list and keyboard.
    val sideBySide = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ||
        !windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val showKeyboard = state.keyboardVisible && state.focusedId != null && !state.useSystemKeyboard

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.graph_title), style = ProstutiTypography.bnTitleLarge) },
                navigationIcon = {
                    ToolbarButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), "graph_back", actions.onBack)
                },
                actions = {
                    ToolbarButton(Icons.Filled.Lightbulb, stringResource(R.string.graph_examples), "open_examples") {
                        actions.onOpenSheet(GraphSheet.Examples)
                    }
                    ToolbarButton(Icons.Filled.Tune, stringResource(R.string.graph_settings), "open_settings") {
                        actions.onOpenSheet(GraphSheet.Settings)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        val listActions = ExpressionListActions(
            onFocus = actions.onFocus,
            onTextChange = actions.onTextChange,
            onToggleVisible = actions.onToggleVisible,
            onOptions = { actions.onOpenSheet(GraphSheet.LineSettings(it)) },
            onDelete = actions.onDeleteLine,
            onSliderChange = actions.onSliderChange,
            onTogglePlay = actions.onTogglePlay,
            onAddSlider = actions.onAddSlider,
            onAddLine = actions.onAddLine,
        )
        if (sideBySide) {
            Row(Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.width(SIDE_PANEL_WIDTH).fillMaxHeight()) {
                    ExpressionList(state, listActions, Modifier.weight(1f).fillMaxWidth())
                    if (showKeyboard) MathKeyboard(state.keyboardPage, actions.onKey)
                }
                VerticalDivider()
                GraphArea(state, actions, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                GraphArea(state, actions, Modifier.weight(1f).fillMaxWidth())
                HorizontalDivider()
                ExpressionList(state, listActions, Modifier.weight(if (showKeyboard) 0.55f else 0.8f).fillMaxWidth())
                if (showKeyboard) MathKeyboard(state.keyboardPage, actions.onKey)
            }
        }
    }

    when (val sheet = state.sheet) {
        GraphSheet.Settings -> GraphSettingsSheet(
            settings = state.settings,
            viewport = state.plotViewport,
            onSettings = actions.onSettings,
            onApplyRanges = actions.onApplyRanges,
            onResetView = actions.onResetView,
            onSquareAxes = actions.onSquareAxes,
            onDismiss = actions.onCloseSheet,
        )
        GraphSheet.Examples -> ExamplesSheet(onPick = actions.onAddExample, onDismiss = actions.onCloseSheet)
        is GraphSheet.LineSettings -> {
            val line = state.line(sheet.lineId)
            if (line != null) {
                val index = state.lines.indexOf(line)
                LineSettingsSheet(
                    line = line,
                    status = state.status(line.id),
                    geometry = state.geometries.firstOrNull { it.lineId == line.id },
                    viewport = state.plotViewport,
                    canMoveUp = index > 0,
                    canMoveDown = index < state.lines.lastIndex,
                    onColor = { actions.onColor(line.id, it) },
                    onOptions = { actions.onOptions(line.id, it) },
                    onRangeTexts = { min, max -> actions.onRangeTexts(line.id, min, max) },
                    onSliderRangeTexts = { min, max, step -> actions.onSliderRangeTexts(line.id, min, max, step) },
                    onMove = { actions.onMoveLine(line.id, it) },
                    onDelete = { actions.onDeleteLine(line.id) },
                    onDismiss = actions.onCloseSheet,
                )
            }
        }
        null -> Unit
    }
}

@Composable
private fun GraphArea(state: GraphBuilderUiState, actions: GraphBuilderActions, modifier: Modifier) {
    Box(modifier) {
        GraphCanvas(
            viewport = state.plotViewport,
            settings = state.settings,
            lines = state.lines,
            geometries = state.geometries,
            selectedId = state.selectedId,
            trace = state.trace,
            selectedPoint = state.selectedPoint,
            contentDescription = stringResource(R.string.graph_title),
            onSizeChanged = actions.onCanvasSize,
            onTransform = actions.onTransform,
            onTap = actions.onTap,
            onTraceDrag = actions.onTraceDrag,
            modifier = Modifier.fillMaxSize(),
        )
        if (state.lines.all { it.text.isBlank() }) EmptyHint()
        TraceChip(state, actions)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
        ) {
            ZoomButton(Icons.Filled.Add, stringResource(R.string.graph_zoom_in), "zoom_in") { actions.onZoom(2f) }
            ZoomButton(Icons.Filled.Remove, stringResource(R.string.graph_zoom_out), "zoom_out") { actions.onZoom(0.5f) }
            ZoomButton(Icons.Filled.CenterFocusStrong, stringResource(R.string.graph_reset_view), "reset_view", actions.onResetView)
        }
    }
}

@Composable
private fun BoxScope.EmptyHint() {
    Text(
        stringResource(R.string.graph_empty_graph),
        style = ProstutiTypography.bnBodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.align(Alignment.Center).widthIn(max = 280.dp).padding(16.dp),
    )
}

@Composable
private fun BoxScope.TraceChip(state: GraphBuilderUiState, actions: GraphBuilderActions) {
    val trace = state.trace ?: return
    val isExplicit = state.status(trace.lineId).isExplicit
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.align(Alignment.TopStart).padding(8.dp).testTag("trace_chip"),
    ) {
        AssistChip(
            onClick = actions.onClearTrace,
            label = {
                val slope = trace.slope?.let { "   " + stringResource(R.string.graph_trace_slope, GraphNumberFormat.format(it, 5)) }.orEmpty()
                Text("(${GraphNumberFormat.format(trace.x, 6)}, ${GraphNumberFormat.format(trace.y, 6)})$slope")
            },
            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null) },
        )
        if (isExplicit) {
            AssistChip(
                onClick = actions.onTangentAtTrace,
                label = { Text(stringResource(R.string.graph_tangent_here)) },
                modifier = Modifier.testTag("tangent_here"),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarButton(icon: ImageVector, label: String, tag: String, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.testTag(tag)) { Icon(icon, contentDescription = label) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoomButton(icon: ImageVector, label: String, tag: String, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Start),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.testTag(tag),
        ) { Icon(icon, contentDescription = label) }
    }
}

private val SIDE_PANEL_WIDTH = 360.dp

@FormFactorPreviews
@Composable
private fun GraphBuilderPreview() {
    val lines = listOf(GraphLineState(1, "y = x^2", colorIndex = 0), GraphLineState(2, "∫_0^2 x^2 dx", colorIndex = 1))
    val statuses = mapOf(
        1L to LineStatus(GraphLineKind.EXPLICIT, latex = "y = x^{2}", isExplicit = true, drawsSomething = true),
        2L to LineStatus(GraphLineKind.INTEGRAL, latex = "\\int_{0}^{2}x^{2}\\,dx", value = 8.0 / 3, exact = "8/3", drawsSomething = true),
    )
    ProstutiTheme { GraphBuilderContent(GraphBuilderUiState(lines = lines, statuses = statuses), GraphBuilderActions()) }
}
