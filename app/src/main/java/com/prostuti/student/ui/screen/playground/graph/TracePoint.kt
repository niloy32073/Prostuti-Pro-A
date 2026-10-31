package com.prostuti.student.ui.screen.playground.graph

/** A point the student picked on a curve (tap or drag); [slope] for y = f(x) curves. */
data class TracePoint(val lineId: Long, val x: Double, val y: Double, val slope: Double? = null)
