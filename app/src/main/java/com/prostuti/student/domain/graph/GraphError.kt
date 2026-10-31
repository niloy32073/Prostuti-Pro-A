package com.prostuti.student.domain.graph

/** A line's error: what went wrong, where in the text, and the names involved (e.g. missing variables). */
data class GraphError(
    val kind: GraphErrorKind,
    val position: Int = -1,
    val names: List<String> = emptyList(),
)
