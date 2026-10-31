package com.prostuti.student.ui.screen.playground.graph

import com.prostuti.student.domain.graph.GraphError
import com.prostuti.student.domain.graph.GraphLineKind

/** The compiler's verdict on a line, ready to show in its card. */
data class LineStatus(
    val kind: GraphLineKind = GraphLineKind.EMPTY,
    val latex: String? = null,
    val error: GraphError? = null,
    /** Constant value (2+3, ∫ value, b = 2a); NaN = undefined. */
    val value: Double? = null,
    /** "8/3", "π/2"… when the value has a simple exact form. */
    val exact: String? = null,
    val parameterName: String? = null,
    val isSlider: Boolean = false,
    val integralConverged: Boolean = true,
    val integralBetweenCurves: Boolean = false,
    val riemannSum: Double? = null,
    val isExplicit: Boolean = false,
    val drawsSomething: Boolean = false,
)
