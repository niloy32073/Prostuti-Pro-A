package com.prostuti.student.domain.code.flow

/**
 * A line of right-angle segments through [points]. [arrow] draws an arrowhead at the last point.
 * A decision's outgoing line carries a [label] drawn at [labelAt] (its top-left corner, or its
 * top-right corner when [labelAlignEnd]).
 */
data class FlowEdge(
    val points: List<FlowPoint>,
    val arrow: Boolean,
    val label: EdgeLabel? = null,
    val labelAt: FlowPoint? = null,
    val labelAlignEnd: Boolean = false,
)
