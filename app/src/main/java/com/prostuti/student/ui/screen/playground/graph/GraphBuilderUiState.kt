package com.prostuti.student.ui.screen.playground.graph

import com.prostuti.student.domain.graph.GraphSpace
import com.prostuti.student.domain.graph.plot.LineGeometry
import com.prostuti.student.domain.graph.plot.PlotViewport
import com.prostuti.student.domain.graph.plot.PointOfInterest
import com.prostuti.student.domain.graph.space.SpaceGeometry
import com.prostuti.student.ui.screen.playground.graph.space.SpaceCamera
import com.prostuti.student.ui.screen.playground.graph.space.SpaceSettings

data class GraphBuilderUiState(
    /** The 2D list and the 3D list are separate; [lines] is the one on screen. */
    val planeLines: List<GraphLineState> = emptyList(),
    val spaceLines: List<GraphLineState> = emptyList(),
    val space: GraphSpace = GraphSpace.PLANE,
    val statuses: Map<Long, LineStatus> = emptyMap(),
    val focusedId: Long? = null,
    /** Line whose points of interest are shown (the focused one, or the last tapped curve). */
    val selectedId: Long? = null,
    val keyboardVisible: Boolean = false,
    val useSystemKeyboard: Boolean = false,
    val keyboardPage: KeyboardPage = KeyboardPage.MAIN,
    /** Phone layout only: the equation panel is slid down to a bar under the graph. */
    val panelCollapsed: Boolean = false,
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
    val spaceSettings: SpaceSettings = SpaceSettings(),
    val camera: SpaceCamera = SpaceCamera(),
    val spaceGeometries: List<SpaceGeometry> = emptyList(),
    /** "My graphs" entry this graph was opened from or saved as. */
    val savedGraphId: Long? = null,
    val savedName: String? = null,
    /** Changed since the last save (or since opening). */
    val isDirty: Boolean = false,
    val dialog: GraphDialog? = null,
    /** One-shot: show "saved" once, then [GraphBuilderViewModel.onMessageShown]. */
    val message: GraphMessage? = null,
    /** One-shot: the screen should close (after save or discard). */
    val exitRequested: Boolean = false,
) {
    val isSpace: Boolean get() = space == GraphSpace.SPACE

    /** The lines of the view on screen (2D or 3D). */
    val lines: List<GraphLineState> get() = if (isSpace) spaceLines else planeLines

    fun withLines(lines: List<GraphLineState>): GraphBuilderUiState =
        if (isSpace) copy(spaceLines = lines) else copy(planeLines = lines)

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
