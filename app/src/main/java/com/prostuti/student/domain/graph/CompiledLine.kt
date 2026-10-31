package com.prostuti.student.domain.graph

/** The compiler's verdict on one expression-list line. */
class CompiledLine(
    val id: Long,
    val kind: GraphLineKind,
    /** Typeset form of the input, or null when it does not parse. */
    val latex: String?,
    val error: GraphError? = null,
    val plot: GraphPlot? = null,
    /** Value of a constant line (`2+3`, `∫_0^1 x² dx`, `b = 2a`). */
    val value: Double? = null,
    val parameter: ParameterInfo? = null,
)
