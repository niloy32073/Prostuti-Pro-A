package com.prostuti.student.ui.screen.playground.code.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prostuti.student.R
import com.prostuti.student.domain.code.flow.EdgeLabel
import com.prostuti.student.domain.code.flow.FlowFunction
import com.prostuti.student.domain.code.flow.FlowMetrics
import com.prostuti.student.domain.code.flow.FlowNode
import com.prostuti.student.domain.code.flow.FlowchartLayout
import com.prostuti.student.domain.code.flow.NodeShape
import com.prostuti.student.domain.code.flow.TextWidth
import com.prostuti.student.ui.theme.MonoFontFamily
import com.prostuti.student.ui.theme.ProstutiTypography
import kotlin.math.hypot
import kotlin.math.min

private const val MIN_SCALE = 0.15f
private const val MAX_SCALE = 3f
internal const val NO_LIGATURES = "liga 0, calt 0"

/**
 * Draws one function's flowchart. Drag to pan, pinch (or mouse wheel) to zoom, tap a symbol to
 * pick its code line. Symbols from [selectedLine] are outlined; symbols of [activeLine] (where a
 * step-by-step run is paused) are filled, and the view pans to them when they are off screen.
 * The view re-fits whenever [fitRequest] or the function changes.
 */
@Composable
fun FlowchartView(
    function: FlowFunction,
    selectedLine: Int?,
    fitRequest: Int,
    onNodeTap: (FlowNode) -> Unit,
    modifier: Modifier = Modifier,
    activeLine: Int? = null,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Ligatures off: students must see `==`, `!=`, `<=` as typed, not as one glyph.
    val codeStyle = TextStyle(fontFamily = MonoFontFamily, fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = NO_LIGATURES)
    val labelStyle = ProstutiTypography.bnLabelMedium
    val startLabel = stringResource(R.string.code_flow_start)
    val endLabel = stringResource(R.string.code_flow_end)
    val yesText = stringResource(R.string.code_flow_yes)
    val noText = stringResource(R.string.code_flow_no)

    val metrics = remember(density, startLabel, endLabel) {
        FlowMetrics(
            lineHeight = measurer.measure("Ag", codeStyle).size.height.toFloat(),
            charWidth = measurer.measure("MMMMMMMMMM", codeStyle).size.width / 10f,
            startLabel = startLabel,
            endLabel = endLabel,
        )
    }
    val chart = remember(function, metrics) {
        FlowchartLayout(metrics, TextWidth { measurer.measure(it, codeStyle).size.width.toFloat() }).layout(function)
    }
    val nodeText = remember(chart) { chart.nodes.map { n -> n.lines.map { measurer.measure(it, codeStyle) } } }
    val yesLayout = remember(yesText, density) { measurer.measure(yesText, labelStyle) }
    val noLayout = remember(noText, density) { measurer.measure(noText, labelStyle) }

    var scale by rememberSaveable(function.name) { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable(function.name) { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable(function.name) { mutableFloatStateOf(0f) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var fittedFor by remember { mutableStateOf<Pair<Int, String>?>(null) }

    fun zoomAt(focus: Offset, factor: Float) {
        val next = (scale * factor).coerceIn(MIN_SCALE, MAX_SCALE)
        val k = next / scale
        offsetX = focus.x - (focus.x - offsetX) * k
        offsetY = focus.y - (focus.y - offsetY) * k
        scale = next
    }

    val controlsInset = with(density) { 64.dp.toPx() }

    // Fits beside the zoom buttons, so they never cover the chart's right-hand lines.
    fun fit() {
        if (viewSize.width == 0 || viewSize.height == 0) return
        val width = (viewSize.width - controlsInset).coerceAtLeast(viewSize.width / 2f)
        val s = min(width / chart.width, viewSize.height / chart.height).coerceIn(MIN_SCALE, 1.4f)
        scale = s
        offsetX = (width - chart.width * s) / 2
        offsetY = if (chart.height * s < viewSize.height) (viewSize.height - chart.height * s) / 2 else 0f
    }

    LaunchedEffect(fitRequest, function.name, viewSize) {
        val key = fitRequest to function.name
        if (viewSize != IntSize.Zero && fittedFor != key) { fit(); fittedFor = key }
    }
    // Keep the running step in sight: pan (not zoom) when its symbol is outside the view.
    LaunchedEffect(activeLine, chart, viewSize) {
        val node = chart.nodes.firstOrNull { it.sourceLine == activeLine } ?: return@LaunchedEffect
        if (viewSize == IntSize.Zero) return@LaunchedEffect
        val margin = with(density) { 24.dp.toPx() }
        val left = (node.x - node.width / 2) * scale + offsetX
        val right = (node.x + node.width / 2) * scale + offsetX
        val top = node.y * scale + offsetY
        val bottom = (node.y + node.height) * scale + offsetY
        val visibleWidth = viewSize.width - controlsInset
        if (left < margin || right > visibleWidth - margin) offsetX = visibleWidth / 2f - node.x * scale
        if (top < margin || bottom > viewSize.height - margin) offsetY = viewSize.height / 2f - (node.y + node.height / 2) * scale
    }

    val colors = MaterialTheme.colorScheme
    val palette = remember(colors) {
        mapOf(
            NodeShape.TERMINAL to (colors.tertiaryContainer to colors.onTertiaryContainer),
            NodeShape.PROCESS to (colors.surfaceContainerHigh to colors.onSurface),
            NodeShape.IO to (colors.primaryContainer to colors.onPrimaryContainer),
            NodeShape.DECISION to (colors.secondaryContainer to colors.onSecondaryContainer),
            NodeShape.CALL to (colors.surfaceContainerHighest to colors.onSurface),
            NodeShape.CONNECTOR to (colors.tertiaryContainer to colors.onTertiaryContainer),
        )
    }
    val chartDescription = stringResource(R.string.code_chart_desc, chart.nodes.size)

    Box(modifier.clipToBounds()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { viewSize = it }
                .semantics { contentDescription = chartDescription }
                .testTag("flowchart_canvas")
                .pointerInput(chart) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        zoomAt(centroid, zoom)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
                .pointerInput(chart) {
                    detectTapGestures { pos ->
                        val x = (pos.x - offsetX) / scale
                        val y = (pos.y - offsetY) / scale
                        chart.nodes.lastOrNull { x in it.x - it.width / 2..it.x + it.width / 2 && y in it.y..it.y + it.height }
                            ?.let(onNodeTap)
                    }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                val change = event.changes.first()
                                zoomAt(change.position, if (change.scrollDelta.y > 0) 0.9f else 1.1f)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                },
        ) {
            withTransform({
                translate(offsetX, offsetY)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                val stroke = 1.5.dp.toPx()
                val arrow = 7.dp.toPx()
                for (e in chart.edges) {
                    val path = Path().apply {
                        moveTo(e.points[0].x, e.points[0].y)
                        for (k in 1 until e.points.size) lineTo(e.points[k].x, e.points[k].y)
                    }
                    drawPath(path, colors.onSurfaceVariant, style = Stroke(stroke, join = StrokeJoin.Round))
                    if (e.arrow) drawArrowHead(e.points[e.points.size - 2].let { Offset(it.x, it.y) }, e.points.last().let { Offset(it.x, it.y) }, arrow, colors.onSurfaceVariant)
                    val label = when (e.label) { EdgeLabel.YES -> yesLayout; EdgeLabel.NO -> noLayout; null -> null }
                    val at = e.labelAt
                    if (label != null && at != null) {
                        val x = if (e.labelAlignEnd) at.x - label.size.width else at.x
                        drawText(label, colors.primary, Offset(x, at.y))
                    }
                }
                chart.nodes.forEachIndexed { i, n ->
                    val active = activeLine != null && n.sourceLine == activeLine
                    val (fill, onFill) = if (active) colors.primary to colors.onPrimary else palette.getValue(n.shape)
                    val selected = active || (selectedLine != null && n.sourceLine == selectedLine)
                    val outline = nodePath(n, metrics.charWidth)
                    drawPath(outline, fill, style = Fill)
                    drawPath(
                        outline,
                        if (selected) colors.primary else colors.outline,
                        style = Stroke(if (selected) 3.dp.toPx() else 1.dp.toPx(), join = StrokeJoin.Round),
                    )
                    if (n.shape == NodeShape.CALL) {
                        val bar = metrics.charWidth * FlowchartLayout.CALL_BAR_CHARS
                        val l = n.x - n.width / 2
                        val r = n.x + n.width / 2
                        drawLine(colors.outline, Offset(l + bar, n.y), Offset(l + bar, n.y + n.height), 1.dp.toPx())
                        drawLine(colors.outline, Offset(r - bar, n.y), Offset(r - bar, n.y + n.height), 1.dp.toPx())
                    }
                    val lines = nodeText[i]
                    val top = n.y + (n.height - lines.size * metrics.lineHeight) / 2
                    lines.forEachIndexed { k, layout ->
                        drawText(layout, onFill, Offset(n.x - layout.size.width / 2f, top + k * metrics.lineHeight))
                    }
                }
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
        ) {
            val center = { Offset(viewSize.width / 2f, viewSize.height / 2f) }
            FilledTonalIconButton(onClick = { zoomAt(center(), 1.25f) }, modifier = Modifier.testTag("flow_zoom_in")) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.code_zoom_in))
            }
            FilledTonalIconButton(onClick = { zoomAt(center(), 0.8f) }, modifier = Modifier.testTag("flow_zoom_out")) {
                Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.code_zoom_out))
            }
            FilledTonalIconButton(onClick = { fit() }, modifier = Modifier.testTag("flow_zoom_fit")) {
                Icon(Icons.Filled.FitScreen, contentDescription = stringResource(R.string.code_zoom_fit))
            }
        }
    }
}

