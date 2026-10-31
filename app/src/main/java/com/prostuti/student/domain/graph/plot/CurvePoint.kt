package com.prostuti.student.domain.graph.plot

/** Writes the pixel position of parameter [t] into out[0], out[1] (NaN when undefined). */
fun interface CurvePoint {
    fun at(t: Double, out: FloatArray)
}
