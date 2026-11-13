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
    /** Fills drawn opaque in the line colour (arrow heads), not as a light shade. */
    val solidFills: Boolean = false,
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
    /** Label each point with its coordinates (single points yes, table data no). */
    val pointLabels: Boolean = true,
    /** Draw curves thin (field arrows), not as the main graph stroke. */
    val thinCurves: Boolean = false,
    val pointsOfInterest: List<PointOfInterest> = emptyList(),
    val tangent: TangentInfo? = null,
)
