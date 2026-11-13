package com.prostuti.student.domain.graph.space

internal class LineBuilder {
    private var data = FloatArray(6 * 256)
    private var size = 0

    fun segment(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float) {
        if (size + 6 > data.size) data = data.copyOf(data.size * 2)
        data[size++] = x1; data[size++] = y1; data[size++] = z1
        data[size++] = x2; data[size++] = y2; data[size++] = z2
    }

    val isEmpty: Boolean get() = size == 0

    fun build(): Lines3? = if (size == 0) null else Lines3(data.copyOf(size))
}
