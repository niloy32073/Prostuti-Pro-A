package com.prostuti.student.ui.screen.playground.graph

import com.prostuti.student.domain.graph.plot.LineGeometry
import com.prostuti.student.domain.graph.plot.PlotViewport
import com.prostuti.student.domain.graph.plot.PointOfInterest

data class GraphBuilderUiState(
    val lines: List<GraphLineState> = emptyList(),
    val statuses: Map<Long, LineStatus> = emptyMap(),
    val focusedId: Long? = null,
    /** Line whose points of interest are shown (the focused one, or the last tapped curve). */
    val selectedId: Long? = null,
    val keyboardVisible: Boolean = false,
    val useSystemKeyboard: Boolean = false,
    val keyboardPage: KeyboardPage = KeyboardPage.MAIN,
    val settings: GraphSettings = GraphSettings(),
    val viewport: GraphViewport = GraphViewport(),
    val canvasWidthPx: Int = 0,
    val canvasHeightPx: Int = 0,
    val geometries: List<LineGeometry> = emptyList(),
    val trace: TracePoint? = null,
    val selectedPoint: PointOfInterest? = null,
    val sheet: GraphSheet? = null,
    /** Line id of a running slider / Riemann animation. */
    val playingId: Long? = null,
) {
    /** The live viewport in canvas pixels, or null before the canvas has been measured. */
    val plotViewport: PlotViewport?
        get() = if (canvasWidthPx > 0 && canvasHeightPx > 0 && viewport.isInitialized) {
            viewport.toPlot(canvasWidthPx, canvasHeightPx)
        } else {
            null
        }

    fun line(id: Long): GraphLineState? = lines.firstOrNull { it.id == id }
    fun status(id: Long): LineStatus = statuses[id] ?: LineStatus()
}
