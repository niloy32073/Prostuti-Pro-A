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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.prostuti.student.ui.screen.playground.graph.share.GraphCapture
import com.prostuti.student.ui.screen.playground.graph.share.GraphShareImage
import com.prostuti.student.ui.screen.playground.graph.share.ShareRows
import com.prostuti.student.ui.screen.playground.graph.share.recordInto
import kotlinx.coroutines.launch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.prostuti.student.domain.graph.GraphSpace
import com.prostuti.student.ui.screen.playground.graph.component.SpaceSettingsSheet
import com.prostuti.student.ui.screen.playground.graph.space.SpaceView
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.prostuti.student.domain.graph.GraphNumberFormat
import com.prostuti.student.ui.component.FormFactorPreviews
import com.prostuti.student.ui.screen.playground.graph.component.ExamplesSheet
import com.prostuti.student.ui.screen.playground.graph.component.CollapsiblePanelLayout
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
    val capture = remember { GraphCapture() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val palette = GraphColors.palette().map { it.toArgb() }
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current.density
    val undefined = stringResource(R.string.graph_value_undefined)
    val footer = "Prostuti · " + stringResource(R.string.graph_title)
    val chooser = stringResource(R.string.graph_share_chooser)
    val messages = mapOf(
        GraphMessage.SAVED to stringResource(R.string.graph_saved),
        GraphMessage.SAVE_FAILED to stringResource(R.string.graph_save_failed),
        GraphMessage.OPEN_FAILED to stringResource(R.string.graph_open_failed),
        GraphMessage.SHARE_FAILED to stringResource(R.string.graph_share_failed),
    )
    fun share() {
        scope.launch {
            val shot = capture.capture(state.isSpace)
            if (shot == null) {
                snackbar.showSnackbar(messages.getValue(GraphMessage.SHARE_FAILED))
                return@launch
            }
            val rows = ShareRows.from(state, palette, undefined)
            val image = GraphShareImage.compose(
                shot, rows, footer, scheme.surface.toArgb(), scheme.onSurface.toArgb(), scheme.onSurfaceVariant.toArgb(), density,
            )
            runCatching { GraphShareImage.share(context, image, chooser) }
                .onFailure { snackbar.showSnackbar(messages.getValue(GraphMessage.SHARE_FAILED)) }
        }
    }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        // Show from the screen's own scope: consuming the message changes this effect's key,
        // which would otherwise cancel the snackbar before it appears.
        scope.launch { snackbar.showSnackbar(messages.getValue(message)) }
        viewModel.onMessageShown()
    }
    LaunchedEffect(state.exitRequested) {
        if (state.exitRequested) {
            viewModel.onExitHandled()
            onBack()
        }
    }
    val actions = GraphBuilderActions(
        onBack = viewModel::requestExit,
        onSave = { viewModel.requestSave() },
        onSaveAndExit = { viewModel.requestSave(exitAfter = true) },
        onSaveAs = viewModel::saveAs,
        onShare = ::share,
        onDiscardAndExit = viewModel::discardAndExit,
        onDismissDialog = viewModel::dismissDialog,
        onFocus = viewModel::onFocus,
        onTextChange = viewModel::onTextChange,
        onKey = viewModel::onKey,
        onAddLine = { viewModel.addLine() },
        onPanelCollapsed = viewModel::setPanelCollapsed,
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
        onAddTable = viewModel::addTable,
        onTableChange = viewModel::setTable,
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
        onSpace = viewModel::setSpace,
        onRotateCamera = viewModel::rotateCamera,
        onZoomSpace = viewModel::zoomSpace,
        onResetCamera = viewModel::resetCamera,
        onSpaceSettings = viewModel::updateSpaceSettings,
        onSpaceRangeText = viewModel::setSpaceRangeText,
        onRevolve = viewModel::revolve,
        onVRangeTexts = viewModel::setVRangeTexts,
    )
    // Back first closes the keyboard, then leaves (asking first if there are unsaved changes).
    BackHandler(enabled = !state.keyboardVisible) { viewModel.requestExit() }
    BackHandler(enabled = state.keyboardVisible) { viewModel.onKey(MathKeyAction.Hide) }
    GraphBuilderContent(state, actions, capture, snackbar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphBuilderContent(
    state: GraphBuilderUiState,
    actions: GraphBuilderActions,
    capture: GraphCapture = remember { GraphCapture() },
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    GraphDialogs(state, actions)
    // A physics template picked while the list has work in it: ask replace / add / cancel.
    var pendingTemplate by remember { mutableStateOf<Pair<GraphExample, String?>?>(null) }
    pendingTemplate?.let { (example, note) ->
        AlertDialog(
            onDismissRequest = { pendingTemplate = null },
            title = { Text(stringResource(R.string.graph_template_replace_title)) },
            text = { Text(stringResource(R.string.graph_template_replace_body), style = ProstutiTypography.bnBodyMedium) },
            confirmButton = {
                TextButton(onClick = { pendingTemplate = null; actions.onAddExample(example, note, true) }, modifier = Modifier.testTag("template_replace")) {
                    Text(stringResource(R.string.graph_template_replace))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingTemplate = null }) { Text(stringResource(R.string.graph_cancel)) }
                    TextButton(onClick = { pendingTemplate = null; actions.onAddExample(example, note, false) }, modifier = Modifier.testTag("template_add")) {
                        Text(stringResource(R.string.graph_template_add))
                    }
                }
            },
        )
    }
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    // Side by side when the window is wide, or too short to stack graph, list and keyboard.
    val sideBySide = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ||
        !windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val showKeyboard = state.keyboardVisible && state.focusedId != null && !state.useSystemKeyboard

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { ModeToggle(state.space, actions.onSpace) },
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
                    MoreMenu(actions)
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
            onAddTable = actions.onAddTable,
            onTableChange = actions.onTableChange,
        )
        if (sideBySide) {
            Row(Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.width(SIDE_PANEL_WIDTH).fillMaxHeight()) {
                    ExpressionList(state, listActions, Modifier.weight(1f).fillMaxWidth())
                    if (showKeyboard) MathKeyboard(state.keyboardPage, actions.onKey, space = state.isSpace)
                }
                VerticalDivider()
                if (state.isSpace) SpaceArea(state, actions, Modifier.weight(1f).fillMaxHeight(), capture)
                else GraphArea(state, actions, Modifier.weight(1f).fillMaxHeight(), capture)
            }
        } else {
            CollapsiblePanelLayout(
                collapsed = state.panelCollapsed,
                keyboardShown = showKeyboard,
                equationCount = state.lines.count { it.table != null || (it.text.isNotBlank() && !it.text.trimStart().startsWith("#")) },
                onCollapsedChange = actions.onPanelCollapsed,
                graph = { graphModifier ->
                    if (state.isSpace) SpaceArea(state, actions, graphModifier, capture)
                    else GraphArea(state, actions, graphModifier, capture)
                },
                modifier = Modifier.padding(padding),
            ) {
                ExpressionList(state, listActions, Modifier.weight(1f).fillMaxWidth())
                if (showKeyboard) MathKeyboard(state.keyboardPage, actions.onKey, space = state.isSpace)
            }
        }
    }

    when (val sheet = state.sheet) {
        GraphSheet.Settings -> if (state.isSpace) {
            SpaceSettingsSheet(
                settings = state.spaceSettings,
                graphSettings = state.settings,
                onSettings = actions.onSpaceSettings,
                onGraphSettings = actions.onSettings,
                onRangeText = actions.onSpaceRangeText,
                onResetCamera = actions.onResetCamera,
                onDismiss = actions.onCloseSheet,
            )
        } else GraphSettingsSheet(
            settings = state.settings,
            viewport = state.plotViewport,
            onSettings = actions.onSettings,
            onApplyRanges = actions.onApplyRanges,
            onResetView = actions.onResetView,
            onSquareAxes = actions.onSquareAxes,
            onDismiss = actions.onCloseSheet,
        )
        GraphSheet.Examples -> ExamplesSheet(
            onPick = { example, note, isTemplate ->
                val target = if (example.space) state.spaceLines else state.planeLines
                if (isTemplate && target.any { it.text.isNotBlank() || it.table != null }) {
                    pendingTemplate = example to note
                } else {
                    actions.onAddExample(example, note, false)
                }
            },
            onDismiss = actions.onCloseSheet,
            categories = if (state.isSpace) GraphExamples.spaceCategories else GraphExamples.categories,
        )
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
                    onRevolve = { axis -> actions.onRevolve(line.id, axis) },
                    onVRangeTexts = { min, max -> actions.onVRangeTexts(line.id, min, max) },
                )
            }
        }
        null -> Unit
    }
}

