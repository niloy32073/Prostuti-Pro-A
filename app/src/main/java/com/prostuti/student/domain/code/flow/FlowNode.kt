package com.prostuti.student.domain.code.flow

/**
 * A placed symbol. [x] is its centre, [y] its top. [lines] is the wrapped text, [sourceLine] the
 * 1-based code line it comes from, [callee] the function a [NodeShape.CALL] opens.
 */
data class FlowNode(
    val id: Int,
    val shape: NodeShape,
    val lines: List<String>,
    val sourceLine: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val callee: String? = null,
)
