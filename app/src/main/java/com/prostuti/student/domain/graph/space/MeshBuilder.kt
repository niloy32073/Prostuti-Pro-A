package com.prostuti.student.domain.graph.space

/** Growable buffers for a triangle list. */
internal class MeshBuilder {
    private var pos = FloatArray(3 * 3 * 1024)
    private var nor = FloatArray(3 * 3 * 1024)
    private var size = 0

    fun vertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float) {
        if (size + 3 > pos.size) {
            pos = pos.copyOf(pos.size * 2)
            nor = nor.copyOf(nor.size * 2)
        }
        pos[size] = x; pos[size + 1] = y; pos[size + 2] = z
        nor[size] = nx; nor[size + 1] = ny; nor[size + 2] = nz
        size += 3
    }

    fun build(): Mesh3 = if (size == 0) Mesh3.EMPTY else Mesh3(pos.copyOf(size), nor.copyOf(size))
}
