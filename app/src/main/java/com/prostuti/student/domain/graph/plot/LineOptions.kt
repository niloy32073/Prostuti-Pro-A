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
)
