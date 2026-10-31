package com.prostuti.student.domain.graph.plot

/** Collects pixel points into separate polylines; [breakLine] starts a new one (gap, asymptote). */
internal class CurveBuilder {
    private var buffer = FloatArray(512)
    private var size = 0
    private val lines = ArrayList<FloatArray>()
    var pointCount = 0
        private set

    fun add(x: Float, y: Float) {
        if (size + 2 > buffer.size) buffer = buffer.copyOf(buffer.size * 2)
        buffer[size++] = x
        buffer[size++] = y
        pointCount++
    }

    fun breakLine() {
        if (size >= 4) lines += buffer.copyOf(size)
        size = 0
    }

    val hasOpenLine: Boolean get() = size > 0

    fun finish(): List<FloatArray> {
        breakLine()
        return lines
    }
}
