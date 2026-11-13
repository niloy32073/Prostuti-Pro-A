package com.prostuti.student.ui.screen.playground.graph.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prostuti.student.domain.graph.GraphNumberFormat
import com.prostuti.student.domain.graph.plot.GridStep
import com.prostuti.student.domain.graph.plot.GridTicks
import com.prostuti.student.domain.graph.plot.LineGeometry
import com.prostuti.student.domain.graph.plot.PlotViewport
import com.prostuti.student.domain.graph.plot.PointOfInterest
import com.prostuti.student.ui.screen.playground.graph.GraphColors
import com.prostuti.student.ui.screen.playground.graph.GraphLineState
import com.prostuti.student.ui.screen.playground.graph.GraphSettings
import com.prostuti.student.ui.screen.playground.graph.TracePoint

/**
 * The graph paper. Geometry arrives in the pixels of the window it was plotted for; each frame it is
 * mapped onto the live window with one transform, so pinch and pan run at full frame rate while a
 * new plot is being computed in the background.
 */
@Composable
fun GraphCanvas(
    viewport: PlotViewport?,
    settings: GraphSettings,
    lines: List<GraphLineState>,
    geometries: List<LineGeometry>,
    selectedId: Long?,
    trace: TracePoint?,
    selectedPoint: PointOfInterest?,
    contentDescription: String,
    onSizeChanged: (Int, Int) -> Unit,
    onTransform: (panX: Float, panY: Float, zoom: Float, centroidX: Float, centroidY: Float) -> Unit,
    onTap: (x: Float, y: Float, touchRadiusPx: Float) -> Unit,
    onTraceDrag: (x: Float, y: Float, touchRadiusPx: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = GraphColors.palette()
    val scheme = MaterialTheme.colorScheme
    val paper = scheme.surface
    val ink = scheme.onSurface
    val negativeColor = scheme.error
    val labelBackground = scheme.surfaceContainerHighest
    val measurer = rememberTextMeasurer()
    val paths = remember(geometries) { geometries.associate { it.lineId to (it to GeometryPaths(it)) } }

    val transform by rememberUpdatedState(onTransform)
    val tap by rememberUpdatedState(onTap)
    val drag by rememberUpdatedState(onTraceDrag)

    Canvas(
        modifier = modifier
            .clipToBounds()
            .testTag("graph_canvas")
            .semantics { this.contentDescription = contentDescription }
            .onSizeChanged { onSizeChanged(it.width, it.height) }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    transform(pan.x, pan.y, zoom, centroid.x, centroid.y)
                }
            }
            .pointerInput(Unit) {
                val radius = 24.dp.toPx()
                detectTapGestures(
                    onTap = { tap(it.x, it.y, radius) },
                    onDoubleTap = { transform(0f, 0f, 2f, it.x, it.y) },
                )
            }
            .pointerInput(Unit) {
                val radius = 32.dp.toPx()
                detectDragGesturesAfterLongPress(
                    onDragStart = { drag(it.x, it.y, radius) },
                    onDrag = { change, _ ->
                        change.consume()
                        drag(change.position.x, change.position.y, radius)
                    },
                )
            },
    ) {
        drawRect(paper)
        val vp = viewport ?: return@Canvas
        val xStep = GridTicks.step(vp.unitsPerPxX, 84.dp.toPx().toDouble(), settings.piXAxis)
        val yStep = GridTicks.step(vp.unitsPerPxY, 84.dp.toPx().toDouble(), piAxis = false)
        if (settings.showGrid) drawGrid(vp, xStep, yStep, ink)
        if (settings.showAxes) drawAxes(vp, ink)

        for (line in lines) {
            if (!line.visible) continue
            val (geometry, p) = paths[line.id] ?: continue
            val color = palette[Math.floorMod(line.colorIndex, palette.size)]
            drawGeometry(vp, geometry, p, color, negativeColor, selected = line.id == selectedId)
        }
        for (line in lines) {
            if (!line.visible) continue
            val (geometry, _) = paths[line.id] ?: continue
            val color = palette[Math.floorMod(line.colorIndex, palette.size)]
            drawMarkers(vp, geometry, color, ink, paper, labelBackground, measurer, line.id == selectedId, selectedPoint)
        }
        if (settings.showAxes && settings.showNumbers) drawAxisLabels(vp, xStep, yStep, ink, paper, measurer)
        trace?.let { t ->
            val line = lines.firstOrNull { it.id == t.lineId } ?: return@let
            val color = palette[Math.floorMod(line.colorIndex, palette.size)]
            val center = Offset(vp.px(t.x), vp.py(t.y))
            drawCircle(color, 7.dp.toPx(), center)
            drawCircle(paper, 3.dp.toPx(), center)
            val text = coordinate(t.x, t.y) + (t.slope?.let { "   m = ${GraphNumberFormat.format(it, 5)}" } ?: "")
            drawBubble(measurer, text, center, ink, labelBackground)
        }
    }
}

