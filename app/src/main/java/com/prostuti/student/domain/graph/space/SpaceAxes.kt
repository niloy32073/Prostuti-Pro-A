package com.prostuti.student.domain.graph.space

import com.prostuti.student.domain.graph.plot.GridTicks

/** The fixed scenery of the 3D view for a box of half-size [range]: axes, box edges, floor grid. */
class SpaceAxes(
    val range: Double,
    /** One arrow per axis, in x, y, z order. */
    val axes: List<Mesh3>,
    val box: Lines3,
    val grid: Lines3,
    /** Tick spacing used for the grid and the axis numbers. */
    val tickStep: Double,
) {
    companion object {
        fun build(range: Double): SpaceAxes {
            val r = range
            val axes = (0..2).map { axis ->
                val out = MeshBuilder()
                val from = DoubleArray(3).also { it[axis] = -r }
                val to = DoubleArray(3).also { it[axis] = r * 1.12 }
                SolidShapes.arrow(from, to, r * 0.005, r * 0.07, out)
                out.build()
            }
            val box = LineBuilder()
            val corners = listOf(-r, r)
            for (a in corners) for (b in corners) {
                box.segment((-r).toFloat(), a.toFloat(), b.toFloat(), r.toFloat(), a.toFloat(), b.toFloat())
                box.segment(a.toFloat(), (-r).toFloat(), b.toFloat(), a.toFloat(), r.toFloat(), b.toFloat())
                box.segment(a.toFloat(), b.toFloat(), (-r).toFloat(), a.toFloat(), b.toFloat(), r.toFloat())
            }
            val step = GridTicks.step(2 * r / 8, 1.0).major
            val grid = LineBuilder()
            for (v in GridTicks.ticks(-r, r, step)) {
                grid.segment(v.toFloat(), (-r).toFloat(), 0f, v.toFloat(), r.toFloat(), 0f)
                grid.segment((-r).toFloat(), v.toFloat(), 0f, r.toFloat(), v.toFloat(), 0f)
            }
            return SpaceAxes(r, axes, box.build()!!, grid.build() ?: Lines3(FloatArray(0)), step)
        }
    }
}
