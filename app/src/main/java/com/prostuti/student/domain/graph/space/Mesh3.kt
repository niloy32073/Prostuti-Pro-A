package com.prostuti.student.domain.graph.space

/** A triangle list (3 vertices per triangle, not indexed) with one normal per vertex. */
class Mesh3(val positions: FloatArray, val normals: FloatArray) {
    val vertexCount: Int get() = positions.size / 3
    val isEmpty: Boolean get() = positions.isEmpty()

    companion object {
        val EMPTY = Mesh3(FloatArray(0), FloatArray(0))
    }
}
