package com.prostuti.student.domain.code.flow

/** Measures one line of node text in the same units as [FlowMetrics]. */
fun interface TextWidth {
    fun of(text: String): Float
}
