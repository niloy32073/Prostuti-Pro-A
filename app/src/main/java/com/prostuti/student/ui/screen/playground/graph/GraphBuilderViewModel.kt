package com.prostuti.student.ui.screen.playground.graph

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.domain.calculator.AngleUnit
import com.prostuti.student.domain.graph.CompiledGraph
import com.prostuti.student.domain.graph.GraphCompiler
import com.prostuti.student.domain.graph.GraphLineInput
import com.prostuti.student.domain.graph.GraphLineKind
import com.prostuti.student.domain.graph.GraphNumberFormat
import com.prostuti.student.domain.graph.GraphPlot
import com.prostuti.student.domain.graph.plot.GraphPlotter
import com.prostuti.student.domain.graph.plot.LineGeometry
import com.prostuti.student.domain.graph.plot.LineOptions
import com.prostuti.student.domain.graph.plot.PlotViewport
import com.prostuti.student.domain.graph.plot.RiemannMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

/**
 * Owns the expression list, the view window and the plotting pipeline.
 *
 * Plotting runs on [computeDispatcher] with "latest wins" (conflated) requests: while you drag a
 * slider or pan, the canvas keeps drawing the previous result mapped to the new window, and a fresh
 * plot replaces it as soon as it is ready. Each plot covers the window plus a margin, so small pans
 * need no new plot at all.
 */
