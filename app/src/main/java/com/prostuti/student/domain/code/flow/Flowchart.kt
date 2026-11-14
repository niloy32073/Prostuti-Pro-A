package com.prostuti.student.domain.code.flow

/** One laid-out chart; every coordinate lies inside 0..[width] × 0..[height]. */
data class Flowchart(val nodes: List<FlowNode>, val edges: List<FlowEdge>, val width: Float, val height: Float)
