package com.prostuti.student.domain.graph.plot

/**
 * A world rectangle drawn into a [widthPx] × [heightPx] pixel area. Pixel y grows downward.
 * Plot output is stored in this viewport's pixels, then mapped to the live viewport while the
 * user pans/zooms, so drawing never waits for a re-plot.
 */
data class PlotViewport(
    val xMin: Double,
    val xMax: Double,
    val yMin: Double,
    val yMax: Double,
    val widthPx: Int,
    val heightPx: Int,
) {
    val unitsPerPxX: Double get() = (xMax - xMin) / widthPx
    val unitsPerPxY: Double get() = (yMax - yMin) / heightPx

    fun px(x: Double): Float = ((x - xMin) / unitsPerPxX).toFloat()
    fun py(y: Double): Float = ((yMax - y) / unitsPerPxY).toFloat()
    fun worldX(px: Float): Double = xMin + px * unitsPerPxX
    fun worldY(py: Float): Double = yMax - py * unitsPerPxY

    val isValid: Boolean
        get() = widthPx > 0 && heightPx > 0 && xMax > xMin && yMax > yMin && xMin.isFinite() && yMax.isFinite()

    /** This viewport grown by [fraction] of its size on every side (same pixel density). */
    fun expanded(fraction: Double): PlotViewport {
        val extraW = (widthPx * fraction).toInt()
        val extraH = (heightPx * fraction).toInt()
        return PlotViewport(
            xMin - extraW * unitsPerPxX, xMax + extraW * unitsPerPxX,
            yMin - extraH * unitsPerPxY, yMax + extraH * unitsPerPxY,
            widthPx + 2 * extraW, heightPx + 2 * extraH,
        )
    }

    /** True when [inner] lies inside this viewport and the zoom differs by under 15%. */
    fun covers(inner: PlotViewport): Boolean {
        val zoomRatio = inner.unitsPerPxX / unitsPerPxX
        val zoomRatioY = inner.unitsPerPxY / unitsPerPxY
        return inner.xMin >= xMin && inner.xMax <= xMax && inner.yMin >= yMin && inner.yMax <= yMax &&
            zoomRatio in 0.87..1.15 && zoomRatioY in 0.87..1.15
    }
}