/** The symbol's outline in chart coordinates. */
private fun nodePath(n: FlowNode, charWidth: Float): Path {
    val l = n.x - n.width / 2
    val r = n.x + n.width / 2
    val t = n.y
    val b = n.y + n.height
    return Path().apply {
        when (n.shape) {
            NodeShape.TERMINAL -> addRoundRect(
                RoundRect(l, t, r, b, CornerRadius(n.height / 2, n.height / 2)),
            )
            NodeShape.PROCESS, NodeShape.CALL -> addRoundRect(
                RoundRect(l, t, r, b, CornerRadius(charWidth * 0.5f, charWidth * 0.5f)),
            )
            NodeShape.IO -> {
                val s = n.height * FlowchartLayout.IO_SKEW
                moveTo(l + s, t); lineTo(r, t); lineTo(r - s, b); lineTo(l, b); close()
            }
            NodeShape.DECISION -> {
                moveTo(n.x, t); lineTo(r, t + n.height / 2); lineTo(n.x, b); lineTo(l, t + n.height / 2); close()
            }
            NodeShape.CONNECTOR -> addOval(Rect(l, t, r, b))
        }
    }
}

private fun DrawScope.drawArrowHead(from: Offset, tip: Offset, size: Float, color: Color) {
    val dx = tip.x - from.x
    val dy = tip.y - from.y
    val len = hypot(dx, dy)
    if (len == 0f) return
    val ux = dx / len
    val uy = dy / len
    val baseX = tip.x - ux * size
    val baseY = tip.y - uy * size
    val half = size * 0.5f
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(baseX - uy * half, baseY + ux * half)
        lineTo(baseX + uy * half, baseY - ux * half)
        close()
    }
    drawPath(path, color)
}