@Composable
private fun GraphArea(state: GraphBuilderUiState, actions: GraphBuilderActions, modifier: Modifier, capture: GraphCapture) {
    val layer = rememberGraphicsLayer()
    SideEffect { capture.planeLayer = layer }
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
            modifier = Modifier.fillMaxSize().recordInto(layer),
        )
        if (state.lines.all { it.text.isBlank() && it.table == null }) EmptyHint()
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
private fun SpaceArea(state: GraphBuilderUiState, actions: GraphBuilderActions, modifier: Modifier, capture: GraphCapture) {
    Box(modifier) {
        SpaceView(
            geometries = state.spaceGeometries,
            lines = state.spaceLines,
            settings = state.spaceSettings,
            camera = state.camera,
            range = state.spaceSettings.range,
            contentDescription = stringResource(R.string.graph_mode_3d),
            onRotate = actions.onRotateCamera,
            onZoom = actions.onZoomSpace,
            onResetCamera = actions.onResetCamera,
            modifier = Modifier.fillMaxSize(),
            capture = capture,
        )
        if (state.spaceLines.all { it.text.isBlank() }) {
            EmptyHint(R.string.graph_empty_space)
        } else {
            Text(
                stringResource(R.string.graph_space_hint),
                style = ProstutiTypography.bnLabelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
        ) {
            ZoomButton(Icons.Filled.Add, stringResource(R.string.graph_zoom_in), "zoom_in") { actions.onZoomSpace(1.5f) }
            ZoomButton(Icons.Filled.Remove, stringResource(R.string.graph_zoom_out), "zoom_out") { actions.onZoomSpace(1f / 1.5f) }
            ZoomButton(Icons.Filled.CenterFocusStrong, stringResource(R.string.graph_reset_camera), "reset_view", actions.onResetCamera)
        }
    }
}

@Composable
private fun MoreMenu(actions: GraphBuilderActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        ToolbarButton(Icons.Filled.MoreVert, stringResource(R.string.graph_more), "graph_more") { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.graph_save)) },
                leadingIcon = { Icon(Icons.Filled.Save, contentDescription = null) },
                onClick = { open = false; actions.onSave() },
                modifier = Modifier.testTag("menu_save"),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.graph_share)) },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                onClick = { open = false; actions.onShare() },
                modifier = Modifier.testTag("menu_share"),
            )
        }
    }
}

