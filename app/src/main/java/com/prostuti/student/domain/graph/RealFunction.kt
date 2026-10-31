package com.prostuti.student.domain.graph

/** A real function of one variable; NaN means undefined at that point. */
fun interface RealFunction {
    fun value(x: Double): Double
}
