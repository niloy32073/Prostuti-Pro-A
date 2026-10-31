package com.prostuti.student.domain.graph.plot

/** F(x, y) without boxing. */
fun interface Field2 {
    fun value(x: Double, y: Double): Double
}