/** Name-the-graph and leave-without-saving dialogs. */
@Composable
private fun GraphDialogs(state: GraphBuilderUiState, actions: GraphBuilderActions) {
    when (val dialog = state.dialog) {
        is GraphDialog.SaveName -> {
            val suggestion = state.savedName
                ?: state.lines.firstOrNull { it.text.isNotBlank() && !it.text.startsWith("#") }?.text?.take(40)
                ?: stringResource(R.string.graph_save_default_name)
            var name by rememberSaveable { mutableStateOf(suggestion) }
            AlertDialog(
                onDismissRequest = actions.onDismissDialog,
                title = { Text(stringResource(R.string.graph_save_title)) },
                text = {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it }, singleLine = true,
                        label = { Text(stringResource(R.string.graph_save_name)) },
                        modifier = Modifier.testTag("save_name"),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { actions.onSaveAs(name, dialog.exitAfter) }, enabled = name.isNotBlank(), modifier = Modifier.testTag("confirm_save")) {
                        Text(stringResource(R.string.graph_save))
                    }
                },
                dismissButton = { TextButton(onClick = actions.onDismissDialog) { Text(stringResource(R.string.graph_cancel)) } },
            )
        }
        GraphDialog.UnsavedChanges -> AlertDialog(
            onDismissRequest = actions.onDismissDialog,
            title = { Text(stringResource(R.string.graph_unsaved_title)) },
            text = { Text(stringResource(R.string.graph_unsaved_body), style = ProstutiTypography.bnBodyMedium) },
            confirmButton = {
                TextButton(onClick = { actions.onDismissDialog(); actions.onSaveAndExit() }, modifier = Modifier.testTag("unsaved_save")) {
                    Text(stringResource(R.string.graph_unsaved_save))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = actions.onDismissDialog) { Text(stringResource(R.string.graph_cancel)) }
                    TextButton(onClick = actions.onDiscardAndExit, modifier = Modifier.testTag("unsaved_discard")) {
                        Text(stringResource(R.string.graph_unsaved_discard), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
        )
        null -> Unit
    }
}

/** 2D | 3D switch in the top bar. */
@Composable
private fun ModeToggle(space: GraphSpace, onSpace: (GraphSpace) -> Unit) {
    val description = stringResource(R.string.graph_mode_switch)
    SingleChoiceSegmentedButtonRow(Modifier.widthIn(max = 220.dp).semantics { contentDescription = description }) {
        val modes = listOf(GraphSpace.PLANE to R.string.graph_mode_2d, GraphSpace.SPACE to R.string.graph_mode_3d)
        modes.forEachIndexed { i, (mode, label) ->
            SegmentedButton(
                selected = space == mode,
                onClick = { onSpace(mode) },
                shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                modifier = Modifier.testTag("mode_${mode.name}"),
                label = { Text(stringResource(label)) },
            )
        }
    }
}

@Composable
private fun BoxScope.EmptyHint(textRes: Int = R.string.graph_empty_graph) {
    Text(
        stringResource(textRes),
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
    ProstutiTheme { GraphBuilderContent(GraphBuilderUiState(planeLines = lines, statuses = statuses), GraphBuilderActions()) }
}
