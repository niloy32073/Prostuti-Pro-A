package com.prostuti.student.ui.screen.playground.graph

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.domain.calculator.AngleUnit
import com.prostuti.student.domain.GetSavedGraphUseCase
import com.prostuti.student.domain.SaveGraphUseCase
import com.prostuti.student.domain.graph.CompiledGraph
import com.prostuti.student.domain.graph.GraphCompiler
import com.prostuti.student.domain.graph.GraphLineInput
import com.prostuti.student.domain.graph.GraphLineKind
import com.prostuti.student.domain.graph.GraphNumberFormat
import com.prostuti.student.domain.graph.GraphPlot
import com.prostuti.student.domain.graph.GraphSpace
import com.prostuti.student.domain.graph.MathNode
import com.prostuti.student.domain.graph.MathParser
import com.prostuti.student.domain.graph.MathPrinter
import com.prostuti.student.domain.graph.MathTree
import com.prostuti.student.domain.graph.ParseResult
import com.prostuti.student.domain.graph.TableInput
import com.prostuti.student.domain.graph.BinOp
import com.prostuti.student.domain.graph.Axis3
import com.prostuti.student.domain.graph.space.SpaceGeometry
import com.prostuti.student.domain.graph.space.SpacePlotter
import com.prostuti.student.ui.screen.playground.graph.space.SpaceCamera
import com.prostuti.student.ui.screen.playground.graph.space.SpaceSettings
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
    private val spacePlotter: SpacePlotter,
    private val getSavedGraph: GetSavedGraphUseCase,
    private val saveGraph: SaveGraphUseCase,
    /** Open this "My graphs" entry (null = a new graph). */
    private val savedGraphId: Long? = null,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }
    private var nextId: Long = 1

    /** What the content looked like when last saved/opened: differs → unsaved changes. */
    private var cleanSignature: String = ""

    private val _state = MutableStateFlow(restore())
    val state: StateFlow<GraphBuilderUiState> = _state.asStateFlow()

    private data class RenderRequest(
        val inputs: List<GraphLineInput>,
        val angle: AngleUnit,
        val options: Map<Long, LineOptions>,
        val visible: Set<Long>,
        val viewport: PlotViewport?,
        val selectedId: Long?,
        val space: GraphSpace,
        /** 3D: half-size of the box to mesh for. */
        val range: Double,
    )

    private val renderRequests = MutableStateFlow<RenderRequest?>(null)
    private var lastCompileKey: Triple<List<GraphLineInput>, AngleUnit, GraphSpace>? = null
    private var lastGraph: CompiledGraph = CompiledGraph.EMPTY

    /** Latest graph, for tracing on the main thread. */
    @Volatile private var graph: CompiledGraph = CompiledGraph.EMPTY
    private var computedViewport: PlotViewport? = null
    private var meshRange: Double = 0.0
    private var playJob: Job? = null

    init {
        viewModelScope.launch {
            renderRequests.filterNotNull().conflate().collect { render(it) }
        }
        cleanSignature = savedStateHandle.get<String>(KEY_CLEAN) ?: signature(_state.value)
        val fresh = savedStateHandle.get<String>(KEY_GRAPH) == null
        if (fresh && savedGraphId != null) open(savedGraphId)
        requestRender()
    }

    // ── Persistence ─────────────────────────────────────────────────────────────────────────

    private fun restore(): GraphBuilderUiState {
        val saved = savedStateHandle.get<String>(KEY_GRAPH)?.let {
            runCatching { json.decodeFromString(SavedGraph.serializer(), it) }.getOrNull()
        }
        if (saved != null && saved.lines.isNotEmpty()) return stateFrom(saved, GraphBuilderUiState())
        return GraphBuilderUiState(
            planeLines = listOf(GraphLineState(id = nextId++, colorIndex = 0)),
            spaceLines = listOf(GraphLineState(id = nextId++, colorIndex = 0)),
        )
    }

    private fun stateFrom(saved: SavedGraph, base: GraphBuilderUiState): GraphBuilderUiState {
        nextId = maxOf(nextId, saved.nextId)
        return base.copy(
            planeLines = saved.lines,
            spaceLines = saved.spaceLines.ifEmpty { listOf(GraphLineState(id = nextId++, colorIndex = 0)) },
            space = saved.space,
            settings = saved.settings,
            viewport = saved.viewport,
            camera = saved.camera,
            spaceSettings = saved.spaceSettings,
            savedGraphId = saved.savedId,
            savedName = saved.savedName,
        )
    }

    private fun snapshot(s: GraphBuilderUiState) =
        SavedGraph(s.planeLines, s.settings, s.viewport, nextId, s.spaceLines, s.space, s.camera, s.spaceSettings, s.savedGraphId, s.savedName)

    /** Keeps the graph in SavedStateHandle (process death) and refreshes the unsaved-changes flag. */
    private fun save() {
        val s = _state.value
        savedStateHandle[KEY_GRAPH] = json.encodeToString(SavedGraph.serializer(), snapshot(s))
        savedStateHandle[KEY_CLEAN] = cleanSignature
        val dirty = signature(s) != cleanSignature
        if (dirty != s.isDirty) _state.update { it.copy(isDirty = dirty) }
    }

    /** Only what the student made counts as a change — panning or turning the view does not. */
    private fun signature(s: GraphBuilderUiState): String =
        (s.planeLines + s.spaceLines).joinToString("\u0001") { l ->
            "${l.text}|${l.table}|${l.options}|${l.slider}|${l.colorIndex}|${l.visible}"
        } + "|" + s.settings.angleUnit

    // ── My graphs ───────────────────────────────────────────────────────────────────────────

    private fun open(id: Long) {
        viewModelScope.launch {
            val entity = getSavedGraph(id)
            val saved = entity?.let { runCatching { json.decodeFromString(SavedGraph.serializer(), it.content) }.getOrNull() }
            if (saved == null) {
                _state.update { it.copy(message = GraphMessage.OPEN_FAILED) }
                return@launch
            }
            _state.update { stateFrom(saved.copy(savedId = id, savedName = entity.name), it) }
            cleanSignature = signature(_state.value)
            computedViewport = null
            save()
            requestRender()
        }
    }

    /** Toolbar "save": overwrite the existing entry, or ask for a name the first time. */
    fun requestSave(exitAfter: Boolean = false) {
        val name = _state.value.savedName
        if (name == null) {
            _state.update { it.copy(dialog = GraphDialog.SaveName(exitAfter)) }
        } else {
            saveAs(name, exitAfter)
        }
    }

    fun saveAs(name: String, exitAfter: Boolean = false) {
        if (name.isBlank()) return
        val s = _state.value
        val preview = (s.planeLines + s.spaceLines)
            .mapNotNull { l -> l.table?.let { "table" } ?: l.text.takeIf { it.isNotBlank() && !it.trimStart().startsWith("#") } }
            .take(PREVIEW_LINES)
            .joinToString("   ")
        val content = json.encodeToString(SavedGraph.serializer(), snapshot(s.copy(savedName = name.trim())))
        viewModelScope.launch {
            val id = runCatching { saveGraph(s.savedGraphId, name, content, preview, s.isSpace) }.getOrNull()
            if (id == null) {
                _state.update { it.copy(message = GraphMessage.SAVE_FAILED) }
                return@launch
            }
            _state.update {
                it.copy(
                    savedGraphId = id, savedName = name.trim(), dialog = null, message = GraphMessage.SAVED,
                    exitRequested = exitAfter,
                )
            }
            cleanSignature = signature(_state.value)
            save()
        }
    }

    /** Back pressed: leave at once, or ask first when there are unsaved changes. */
    fun requestExit() {
        val s = _state.value
        val empty = (s.planeLines + s.spaceLines).all { it.text.isBlank() && it.table == null }
        if (s.isDirty && !empty) _state.update { it.copy(dialog = GraphDialog.UnsavedChanges) }
        else _state.update { it.copy(exitRequested = true) }
    }

    fun discardAndExit() = _state.update { it.copy(dialog = null, exitRequested = true) }
    fun dismissDialog() = _state.update { it.copy(dialog = null) }
    fun onMessageShown() = _state.update { it.copy(message = null) }
    fun onExitHandled() = _state.update { it.copy(exitRequested = false) }

    // ── Rendering ───────────────────────────────────────────────────────────────────────────

    private fun requestRender() {
        val s = _state.value
        val live = s.plotViewport
        val target = when {
            live == null -> null
            computedViewport?.covers(live) == true -> computedViewport
            else -> live.expanded(PLOT_MARGIN)
        }
        // 3D meshes are reused while the zoom stays within ~15% of the range they were made for.
        val range = s.spaceSettings.range
        if (meshRange <= 0 || abs(kotlin.math.ln(range / meshRange)) > MESH_RANGE_TOLERANCE) meshRange = range
        renderRequests.value = RenderRequest(
            inputs = s.lines.map { line ->
                GraphLineInput(line.id, line.text, line.table?.let { TableInput(it.xs, it.ys, it.model) })
            },
            angle = s.settings.angleUnit,
            options = s.lines.associate { it.id to it.options },
            visible = s.lines.filter { it.visible }.mapTo(HashSet()) { it.id },
            viewport = if (s.isSpace) null else target,
            selectedId = s.selectedId ?: s.focusedId,
            space = s.space,
            range = if (s.isSpace) meshRange else 0.0,
        )
    }

    private suspend fun render(request: RenderRequest) {
        if (request.space == GraphSpace.SPACE) {
            renderSpace(request)
            return
        }
        val compileKey = Triple(request.inputs, request.angle, request.space)
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
            s.copy(statuses = mergeStatuses(s, statusesFor(compiled, geometries)), geometries = geometries)
        }
    }

    private suspend fun renderSpace(request: RenderRequest) {
        val compileKey = Triple(request.inputs, request.angle, request.space)
        val (compiled, meshes) = withContext(computeDispatcher) {
            val compiled = if (compileKey == lastCompileKey) lastGraph else compiler.compile(request.inputs, request.angle, GraphSpace.SPACE)
            lastCompileKey = compileKey
            lastGraph = compiled
            val drawn = compiled.lines.filter { it.id in request.visible && it.plot != null }
            val meshes = coroutineScope {
                drawn.map { line ->
                    async { spacePlotter.plot(line, request.options[line.id] ?: LineOptions(), request.range) }
                }.awaitAll().filterNotNull()
            }
            compiled to meshes
        }
        _state.update { s ->
            s.copy(statuses = mergeStatuses(s, statusesFor(compiled, emptyList())), spaceGeometries = meshes)
        }
    }

    /** Keeps the other view's statuses (ids are unique across both lists) and drops deleted lines. */
    private fun mergeStatuses(s: GraphBuilderUiState, fresh: Map<Long, LineStatus>): Map<Long, LineStatus> {
        val alive = (s.planeLines + s.spaceLines).mapTo(HashSet()) { it.id }
        return (s.statuses + fresh).filterKeys { it in alive }
    }

    private fun statusesFor(compiled: CompiledGraph, geometries: List<LineGeometry>): Map<Long, LineStatus> =
        compiled.lines.associate { line ->
            val integral = line.plot as? GraphPlot.IntegralArea
            val table = line.plot as? GraphPlot.Table
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
                pointCount = table?.xs?.size ?: 0,
                fitRSquared = table?.result?.rSquared,
                fitR = table?.result?.r,
                fitText = table?.result?.display,
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
        if (_state.value.isSpace) { zoomSpace(factor); return }
        val s = _state.value
        onTransform(0f, 0f, factor, s.canvasWidthPx / 2f, s.canvasHeightPx / 2f)
    }

    fun resetView() {
        if (_state.value.isSpace) { resetCamera(); return }
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

    /** Second (v) range of a parametric surface. */
    fun setVRangeTexts(id: Long, min: String, max: String) {
        val lo = evaluate(min) ?: return
        val hi = evaluate(max) ?: return
        if (!(hi > lo)) return
        val line = _state.value.line(id) ?: return
        setOptions(id, line.options.copy(vMin = lo, vMax = hi))
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
        _state.update { it.withLines(transform(it.lines)) }
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
            it.copy(focusedId = id, selectedId = id, keyboardVisible = true, trace = null, selectedPoint = null, panelCollapsed = false)
        }
        requestRender()
    }

    /**
     * Phone layout: slide the equation panel (list + keyboard) down to a thin bar so the graph gets
     * the screen, or bring it back up. Collapsing ends editing, so the keyboard closes too.
     */
    fun setPanelCollapsed(collapsed: Boolean) = _state.update {
        if (collapsed) it.copy(panelCollapsed = true, focusedId = null, keyboardVisible = false) else it.copy(panelCollapsed = false)
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
        _state.update { it.copy(focusedId = line.id, selectedId = line.id, keyboardVisible = true, panelCollapsed = false) }
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

    /**
     * Adds an example or template ([note] is its translated note). With [replace] the current list
     * is cleared first — templates define their own sliders (g, v₀…), which would clash otherwise.
     */
    fun addExample(example: GraphExample, note: String? = null, replace: Boolean = false) {
        if (example.space != _state.value.isSpace) setSpace(if (example.space) GraphSpace.SPACE else GraphSpace.PLANE)
        stopPlaying()
        val keep = if (replace) emptyList() else _state.value.lines.filter { it.text.isNotBlank() || it.table != null }
        val added = mutableListOf<GraphLineState>()
        note?.let { added += newLine("# $it", keep + added) }
        for (text in example.lines) {
            val name = PARAMETER_NAME.find(text)?.groupValues?.get(1)
            val line = newLine(text, keep + added)
            added += example.sliders[name]?.let { line.copy(slider = it) } ?: line
        }
        example.table?.let { added += newLine("", keep + added).copy(table = it) }
        updateLines { keep + added }
        _state.update {
            it.copy(sheet = null, focusedId = null, keyboardVisible = false, selectedId = added.lastOrNull { l -> !l.text.startsWith("#") }?.id)
        }
        example.window?.let { w ->
            if (example.space) {
                updateSpaceSettings(_state.value.spaceSettings.copy(range = w[0]))
            } else if (w.size == 4) {
                applyRanges(w[0], w[1], w[2], w[3])
            }
        }
        requestRender()
    }

    /** A new data table line under the focused line (or at the end). */
    fun addTable() {
        val line = newLine().copy(table = TableState())
        val after = _state.value.focusedId
        updateLines { lines ->
            val index = after?.let { id -> lines.indexOfFirst { it.id == id } }?.takeIf { it >= 0 }
            val rest = lines.filterNot { it.text.isBlank() && it.table == null && it.id != after }
            if (index == null) rest + line else rest.toMutableList().apply { add(minOf(index + 1, size), line) }
        }
        _state.update { it.copy(focusedId = null, keyboardVisible = false, selectedId = line.id) }
    }

    fun setTable(id: Long, table: TableState) = updateLine(id) { it.copy(table = table) }

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

    // ── 3D ──────────────────────────────────────────────────────────────────────────────────

    fun setSpace(space: GraphSpace) {
        if (_state.value.space == space) return
        stopPlaying()
        _state.update {
            it.copy(space = space, focusedId = null, selectedId = null, keyboardVisible = false, trace = null, selectedPoint = null, sheet = null)
        }
        save()
        requestRender()
    }

    fun rotateCamera(dYaw: Float, dPitch: Float) {
        _state.update { it.copy(camera = it.camera.rotated(dYaw, dPitch)) }
    }

    /** Pinch in 3D: a bigger factor shows a smaller box (zoom in). */
    fun zoomSpace(factor: Float) {
        if (!(factor > 0f) || !factor.isFinite()) return
        _state.update {
            val range = (it.spaceSettings.range / factor).coerceIn(MIN_SPACE_RANGE, MAX_SPACE_RANGE)
            it.copy(spaceSettings = it.spaceSettings.copy(range = range))
        }
        save()
        requestRender()
    }

    fun resetCamera() {
        _state.update { it.copy(camera = SpaceCamera(), spaceSettings = it.spaceSettings.copy(range = SpaceSettings.DEFAULT_RANGE)) }
        save()
        requestRender()
    }

    fun updateSpaceSettings(settings: SpaceSettings) {
        _state.update { it.copy(spaceSettings = settings.copy(range = settings.range.coerceIn(MIN_SPACE_RANGE, MAX_SPACE_RANGE))) }
        save()
        requestRender()
    }

    fun setSpaceRangeText(text: String) {
        val r = evaluate(text) ?: return
        if (r > 0) updateSpaceSettings(_state.value.spaceSettings.copy(range = r))
    }

    /**
     * Turns a 2D integral line ∫_a^b f dx (or ∫(f − g)dx) into a 3D solid of revolution about [axis]:
     * writes `revolvex(f, [g,] a, b)` in the 3D list, copies the definitions it uses, opens 3D.
     */
    fun revolve(lineId: Long, axis: Axis3) {
        val s = _state.value
        val source = s.planeLines.firstOrNull { it.id == lineId } ?: return
        val functions = s.planeLines.mapNotNull { GraphCompiler.definedFunctionName(it.text) }.toSet()
        val statement = (MathParser.parse(source.text, functions) as? ParseResult.Success)?.statement ?: return
        val integral = statement.operands.singleOrNull() as? MathNode.Integral ?: return
        val v = integral.variable
        fun asX(n: MathNode) = if (v == "x") n else MathTree.renameFree(n, v, "x")
        val body = integral.body
        val between = body is MathNode.Bin && body.op == BinOp.SUB &&
            MathTree.dependsOn(body.left, v) && MathTree.dependsOn(body.right, v)
        val parts = if (between) listOf(asX((body as MathNode.Bin).left), asX(body.right)) else listOf(asX(body))
        val args = parts + listOf(integral.lower, integral.upper)
        val name = if (axis == Axis3.Y) "revolvey" else "revolvex"
        val text = name + "(" + args.joinToString(", ") { MathPrinter.print(it) } + ")"

        // Bring along the parameters and functions the integral uses.
        val needed = args.flatMap { MathTree.freeSymbols(it) + MathTree.calledFunctions(it) }.toMutableSet()
        val copied = ArrayList<String>()
        var changed = true
        while (changed) {
            changed = false
            for (line in s.planeLines) {
                val defined = GraphCompiler.definedFunctionName(line.text) ?: ASSIGNMENT.find(line.text)?.groupValues?.get(1)
                if (defined == null || defined in GraphCompiler.RESERVED_VARIABLES || defined !in needed || line.text in copied) continue
                copied += line.text
                val st = (MathParser.parse(line.text, functions) as? ParseResult.Success)?.statement ?: continue
                st.operands.forEach { needed += MathTree.freeSymbols(it) + MathTree.calledFunctions(it) }
                changed = true
            }
        }
        val ordered = s.planeLines.map { it.text }.filter { it in copied && s.spaceLines.none { l -> l.text == it } }
        val fit = fitRange(source.text, functions, s.planeLines)
        _state.update {
            it.copy(
                space = GraphSpace.SPACE, sheet = null, focusedId = null, keyboardVisible = false,
                camera = SpaceCamera(),
                spaceSettings = if (fit != null) it.spaceSettings.copy(range = fit) else it.spaceSettings,
            )
        }
        val existing = _state.value.spaceLines.filter { it.text.isNotBlank() }
        val added = ArrayList<GraphLineState>()
        for (t in ordered + text) added += newLine(t, existing + added)
        updateLines { existing + added }
        _state.update { it.copy(selectedId = added.last().id) }
    }

    /**
     * A box that fits the solid made from the 2D integral line: the larger of its x-extent and the
     * curve's height (the radius after turning), plus a margin.
     */
    private fun fitRange(text: String, functions: Set<String>, planeLines: List<GraphLineState>): Double? {
        val graph = GraphCompiler().compile(planeLines.map { GraphLineInput(it.id, it.text) }, _state.value.settings.angleUnit)
        val area = graph.lines.firstOrNull { line -> planeLines.firstOrNull { it.id == line.id }?.text == text }
            ?.plot as? GraphPlot.IntegralArea ?: return null
        if (!area.lower.isFinite() || !area.upper.isFinite()) return null
        var extent = maxOf(abs(area.lower), abs(area.upper))
        val top = area.top.asFunctionOf(0)
        val bottom = area.bottom?.asFunctionOf(0)
        for (i in 0..200) {
            val x = area.lower + (area.upper - area.lower) * i / 200
            top.value(x).takeIf { it.isFinite() }?.let { extent = maxOf(extent, abs(it)) }
            bottom?.value(x)?.takeIf { it.isFinite() }?.let { extent = maxOf(extent, abs(it)) }
        }
        return (extent * 1.25).coerceIn(MIN_SPACE_RANGE, MAX_SPACE_RANGE).takeIf { extent > 0 }
    }

    override fun onCleared() {
        playJob?.cancel()
        save()
    }

    companion object {
        private const val KEY_GRAPH = "graph_builder_state"
        private const val KEY_CLEAN = "graph_builder_clean"
        private const val PREVIEW_LINES = 3
        private const val PLOT_MARGIN = 0.25
        private const val DEFAULT_SPAN = 20.0
        private const val MIN_UNITS_PER_PX = 1e-9
        private const val MAX_UNITS_PER_PX = 1e7
        private const val FRAME_MS = 16L
        private const val SLIDER_SWEEP_FRAMES = 240.0
        private const val RIEMANN_FRAME_MS = 220L
        private const val RIEMANN_ANIMATION_MAX = 100
        private const val MESH_RANGE_TOLERANCE = 0.15
        private const val MIN_SPACE_RANGE = 0.05
        private const val MAX_SPACE_RANGE = 1e5
        private val PARAMETER_NAME = Regex("""^\s*([A-Za-zα-ωΑ-Ω](?:_[A-Za-z0-9]+)?)\s*=""")
        private val ASSIGNMENT = Regex("""^\s*([A-Za-zα-ωΑ-Ω](?:_[A-Za-z0-9]+)?)\s*=""")
    }
}