class GraphBuilderViewModel(
    private val compiler: GraphCompiler,
    private val plotter: GraphPlotter,
    private val computeDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }
    private var nextId: Long = 1

    private val _state = MutableStateFlow(restore())
    val state: StateFlow<GraphBuilderUiState> = _state.asStateFlow()

    private data class RenderRequest(
        val inputs: List<GraphLineInput>,
        val angle: AngleUnit,
        val options: Map<Long, LineOptions>,
        val visible: Set<Long>,
        val viewport: PlotViewport?,
        val selectedId: Long?,
    )

    private val renderRequests = MutableStateFlow<RenderRequest?>(null)
    private var lastCompileKey: Pair<List<GraphLineInput>, AngleUnit>? = null
    private var lastGraph: CompiledGraph = CompiledGraph.EMPTY

    /** Latest graph, for tracing on the main thread. */
    @Volatile private var graph: CompiledGraph = CompiledGraph.EMPTY
    private var computedViewport: PlotViewport? = null
    private var playJob: Job? = null

    init {
        viewModelScope.launch {
            renderRequests.filterNotNull().conflate().collect { render(it) }
        }
        requestRender()
    }

    // ── Persistence ─────────────────────────────────────────────────────────────────────────

    private fun restore(): GraphBuilderUiState {
        val saved = savedStateHandle.get<String>(KEY_GRAPH)?.let {
            runCatching { json.decodeFromString(SavedGraph.serializer(), it) }.getOrNull()
        }
        if (saved != null && saved.lines.isNotEmpty()) {
            nextId = saved.nextId
            return GraphBuilderUiState(lines = saved.lines, settings = saved.settings, viewport = saved.viewport)
        }
        return GraphBuilderUiState(lines = listOf(GraphLineState(id = nextId++, colorIndex = 0)))
    }

    private fun save() {
        val s = _state.value
        savedStateHandle[KEY_GRAPH] = json.encodeToString(SavedGraph.serializer(), SavedGraph(s.lines, s.settings, s.viewport, nextId))
    }

    // ── Rendering ───────────────────────────────────────────────────────────────────────────

    private fun requestRender() {
        val s = _state.value
        val live = s.plotViewport
        val target = when {
            live == null -> null
            computedViewport?.covers(live) == true -> computedViewport
            else -> live.expanded(PLOT_MARGIN)
        }
        renderRequests.value = RenderRequest(
            inputs = s.lines.map { GraphLineInput(it.id, it.text) },
            angle = s.settings.angleUnit,
            options = s.lines.associate { it.id to it.options },
            visible = s.lines.filter { it.visible }.mapTo(HashSet()) { it.id },
            viewport = target,
            selectedId = s.selectedId ?: s.focusedId,
        )
    }

    private suspend fun render(request: RenderRequest) {
        val compileKey = request.inputs to request.angle
        val (compiled, geometries) = withContext(computeDispatcher) {
            val compiled = if (compileKey == lastCompileKey) lastGraph else compiler.compile(request.inputs, request.angle)
            lastCompileKey = compileKey
            lastGraph = compiled
            val viewport = request.viewport
            val drawn = compiled.lines.filter { it.id in request.visible && it.plot != null }
            val explicit = drawn.mapNotNull { it.plot as? GraphPlot.ExplicitY }
            val geometries = if (viewport == null) {
                emptyList()
            } else {
                coroutineScope {
                    drawn.map { line ->
                        async {
                            plotter.plot(
                                line, request.options[line.id] ?: LineOptions(), viewport,
                                findPoints = line.id == request.selectedId, others = explicit,
                            )
                        }
                    }.awaitAll().filterNotNull()
                }
            }
            compiled to geometries
        }
        graph = compiled
        computedViewport = request.viewport
        _state.update { s ->
            s.copy(statuses = statusesFor(compiled, geometries), geometries = geometries)
        }
    }

    private fun statusesFor(compiled: CompiledGraph, geometries: List<LineGeometry>): Map<Long, LineStatus> =
        compiled.lines.associate { line ->
            val integral = line.plot as? GraphPlot.IntegralArea
            val value = line.value
            line.id to LineStatus(
                kind = line.kind,
                latex = line.latex,
                error = line.error,
                value = value,
                exact = value?.let { GraphNumberFormat.exactForm(it) },
                parameterName = line.parameter?.name,
                isSlider = line.parameter?.isSlider == true,
                integralConverged = integral?.result?.converged ?: true,
                integralBetweenCurves = integral?.bottom != null,
                riemannSum = geometries.firstOrNull { it.lineId == line.id }?.riemannSum,
                isExplicit = line.plot is GraphPlot.ExplicitY,
                drawsSomething = line.plot != null,
            )
        }

    // ── Canvas ──────────────────────────────────────────────────────────────────────────────

    fun onCanvasSize(widthPx: Int, heightPx: Int) {
        if (widthPx <= 0 || heightPx <= 0) return
        val s = _state.value
        if (s.canvasWidthPx == widthPx && s.canvasHeightPx == heightPx && s.viewport.isInitialized) return
        _state.update {
            val viewport = if (it.viewport.isInitialized) it.viewport else defaultViewport(widthPx, heightPx)
            it.copy(canvasWidthPx = widthPx, canvasHeightPx = heightPx, viewport = viewport)
        }
        requestRender()
    }

    private fun defaultViewport(widthPx: Int, heightPx: Int): GraphViewport {
        val units = DEFAULT_SPAN / min(widthPx, heightPx)
        return GraphViewport(0.0, 0.0, units, units)
    }

    /** Pinch/drag: [zoom] about the [centroidX], [centroidY] pixel, then pan by ([panX], [panY]) px. */
    fun onTransform(panX: Float, panY: Float, zoom: Float, centroidX: Float, centroidY: Float) {
        val s = _state.value
        if (!s.viewport.isInitialized) return
        val w = s.canvasWidthPx
        val h = s.canvasHeightPx
        val v = s.viewport
        val z = zoom.toDouble().takeIf { it.isFinite() && it > 0 } ?: 1.0
        val worldX = v.centerX + (centroidX - w / 2.0) * v.unitsPerPxX
        val worldY = v.centerY - (centroidY - h / 2.0) * v.unitsPerPxY
        val sx = (v.unitsPerPxX / z).coerceIn(MIN_UNITS_PER_PX, MAX_UNITS_PER_PX)
        val sy = (v.unitsPerPxY / z).coerceIn(MIN_UNITS_PER_PX, MAX_UNITS_PER_PX)
        val cx = worldX - (centroidX - w / 2.0) * sx - panX * sx
        val cy = worldY + (centroidY - h / 2.0) * sy + panY * sy
        setViewport(GraphViewport(cx, cy, sx, sy))
    }

    fun zoomBy(factor: Float) {
        val s = _state.value
        onTransform(0f, 0f, factor, s.canvasWidthPx / 2f, s.canvasHeightPx / 2f)
    }

    fun resetView() {
        val s = _state.value
        if (s.canvasWidthPx > 0) setViewport(defaultViewport(s.canvasWidthPx, s.canvasHeightPx))
    }

    fun squareAxes() {
        val v = _state.value.viewport
        setViewport(v.copy(unitsPerPxY = v.unitsPerPxX))
    }

    /** From the settings sheet's x/y range fields. */
    fun applyRanges(xMin: Double, xMax: Double, yMin: Double, yMax: Double) {
        val s = _state.value
        if (!(xMax > xMin) || !(yMax > yMin) || s.canvasWidthPx == 0) return
        setViewport(
            GraphViewport(
                (xMin + xMax) / 2, (yMin + yMax) / 2,
                (xMax - xMin) / s.canvasWidthPx, (yMax - yMin) / s.canvasHeightPx,
            ),
        )
    }

    /** Range fields accept expressions (-2π, 3/2): evaluated with a throw-away compiler. */
    private fun evaluate(text: String): Double? {
        if (text.isBlank()) return null
        val line = GraphCompiler().compile(listOf(GraphLineInput(0, text)), _state.value.settings.angleUnit).lines.first()
        return line.value?.takeIf { it.isFinite() && line.kind == GraphLineKind.VALUE }
    }

    fun applyRangeTexts(xMin: String, xMax: String, yMin: String, yMax: String) {
        val values = listOf(xMin, xMax, yMin, yMax).map(::evaluate)
        if (values.any { it == null }) return
        applyRanges(values[0]!!, values[1]!!, values[2]!!, values[3]!!)
    }

    /** Parameter range of a polar/parametric line. */
    fun setRangeTexts(id: Long, min: String, max: String) {
        val lo = evaluate(min) ?: return
        val hi = evaluate(max) ?: return
        if (!(hi > lo)) return
        val line = _state.value.line(id) ?: return
        setOptions(id, line.options.copy(rangeMin = lo, rangeMax = hi))
    }

    fun setSliderRangeTexts(id: Long, min: String, max: String, step: String) {
        val lo = evaluate(min) ?: return
        val hi = evaluate(max) ?: return
        val st = evaluate(step) ?: return
        setSliderRange(id, SliderRange(lo, hi, st))
    }

    private fun setViewport(viewport: GraphViewport) {
        _state.update { it.copy(viewport = viewport) }
        save()
        requestRender()
    }

    /** Tap: a point of interest of the selected curve, else the nearest curve point, else clears. */
    fun onTap(px: Float, py: Float, touchRadiusPx: Float) {
        val s = _state.value
        val vp = s.plotViewport ?: return
        val poi = s.geometries.firstOrNull { it.lineId == s.selectedId }?.pointsOfInterest
            ?.minByOrNull { hypot(vp.px(it.x) - px, vp.py(it.y) - py) }
            ?.takeIf { hypot(vp.px(it.x) - px, vp.py(it.y) - py) <= touchRadiusPx }
        if (poi != null) {
            _state.update { it.copy(selectedPoint = poi, trace = null) }
            return
        }
        val trace = nearestPoint(px, py, touchRadiusPx, onlyLine = null)
        _state.update {
            it.copy(
                trace = trace,
                selectedPoint = null,
                selectedId = trace?.lineId ?: it.selectedId,
                focusedId = if (trace == null) null else it.focusedId,
                keyboardVisible = if (trace == null) false else it.keyboardVisible,
            )
        }
        requestRender()
    }

    /** Long-press drag: follows the curve picked at the start of the drag. */
    fun onTraceDrag(px: Float, py: Float, touchRadiusPx: Float) {
        val current = _state.value.trace
        val trace = nearestPoint(px, py, if (current == null) touchRadiusPx else Float.MAX_VALUE, current?.lineId)
        if (trace != null) _state.update { it.copy(trace = trace, selectedPoint = null) }
    }

    fun clearTrace() = _state.update { it.copy(trace = null, selectedPoint = null) }

    private fun nearestPoint(px: Float, py: Float, radius: Float, onlyLine: Long?): TracePoint? {
        val s = _state.value
        val vp = s.plotViewport ?: return null
        val visible = s.lines.filter { it.visible && (onlyLine == null || it.id == onlyLine) }.map { it.id }.toSet()
        var best: TracePoint? = null
        var bestDist = radius
        val wx = vp.worldX(px)
        for (line in graph.lines) {
            if (line.id !in visible) continue
            val plot = line.plot as? GraphPlot.ExplicitY
            if (plot != null) {
                val y = plot.f.evaluate(wx)
                if (y.isFinite()) {
                    val d = abs(vp.py(y) - py)
                    if (d < bestDist) {
                        bestDist = d
                        best = TracePoint(line.id, wx, y, plot.derivative?.evaluate(wx)?.takeIf { it.isFinite() })
                    }
                }
                continue
            }
            val geometry = s.geometries.firstOrNull { it.lineId == line.id } ?: continue
            nearestOnGeometry(geometry, vp, px, py)?.let { (x, y, d) ->
                if (d < bestDist) {
                    bestDist = d
                    best = TracePoint(line.id, x, y)
                }
            }
        }
        return best
    }

    private fun nearestOnGeometry(g: LineGeometry, vp: PlotViewport, px: Float, py: Float): Triple<Double, Double, Float>? {
        var best: Triple<Double, Double, Float>? = null
        fun consider(gx: Float, gy: Float) {
            val x = g.viewport.worldX(gx)
            val y = g.viewport.worldY(gy)
            val d = hypot(vp.px(x) - px, vp.py(y) - py)
            if (best == null || d < best!!.third) best = Triple(x, y, d)
        }
        for (c in g.curves) for (i in c.indices step 2) consider(c[i], c[i + 1])
        g.segments?.let { s -> for (i in s.indices step 2) consider(s[i], s[i + 1]) }
        for ((x, y) in g.points) {
            val d = hypot(vp.px(x) - px, vp.py(y) - py)
            if (best == null || d < best!!.third) best = Triple(x, y, d)
        }
        return best
    }

    // ── Lines ───────────────────────────────────────────────────────────────────────────────

    private fun updateLines(render: Boolean = true, transform: (List<GraphLineState>) -> List<GraphLineState>) {
        _state.update { it.copy(lines = transform(it.lines)) }
        save()
        if (render) requestRender()
    }

    private fun updateLine(id: Long, render: Boolean = true, transform: (GraphLineState) -> GraphLineState) =
        updateLines(render) { lines -> lines.map { if (it.id == id) transform(it) else it } }

    /** A new line with the first colour not already used by [existing] (cycling once all are taken). */
    private fun newLine(text: String = "", existing: List<GraphLineState> = _state.value.lines): GraphLineState {
        val used = existing.map { it.colorIndex }
        val color = (0 until GraphColors.count).firstOrNull { it !in used } ?: (existing.size % GraphColors.count)
        return GraphLineState(id = nextId++, text = text, colorIndex = color)
    }

    fun onFocus(id: Long) {
        _state.update {
            it.copy(focusedId = id, selectedId = id, keyboardVisible = true, trace = null, selectedPoint = null)
        }
        requestRender()
    }

    fun onTextChange(id: Long, text: String, selectionStart: Int, selectionEnd: Int) {
        val line = _state.value.line(id) ?: return
        val textChanged = line.text != text
        updateLine(id, render = textChanged) { it.copy(text = text, selectionStart = selectionStart, selectionEnd = selectionEnd) }
    }

    fun addLine(afterId: Long? = null, text: String = "") {
        val line = newLine(text)
        updateLines { lines ->
            val index = afterId?.let { id -> lines.indexOfFirst { it.id == id } }?.takeIf { it >= 0 }
            if (index == null) lines + line else lines.toMutableList().apply { add(index + 1, line) }
        }
        _state.update { it.copy(focusedId = line.id, selectedId = line.id, keyboardVisible = true) }
    }

    fun deleteLine(id: Long) {
        if (_state.value.playingId == id) stopPlaying()
        updateLines { lines ->
            val rest = lines.filterNot { it.id == id }
            rest.ifEmpty { listOf(newLine()) }
        }
        _state.update {
            it.copy(
                focusedId = it.focusedId.takeIf { f -> f != id },
                selectedId = it.selectedId.takeIf { f -> f != id },
                trace = it.trace?.takeIf { t -> t.lineId != id },
                sheet = null,
            )
        }
    }

    fun moveLine(id: Long, delta: Int) = updateLines { lines ->
        val i = lines.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in lines.indices) lines else lines.toMutableList().apply { add(j, removeAt(i)) }
    }

    fun toggleVisible(id: Long) = updateLine(id) { it.copy(visible = !it.visible) }

    fun setColor(id: Long, colorIndex: Int) = updateLine(id, render = false) { it.copy(colorIndex = colorIndex) }

    fun setOptions(id: Long, options: LineOptions) = updateLine(id) { it.copy(options = options) }

    fun setSliderRange(id: Long, range: SliderRange) {
        if (!(range.max > range.min) || !(range.step > 0)) return
        updateLine(id, render = false) { it.copy(slider = range) }
    }

    /** Adds `name = 1` under [afterId] for a letter the line uses but nothing defines. */
    fun addSlider(name: String, afterId: Long) {
        val line = newLine("$name = 1")
        updateLines { lines ->
            val index = lines.indexOfFirst { it.id == afterId }
            lines.toMutableList().apply { add(if (index < 0) size else index + 1, line) }
        }
    }

    fun addExample(example: GraphExample) {
        val s = _state.value
        val keep = s.lines.filter { it.text.isNotBlank() }
        val added = mutableListOf<GraphLineState>()
        for (text in example.lines) added += newLine(text, keep + added)
        updateLines { keep + added }
        _state.update { it.copy(sheet = null, focusedId = null, keyboardVisible = false, selectedId = added.last().id) }
        requestRender()
    }

    fun onSliderChange(id: Long, value: Double) {
        val name = _state.value.status(id).parameterName ?: return
        val line = _state.value.line(id) ?: return
        val stepped = snap(value, line.slider)
        val text = "$name = ${formatSlider(stepped, line.slider.step)}"
        updateLine(id) { it.copy(text = text, selectionStart = text.length, selectionEnd = text.length) }
    }

    private fun snap(value: Double, range: SliderRange): Double {
        val k = round((value - range.min) / range.step)
        return (range.min + k * range.step).coerceIn(range.min, range.max)
    }

    private fun formatSlider(value: Double, step: Double): String {
        val decimals = (-floor(log10(step))).toInt().coerceIn(0, 6)
        val factor = 10.0.pow(decimals)
        val rounded = round(value * factor) / factor
        val text = if (decimals == 0) rounded.toLong().toString() else "%.${decimals}f".format(java.util.Locale.US, rounded).trimEnd('0').trimEnd('.')
        return if (text == "-0") "0" else text
    }

    /** Play/pause: sweeps a slider back and forth, or grows the Riemann rectangle count. */
    fun togglePlay(id: Long) {
        if (_state.value.playingId == id) { stopPlaying(); return }
        stopPlaying()
        val line = _state.value.line(id) ?: return
        val status = _state.value.status(id)
        _state.update { it.copy(playingId = id) }
        playJob = viewModelScope.launch {
            if (status.kind == GraphLineKind.INTEGRAL) {
                var n = 1
                while (isActive && n <= RIEMANN_ANIMATION_MAX) {
                    val options = (_state.value.line(id) ?: break).options
                    val mode = if (options.riemann == RiemannMode.NONE) RiemannMode.MIDPOINT else options.riemann
                    setOptions(id, options.copy(riemann = mode, riemannCount = n))
                    delay(RIEMANN_FRAME_MS)
                    n = if (n < 10) n + 1 else n + max(1, n / 5)
                }
                stopPlaying()
            } else {
                val range = line.slider
                var value = _state.value.status(id).value ?: range.min
                var direction = 1.0
                val stepPerFrame = (range.max - range.min) / SLIDER_SWEEP_FRAMES
                while (isActive) {
                    value += direction * stepPerFrame
                    if (value >= range.max) { value = range.max; direction = -1.0 }
                    if (value <= range.min) { value = range.min; direction = 1.0 }
                    onSliderChange(id, value)
                    delay(FRAME_MS)
                }
            }
        }
    }

    fun stopPlaying() {
        playJob?.cancel()
        playJob = null
        _state.update { it.copy(playingId = null) }
    }

    // ── Keyboard ────────────────────────────────────────────────────────────────────────────

    fun onKey(action: MathKeyAction) {
        val s = _state.value
        when (action) {
            MathKeyAction.Hide -> { _state.update { it.copy(keyboardVisible = false, focusedId = null) }; return }
            MathKeyAction.SystemKeyboard -> { _state.update { it.copy(useSystemKeyboard = !it.useSystemKeyboard) }; return }
            is MathKeyAction.Page -> { _state.update { it.copy(keyboardPage = action.page) }; return }
            else -> Unit
        }
        val id = s.focusedId ?: return
        val line = s.line(id) ?: return
        if (action == MathKeyAction.Enter) {
            addLine(afterId = id)
            return
        }
        val current = EditorValue(line.text, line.selectionStart, line.selectionEnd)
        val next = when (action) {
            is MathKeyAction.Insert -> MathInputEditor.insert(current, action.text, action.cursorBack)
            MathKeyAction.Backspace -> {
                if (line.text.isEmpty() && s.lines.size > 1) {
                    val index = s.lines.indexOfFirst { it.id == id }
                    deleteLine(id)
                    s.lines.getOrNull(index - 1)?.let { onFocus(it.id) }
                    return
                }
                MathInputEditor.backspace(current)
            }
            MathKeyAction.Left -> MathInputEditor.left(current)
            MathKeyAction.Right -> MathInputEditor.right(current)
            else -> current
        }
        onTextChange(id, next.text, next.selectionStart, next.selectionEnd)
    }

    // ── Settings & sheets ───────────────────────────────────────────────────────────────────

    fun updateSettings(settings: GraphSettings) {
        _state.update { it.copy(settings = settings) }
        save()
        requestRender()
    }

    fun openSheet(sheet: GraphSheet) = _state.update { it.copy(sheet = sheet, keyboardVisible = false) }
    fun closeSheet() = _state.update { it.copy(sheet = null) }

    /** Tangent at the traced point of an explicit curve. */
    fun tangentAtTrace() {
        val trace = _state.value.trace ?: return
        val line = _state.value.line(trace.lineId) ?: return
        setOptions(line.id, line.options.copy(tangentAt = trace.x))
    }

    override fun onCleared() {
        playJob?.cancel()
        save()
        super.onCleared()
    }

    companion object {
        private const val KEY_GRAPH = "graph_builder_state"
        private const val PLOT_MARGIN = 0.25
        private const val DEFAULT_SPAN = 20.0
        private const val MIN_UNITS_PER_PX = 1e-9
        private const val MAX_UNITS_PER_PX = 1e7
        private const val FRAME_MS = 16L
        private const val SLIDER_SWEEP_FRAMES = 240.0
        private const val RIEMANN_FRAME_MS = 220L
        private const val RIEMANN_ANIMATION_MAX = 100
    }
}
