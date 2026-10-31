package com.prostuti.student.ui.screen.examengine.scratchpad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostuti.student.R
import com.prostuti.student.domain.scratchpad.ScratchStroke
import com.prostuti.student.domain.scratchpad.StrokePoint
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel

/** Stateful entry; the ViewModel is scoped to the exam's navigation entry, not the exam ViewModel. */
@Composable
fun ScratchpadPanel(
    modifier: Modifier = Modifier,
    viewModel: ScratchpadViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScratchpadContent(
        state = state,
        onSelectTool = viewModel::selectTool,
        onSelectEraserMode = viewModel::selectEraserMode,
        onStroke = viewModel::addStroke,
        onEraseStart = viewModel::beginErase,
        onErase = viewModel::eraseAlong,
        onEraseEnd = viewModel::endErase,
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
        onClear = viewModel::clear,
        modifier = modifier,
    )
}

@Composable
fun ScratchpadContent(
    state: ScratchpadUiState,
    onSelectTool: (ScratchTool) -> Unit,
    onSelectEraserMode: (EraserMode) -> Unit,
    onStroke: (List<StrokePoint>, Float) -> Unit,
    onEraseStart: () -> Unit,
    onErase: (StrokePoint, StrokePoint, Float) -> Unit,
    onEraseEnd: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Toolbar(state, onSelectTool, onUndo, onRedo, onClear)
        if (state.tool == ScratchTool.ERASER) EraserModeRow(state.eraserMode, onSelectEraserMode)
        DrawingSurface(
            state = state,
            onStroke = onStroke,
            onEraseStart = onEraseStart,
            onErase = onErase,
            onEraseEnd = onEraseEnd,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun Toolbar(
    state: ScratchpadUiState,
    onSelectTool: (ScratchTool) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val pen = stringResource(R.string.scratch_pen)
        val eraser = stringResource(R.string.scratch_eraser)
        WithTooltip(pen) {
            FilledTonalIconToggleButton(
                checked = state.tool == ScratchTool.PEN,
                onCheckedChange = { onSelectTool(ScratchTool.PEN) },
                modifier = Modifier.testTag(TAG_PEN),
            ) { Icon(Icons.Default.Edit, contentDescription = pen, modifier = Modifier.size(18.dp)) }
        }
        WithTooltip(eraser) {
            FilledTonalIconToggleButton(
                checked = state.tool == ScratchTool.ERASER,
                onCheckedChange = { onSelectTool(ScratchTool.ERASER) },
                modifier = Modifier.testTag(TAG_ERASER),
            ) { Icon(EraserIcon, contentDescription = eraser, modifier = Modifier.size(18.dp)) }
        }
        Spacer(Modifier.weight(1f))
        val undo = stringResource(R.string.scratch_undo)
        val redo = stringResource(R.string.scratch_redo)
        val clear = stringResource(R.string.scratch_clear)
        WithTooltip(undo) {
            IconButton(onClick = onUndo, enabled = state.canUndo, modifier = Modifier.testTag(TAG_UNDO)) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = undo, modifier = Modifier.size(18.dp))
            }
        }
        WithTooltip(redo) {
            IconButton(onClick = onRedo, enabled = state.canRedo, modifier = Modifier.testTag(TAG_REDO)) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = redo, modifier = Modifier.size(18.dp))
            }
        }
        WithTooltip(clear) {
            IconButton(onClick = onClear, enabled = state.strokes.isNotEmpty(), modifier = Modifier.testTag(TAG_CLEAR)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = clear,
                    tint = if (state.strokes.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun EraserModeRow(mode: EraserMode, onSelect: (EraserMode) -> Unit) {
    val options = listOf(
        EraserMode.PARTIAL to stringResource(R.string.scratch_eraser_partial),
        EraserMode.STROKE to stringResource(R.string.scratch_eraser_stroke),
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        options.forEachIndexed { index, (option, label) ->
            SegmentedButton(
                selected = mode == option,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = Modifier.testTag(if (option == EraserMode.PARTIAL) TAG_ERASER_PARTIAL else TAG_ERASER_STROKE),
                label = { Text(label, style = ProstutiTypography.bnLabelMedium, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun DrawingSurface(
    state: ScratchpadUiState,
    onStroke: (List<StrokePoint>, Float) -> Unit,
    onEraseStart: () -> Unit,
    onErase: (StrokePoint, StrokePoint, Float) -> Unit,
    onEraseEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val penWidthPx = with(density) { PEN_WIDTH.toPx() }
    val eraserRadiusPx = with(density) {
        (if (state.eraserMode == EraserMode.PARTIAL) PARTIAL_ERASER_RADIUS else STROKE_ERASER_RADIUS).toPx()
    }
    val outlineColor = MaterialTheme.colorScheme.outline

    // The line being drawn stays local (it changes every frame); it reaches the ViewModel when lifted.
    val currentStroke = remember { mutableStateListOf<Offset>() }
    var eraserPosition by remember { mutableStateOf<Offset?>(null) }

    val tool by rememberUpdatedState(state.tool)
    val radius by rememberUpdatedState(eraserRadiusPx)
    val penWidth by rememberUpdatedState(penWidthPx)
    val strokeDone by rememberUpdatedState(onStroke)
    val eraseStart by rememberUpdatedState(onEraseStart)
    val erase by rememberUpdatedState(onErase)
    val eraseEnd by rememberUpdatedState(onEraseEnd)

    val paths = remember(state.strokes) { state.strokes.map { it to it.toPath() } }

    Box(
        modifier = modifier
            .clipToBounds()
            .background(Color.White)
            .testTag(TAG_SURFACE)
            .pointerHoverIcon(PointerIcon.Crosshair)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val erasing = tool == ScratchTool.ERASER
                    var last = down.position
                    if (erasing) {
                        eraseStart()
                        eraserPosition = last
                        erase(last.toPoint(), last.toPoint(), radius)
                    } else {
                        currentStroke.clear()
                        currentStroke.add(last)
                    }
                    try {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            if (!change.positionChanged()) continue
                            change.consume()
                            val position = change.position
                            if (erasing) {
                                erase(last.toPoint(), position.toPoint(), radius)
                                eraserPosition = position
                                last = position
                            } else if ((position - last).getDistance() >= MIN_POINT_GAP_PX) {
                                currentStroke.add(position)
                                last = position
                            }
                        }
                    } finally {
                        if (erasing) {
                            eraseEnd()
                            eraserPosition = null
                        } else {
                            strokeDone(currentStroke.map { it.toPoint() }, penWidth)
                            currentStroke.clear()
                        }
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            paths.forEach { (stroke, path) -> drawScratchStroke(stroke.points, path, stroke.widthPx) }
            if (currentStroke.isNotEmpty()) {
                val points = currentStroke.map { it.toPoint() }
                drawScratchStroke(points, points.toPath(), penWidthPx)
            }
            eraserPosition?.let { center ->
                drawCircle(color = outlineColor.copy(alpha = 0.15f), radius = eraserRadiusPx, center = center)
                drawCircle(color = outlineColor, radius = eraserRadiusPx, center = center, style = Stroke(width = 1.dp.toPx()))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScratchStroke(
    points: List<StrokePoint>,
    path: Path,
    widthPx: Float,
) {
    if (points.size == 1) {
        drawCircle(color = INK, radius = widthPx / 2, center = Offset(points[0].x, points[0].y))
    } else {
        drawPath(path, color = INK, style = Stroke(width = widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun ScratchStroke.toPath(): Path = points.toPath()

/** Smooth line through the points: quadratic curves between segment midpoints. */
private fun List<StrokePoint>.toPath(): Path = Path().apply {
    if (isEmpty()) return@apply
    moveTo(this@toPath[0].x, this@toPath[0].y)
    for (i in 1 until lastIndex) {
        val p = this@toPath[i]
        val next = this@toPath[i + 1]
        quadraticTo(p.x, p.y, (p.x + next.x) / 2, (p.y + next.y) / 2)
    }
    if (size > 1) lineTo(last().x, last().y)
}

private fun Offset.toPoint() = StrokePoint(x, y)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithTooltip(text: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(),
    ) { content() }
}

/** A tilted eraser block over a baseline. */
private val EraserIcon: ImageVector by lazy {
    ImageVector.Builder(name = "Eraser", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(15f, 3.5f)
            lineTo(20.5f, 9f)
            lineTo(11f, 18.5f)
            lineTo(5.5f, 13f)
            close()
            moveTo(10.25f, 8.25f)
            lineTo(15.75f, 13.75f)
            moveTo(9f, 20.5f)
            lineTo(20f, 20.5f)
        }
        .build()
}

private val INK = Color(0xFF1B1B1F)
private val PEN_WIDTH = 2.5.dp
private val PARTIAL_ERASER_RADIUS = 12.dp
private val STROKE_ERASER_RADIUS = 6.dp
private const val MIN_POINT_GAP_PX = 1.5f

internal const val TAG_PEN = "scratch_pen"
internal const val TAG_ERASER = "scratch_eraser"
internal const val TAG_ERASER_PARTIAL = "scratch_eraser_partial"
internal const val TAG_ERASER_STROKE = "scratch_eraser_stroke"
internal const val TAG_UNDO = "scratch_undo"
internal const val TAG_REDO = "scratch_redo"
internal const val TAG_CLEAR = "scratch_clear"
internal const val TAG_SURFACE = "scratch_surface"
