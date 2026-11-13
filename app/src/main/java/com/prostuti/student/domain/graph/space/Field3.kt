package com.prostuti.student.domain.graph.space

/** F(x, y, z) without boxing. */
fun interface Field3 {
    fun value(x: Double, y: Double, z: Double): Double
}
