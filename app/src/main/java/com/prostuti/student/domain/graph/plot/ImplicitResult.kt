package com.prostuti.student.domain.graph.plot

/**
 * Implicit plot output in viewport pixels: contour [segments] as (x1, y1, x2, y2)…, and for regions
 * the inside as merged [rects] (l, t, r, b)… plus [polygons] for cells the boundary cuts.
 */
internal class ImplicitResult(
    val segments: FloatArray,
    val rects: FloatArray,
    val polygons: List<FloatArray>,
)
