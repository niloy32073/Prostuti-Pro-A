package com.prostuti.student.ui.screen.playground.graph

import com.prostuti.student.domain.graph.plot.PlotViewport
import kotlinx.serialization.Serializable

/** The visible window: its centre and zoom (world units per pixel on each axis). */
@Serializable
data class GraphViewport(
    val centerX: Double = 0.0,
    val centerY: Double = 0.0,
    val unitsPerPxX: Double = 0.0,
    val unitsPerPxY: Double = 0.0,
) {
    val isInitialized: Boolean get() = unitsPerPxX > 0 && unitsPerPxY > 0

    fun toPlot(widthPx: Int, heightPx: Int): PlotViewport = PlotViewport(
        xMin = centerX - widthPx / 2.0 * unitsPerPxX,
        xMax = centerX + widthPx / 2.0 * unitsPerPxX,
        yMin = centerY - heightPx / 2.0 * unitsPerPxY,
        yMax = centerY + heightPx / 2.0 * unitsPerPxY,
        widthPx = widthPx,
        heightPx = heightPx,
    )
}
