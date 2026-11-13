package com.prostuti.student.domain.graph.plot

import kotlinx.serialization.Serializable

/** Per-line drawing extras chosen in the card's settings. */
@Serializable
data class LineOptions(
    val showDerivative: Boolean = false,
    /** Draw the tangent at this x (explicit curves only). */
    val tangentAt: Double? = null,
    val riemann: RiemannMode = RiemannMode.NONE,
    val riemannCount: Int = 8,
    /** Parameter range for parametric/polar curves; null = a full turn. */
    val rangeMin: Double? = null,
    val rangeMax: Double? = null,
    /** Second parameter range (v) of a parametric surface. */
    val vMin: Double? = null,
    val vMax: Double? = null,
    // ── 3D ──
    /** Thin grid lines on a surface, like graph paper bent onto it. */
    val showWireframe: Boolean = true,
    /** Level curves z = c drawn on a surface. */
    val showContours: Boolean = false,
    /** Colour a surface by height (blue low → yellow high) instead of one colour. */
    val colorByHeight: Boolean = false,
    /** 1 = solid, lower = see-through. */
    val opacity: Double = 1.0,
)