private fun coordinate(x: Double, y: Double) = "(${GraphNumberFormat.format(x, 5)}, ${GraphNumberFormat.format(y, 5)})"

private fun DrawScope.drawGrid(vp: PlotViewport, xStep: GridStep, yStep: GridStep, ink: Color) {
    val minor = ink.copy(alpha = 0.05f)
    val major = ink.copy(alpha = 0.13f)
    val thin = 1.dp.toPx()
    for (x in GridTicks.ticks(vp.xMin, vp.xMax, xStep.major / xStep.minorDivisions)) {
        val px = vp.px(x)
        drawLine(minor, Offset(px, 0f), Offset(px, size.height), thin)
    }
    for (y in GridTicks.ticks(vp.yMin, vp.yMax, yStep.major / yStep.minorDivisions)) {
        val py = vp.py(y)
        drawLine(minor, Offset(0f, py), Offset(size.width, py), thin)
    }
    for (x in GridTicks.ticks(vp.xMin, vp.xMax, xStep.major)) {
        val px = vp.px(x)
        drawLine(major, Offset(px, 0f), Offset(px, size.height), thin)
    }
    for (y in GridTicks.ticks(vp.yMin, vp.yMax, yStep.major)) {
        val py = vp.py(y)
        drawLine(major, Offset(0f, py), Offset(size.width, py), thin)
    }
}

private fun DrawScope.drawAxes(vp: PlotViewport, ink: Color) {
    val axis = ink.copy(alpha = 0.75f)
    val w = 1.5.dp.toPx()
    if (vp.yMin <= 0 && vp.yMax >= 0) drawLine(axis, Offset(0f, vp.py(0.0)), Offset(size.width, vp.py(0.0)), w)
    if (vp.xMin <= 0 && vp.xMax >= 0) drawLine(axis, Offset(vp.px(0.0), 0f), Offset(vp.px(0.0), size.height), w)
}

private fun DrawScope.drawAxisLabels(
    vp: PlotViewport,
    xStep: GridStep,
    yStep: GridStep,
    ink: Color,
    paper: Color,
    measurer: TextMeasurer,
) {
    val style = TextStyle(fontSize = 11.sp, color = ink.copy(alpha = 0.8f))
    val pad = 3.dp.toPx()
    // Labels sit on the axes, or stick to the nearest edge when an axis is off screen.
    val axisY = vp.py(0.0).coerceIn(0f, size.height - 16.dp.toPx())
    for (x in GridTicks.ticks(vp.xMin, vp.xMax, xStep.major)) {
        if (kotlin.math.abs(x) < xStep.major * 1e-6) continue
        val layout = measurer.measure(GridTicks.label(x, xStep), style)
        val left = vp.px(x) - layout.size.width / 2f
        if (left < 0 || left + layout.size.width > size.width) continue
        val top = axisY + pad
        drawRect(paper.copy(alpha = 0.7f), Offset(left - 1, top), Size(layout.size.width + 2f, layout.size.height.toFloat()))
        drawText(layout, topLeft = Offset(left, top))
    }
    val axisX = vp.px(0.0)
    for (y in GridTicks.ticks(vp.yMin, vp.yMax, yStep.major)) {
        if (kotlin.math.abs(y) < yStep.major * 1e-6) continue
        val layout = measurer.measure(GridTicks.label(y, yStep), style)
        val top = vp.py(y) - layout.size.height / 2f
        if (top < 0 || top + layout.size.height > size.height) continue
        val left = (axisX - layout.size.width - pad).coerceIn(pad, size.width - layout.size.width - pad)
        drawRect(paper.copy(alpha = 0.7f), Offset(left - 1, top), Size(layout.size.width + 2f, layout.size.height.toFloat()))
        drawText(layout, topLeft = Offset(left, top))
    }
    if (vp.xMin <= 0 && vp.xMax >= 0 && vp.yMin <= 0 && vp.yMax >= 0) {
        val layout = measurer.measure("0", style)
        drawText(layout, topLeft = Offset(axisX - layout.size.width - pad, vp.py(0.0) + pad))
    }
}

