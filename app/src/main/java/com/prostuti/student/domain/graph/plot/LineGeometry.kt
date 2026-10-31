package com.prostuti.student.domain.graph.plot

/**
 * Everything one line draws. Shapes are in [viewport] pixels (the viewport they were computed for);
 * points are in world coordinates so they stay exact while the view moves.
 */
class LineGeometry(
    val lineId: Long,
    val viewport: PlotViewport,
    val curves: List<FloatArray> = emptyList(),
    /** Strict inequality boundaries (y > f) are dashed, as in textbooks. */
    val dashed: Boolean = false,
    /** Implicit contour segments (x1, y1, x2, y2)…. */
    val segments: FloatArray? = null,
    /** Shaded polygons: regions, inequalities, positive integral area. */
    val fills: List<FloatArray> = emptyList(),
    /** Integral area below the x-axis (negative contribution). */
    val negativeFills: List<FloatArray> = emptyList(),
    /** Region interior as rectangles (l, t, r, b)…. */
    val fillRects: FloatArray? = null,
    /** f'(x) when "show derivative" is on. */
    val derivativeCurves: List<FloatArray> = emptyList(),
    /** Vertical guides at integral bounds, as segments. */
    val guides: FloatArray? = null,
    val riemannPolygons: List<FloatArray> = emptyList(),
    val riemannSum: Double? = null,
    val points: List<Pair<Double, Double>> = emptyList(),
    val pointsOfInterest: List<PointOfInterest> = emptyList(),
    val tangent: TangentInfo? = null,
)
