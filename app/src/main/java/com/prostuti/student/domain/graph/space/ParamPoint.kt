package com.prostuti.student.domain.graph.space

/** Writes the 3D point for parameters (u, v) into out[0..2]; NaN when undefined. */
fun interface ParamPoint {
    fun at(u: Double, v: Double, out: DoubleArray)
}
