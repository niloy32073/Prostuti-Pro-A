package com.prostuti.student.domain.graph.space

/** Line segments as (x1, y1, z1, x2, y2, z2)…, drawn thin on top of surfaces (contours, mesh lines). */
class Lines3(val positions: FloatArray) {
    val isEmpty: Boolean get() = positions.isEmpty()
}