private fun DrawScope.drawGeometry(
    vp: PlotViewport,
    g: LineGeometry,
    p: GeometryPaths,
    color: Color,
    negativeColor: Color,
    selected: Boolean,
) {
    val gv = g.viewport
    val sx = (gv.unitsPerPxX / vp.unitsPerPxX).toFloat()
    val sy = (gv.unitsPerPxY / vp.unitsPerPxY).toFloat()
    val tx = ((gv.xMin - vp.xMin) / vp.unitsPerPxX).toFloat()
    val ty = ((vp.yMax - gv.yMax) / vp.unitsPerPxY).toFloat()
    val k = 2f / (sx + sy) // keeps stroke widths constant under the transform
    val width = (if (g.thinCurves) 1.6.dp.toPx() else if (selected) 3.4.dp.toPx() else 2.6.dp.toPx()) * k
    val dash = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx() * k, 6.dp.toPx() * k))
    val stroke = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = if (g.dashed) dash else null)
    withTransform({
        translate(tx, ty)
        scale(sx, sy, pivot = Offset.Zero)
    }) {
        if (p.hasFill) drawPath(p.fills, if (g.solidFills) color else color.copy(alpha = if (p.hasRiemann) 0.10f else 0.22f))
        if (p.hasNegative) drawPath(p.negativeFills, negativeColor.copy(alpha = if (p.hasRiemann) 0.10f else 0.22f))
        if (p.hasRiemann) {
            drawPath(p.riemann, color.copy(alpha = 0.22f))
            drawPath(p.riemann, color.copy(alpha = 0.85f), style = Stroke(1.2.dp.toPx() * k))
        }
        drawPath(p.guides, color.copy(alpha = 0.7f), style = Stroke(1.4.dp.toPx() * k, pathEffect = dash))
        drawPath(p.curves, color, style = stroke)
        drawPath(p.segments, color, style = Stroke(width, cap = StrokeCap.Round, pathEffect = if (g.dashed) dash else null))
        drawPath(p.derivative, color.copy(alpha = 0.6f), style = Stroke(width * 0.8f, cap = StrokeCap.Round, pathEffect = dash))
        drawPath(p.tangent, color.copy(alpha = 0.9f), style = Stroke(1.6.dp.toPx() * k, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawMarkers(
    vp: PlotViewport,
    g: LineGeometry,
    color: Color,
    ink: Color,
    paper: Color,
    labelBackground: Color,
    measurer: TextMeasurer,
    selected: Boolean,
    selectedPoint: PointOfInterest?,
) {
    for ((x, y) in g.points) {
        val c = Offset(vp.px(x), vp.py(y))
        drawCircle(color, if (g.pointLabels) 5.5.dp.toPx() else 4.5.dp.toPx(), c)
        if (!g.pointLabels) continue
        val layout = measurer.measure(coordinate(x, y), TextStyle(fontSize = 11.sp, color = ink))
        drawText(layout, topLeft = Offset(c.x + 8.dp.toPx(), c.y - layout.size.height - 2.dp.toPx()))
    }
    g.tangent?.let { t ->
        val c = Offset(vp.px(t.x), vp.py(t.y))
        drawCircle(color, 5.dp.toPx(), c)
        drawBubble(measurer, "m = ${GraphNumberFormat.format(t.slope, 5)}", c, ink, labelBackground)
    }
    if (!selected) return
    for (poi in g.pointsOfInterest) {
        val c = Offset(vp.px(poi.x), vp.py(poi.y))
        if (c.x < 0 || c.y < 0 || c.x > size.width || c.y > size.height) continue
        drawCircle(paper, 5.dp.toPx(), c)
        drawCircle(ink.copy(alpha = 0.55f), 5.dp.toPx(), c, style = Stroke(2.dp.toPx()))
    }
    selectedPoint?.let { poi ->
        if (g.pointsOfInterest.none { it == poi }) return@let
        val c = Offset(vp.px(poi.x), vp.py(poi.y))
        drawCircle(ink, 5.dp.toPx(), c)
        drawBubble(measurer, coordinate(poi.x, poi.y), c, ink, labelBackground)
    }
}

/** A rounded label above [anchor], kept inside the canvas. */
private fun DrawScope.drawBubble(measurer: TextMeasurer, text: String, anchor: Offset, ink: Color, background: Color) {
    val layout = measurer.measure(text, TextStyle(fontSize = 12.sp, color = ink))
    val padH = 8.dp.toPx()
    val padV = 4.dp.toPx()
    val w = layout.size.width + 2 * padH
    val h = layout.size.height + 2 * padV
    val left = (anchor.x - w / 2).coerceIn(4f, (size.width - w - 4f).coerceAtLeast(4f))
    var top = anchor.y - h - 12.dp.toPx()
    if (top < 4f) top = anchor.y + 12.dp.toPx()
    drawRoundRect(background, Offset(left, top), Size(w, h), CornerRadius(8.dp.toPx()))
    drawText(layout, topLeft = Offset(left + padH, top + padV))
}
