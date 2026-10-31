package com.prostuti.student.domain.graph.plot

/** Shaded integral area split by sign, plus the Riemann approximation when asked for. */
internal class AreaShape(
    val positive: List<FloatArray>,
    val negative: List<FloatArray>,
    val guides: FloatArray,
    val riemannPolygons: List<FloatArray>,
    val riemannSum: Double?,
)
