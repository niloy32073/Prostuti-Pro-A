package com.prostuti.student.domain.graph.space

import com.prostuti.student.domain.graph.Axis3
import com.prostuti.student.domain.graph.CompiledFunction
import com.prostuti.student.domain.graph.CompiledLine
import com.prostuti.student.domain.graph.GraphPlot
import com.prostuti.student.domain.graph.plot.GridTicks
import com.prostuti.student.domain.graph.plot.LineOptions
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Turns one compiled 3D line into meshes for a box of half-size `range` (the view shows
 * −range…range on every axis). Pure and thread-safe; each call has its own environments.
 */
class SpacePlotter {

    fun plot(line: CompiledLine, options: LineOptions, range: Double): SpaceGeometry? {
        val id = line.id
        val maxEdge = 2 * range
        return when (val p = line.plot ?: return null) {
            is GraphPlot.Surface -> surface(id, p, options, range)
            is GraphPlot.ImplicitSurface -> {
                val f = field(p.field)
                val grad = p.gradient?.let { gradient(it) }
                val mesh = ImplicitMesher(f, grad, range, IMPLICIT_CELLS).mesh()
                SpaceGeometry(id, range, surfaces = listOf(mesh), heightMin = (-range).toFloat(), heightMax = range.toFloat())
            }
            is GraphPlot.ParametricSurface -> {
                val ex = p.x.newEnv(); val ey = p.y.newEnv(); val ez = p.z.newEnv()
                val point = ParamPoint { u, v, out ->
                    ex[0] = u; ex[1] = v; ey[0] = u; ey[1] = v; ez[0] = u; ez[1] = v
                    out[0] = p.x.eval(ex); out[1] = p.y.eval(ey); out[2] = p.z.eval(ez)
                }
                val wire = if (options.showWireframe) LineBuilder() else null
                val mesh = GridMesher.mesh(
                    point,
                    options.rangeMin ?: p.uRange.start, options.rangeMax ?: p.uRange.endInclusive,
                    options.vMin ?: p.vRange.start, options.vMax ?: p.vRange.endInclusive,
                    PARAM_U, PARAM_V, maxEdge, wire = wire, wireEvery = 8,
                )
                SpaceGeometry(id, range, surfaces = listOf(mesh), lines = wire?.build(), heightMin = (-range).toFloat(), heightMax = range.toFloat())
            }
            is GraphPlot.SpaceCurve -> {
                val ex = p.x.newEnv(); val ey = p.y.newEnv(); val ez = p.z.newEnv()
                val t0 = options.rangeMin ?: p.defaultRange.start
                val t1 = options.rangeMax ?: p.defaultRange.endInclusive
                val pieces = ArrayList<DoubleArray>()
                var current = ArrayList<Double>()
                var last: DoubleArray? = null
                for (i in 0..CURVE_SAMPLES) {
                    val t = t0 + (t1 - t0) * i / CURVE_SAMPLES
                    ex[0] = t; ey[0] = t; ez[0] = t
                    val q = doubleArrayOf(p.x.eval(ex), p.y.eval(ey), p.z.eval(ez))
                    val ok = q.all { it.isFinite() && abs(it) < range * 4 }
                    val jump = last != null && ok && (abs(q[0] - last[0]) + abs(q[1] - last[1]) + abs(q[2] - last[2])) > range
                    if (!ok || jump) {
                        if (current.size >= 6) pieces += current.toDoubleArray()
                        current = ArrayList()
                    }
                    if (ok) current.addAll(q.toList())
                    last = if (ok) q else null
                }
                if (current.size >= 6) pieces += current.toDoubleArray()
                val out = MeshBuilder()
                SolidShapes.tube(pieces, range * CURVE_RADIUS, out)
                SpaceGeometry(id, range, solidParts = listOf(out.build()))
            }
            is GraphPlot.Point3 -> {
                val c = doubleArrayOf(p.x, p.y, p.z)
                if (!c.all { it.isFinite() }) return null
                val out = MeshBuilder()
                SolidShapes.sphere(c, range * POINT_RADIUS, out)
                SpaceGeometry(id, range, solidParts = listOf(out.build()), labels = listOf(c))
            }
            is GraphPlot.Vector3 -> {
                val out = MeshBuilder()
                SolidShapes.arrow(p.from, p.to, range * CURVE_RADIUS, range * 0.09, out)
                SpaceGeometry(id, range, solidParts = listOf(out.build()), labels = listOf(p.to))
            }
            is GraphPlot.VolumeUnder -> volume(id, p, range)
            is GraphPlot.Revolution -> revolution(id, p, range)
            else -> null // 2D kinds
        }
    }

    private fun surface(id: Long, p: GraphPlot.Surface, options: LineOptions, range: Double): SpaceGeometry {
        val env = p.f.newEnv()
        val partialEnv = p.partials?.map { it.newEnv() }
        // Inputs (a, b) are the two other axes; the solved value goes on p.axis.
        fun place(a: Double, b: Double, v: Double, out: DoubleArray) = when (p.axis) {
            Axis3.Z -> { out[0] = a; out[1] = b; out[2] = v }
            Axis3.X -> { out[0] = v; out[1] = a; out[2] = b }
            Axis3.Y -> { out[0] = a; out[1] = v; out[2] = b }
        }
        val point = ParamPoint { a, b, out ->
            env[0] = a; env[1] = b
            place(a, b, p.f.eval(env), out)
        }
        val normal = if (p.partials != null && partialEnv != null) {
            ParamPoint { a, b, out ->
                partialEnv[0][0] = a; partialEnv[0][1] = b
                partialEnv[1][0] = a; partialEnv[1][1] = b
                val fa = p.partials[0].eval(partialEnv[0])
                val fb = p.partials[1].eval(partialEnv[1])
                // gradient of (solved − f(a, b)): −f_a, −f_b on the inputs, 1 on the solved axis
                place(-fa, -fb, 1.0, out)
            }
        } else {
            null
        }
        val wire = if (options.showWireframe) LineBuilder() else null
        val mesh = GridMesher.mesh(point, -range, range, -range, range, SURFACE_CELLS, SURFACE_CELLS, 2 * range, normal, wire, SURFACE_CELLS / 12)
        val lines = wire ?: LineBuilder()
        if (options.showContours) contours(p, range, lines)
        return SpaceGeometry(id, range, surfaces = listOf(mesh), lines = lines.build(), heightMin = (-range).toFloat(), heightMax = range.toFloat())
    }

    /** Level curves (solved value = c) by marching squares over the surface grid. */
    private fun contours(p: GraphPlot.Surface, range: Double, out: LineBuilder) {
        val n = SURFACE_CELLS
        val env = p.f.newEnv()
        val grid = Array(n + 1) { j ->
            DoubleArray(n + 1) { i ->
                env[0] = -range + 2 * range * i / n
                env[1] = -range + 2 * range * j / n
                p.f.eval(env)
            }
        }
        val step = GridTicks.step(2 * range / 10, 1.0).major
        var level = floor(-range / step) * step
        while (level <= range) {
            for (j in 0 until n) for (i in 0 until n) {
                val v = doubleArrayOf(grid[j][i], grid[j][i + 1], grid[j + 1][i + 1], grid[j + 1][i])
                if (v.any { !it.isFinite() }) continue
                val xs = doubleArrayOf(0.0, 1.0, 1.0, 0.0)
                val ys = doubleArrayOf(0.0, 0.0, 1.0, 1.0)
                val pts = ArrayList<DoubleArray>(4)
                for (e in 0 until 4) {
                    val a = v[e] - level
                    val b = v[(e + 1) % 4] - level
                    if ((a < 0) != (b < 0)) {
                        val t = a / (a - b)
                        val ga = -range + 2 * range * (i + xs[e] + (xs[(e + 1) % 4] - xs[e]) * t) / n
                        val gb = -range + 2 * range * (j + ys[e] + (ys[(e + 1) % 4] - ys[e]) * t) / n
                        val q = DoubleArray(3)
                        when (p.axis) {
                            Axis3.Z -> { q[0] = ga; q[1] = gb; q[2] = level }
                            Axis3.X -> { q[0] = level; q[1] = ga; q[2] = gb }
                            Axis3.Y -> { q[0] = ga; q[1] = level; q[2] = gb }
                        }
                        pts += q
                    }
                }
                if (pts.size >= 2) {
                    out.segment(pts[0][0].toFloat(), pts[0][1].toFloat(), pts[0][2].toFloat(), pts[1][0].toFloat(), pts[1][1].toFloat(), pts[1][2].toFloat())
                }
                if (pts.size == 4) {
                    out.segment(pts[2][0].toFloat(), pts[2][1].toFloat(), pts[2][2].toFloat(), pts[3][0].toFloat(), pts[3][1].toFloat(), pts[3][2].toFloat())
                }
            }
            level += step
        }
    }

    /** The solid under z = f(x, y) over the region: top, four walls and the floor. */
    private fun volume(id: Long, p: GraphPlot.VolumeUnder, range: Double): SpaceGeometry {
        val env = p.f.newEnv()
        val loEnv = p.innerMin.newEnv()
        val hiEnv = p.innerMax.newEnv()
        // (s, t) ∈ [0, 1]²: outer = mix(outerMin, outerMax, s), inner = mix(lo(outer), hi(outer), t)
        fun xy(s: Double, t: Double, out: DoubleArray) {
            val outer = p.outerMin + (p.outerMax - p.outerMin) * s
            loEnv[0] = outer; hiEnv[0] = outer
            val lo = p.innerMin.eval(loEnv)
            val hi = p.innerMax.eval(hiEnv)
            val inner = lo + (hi - lo) * t
            if (p.outerIsX) { out[0] = outer; out[1] = inner } else { out[0] = inner; out[1] = outer }
        }
        fun height(x: Double, y: Double): Double { env[0] = x; env[1] = y; return p.f.eval(env) }
        val maxEdge = 2 * range
        val top = GridMesher.mesh({ s, t, out -> xy(s, t, out); out[2] = height(out[0], out[1]) }, 0.0, 1.0, 0.0, 1.0, 64, 64, maxEdge)
        val floorMesh = GridMesher.mesh({ s, t, out -> xy(s, t, out); out[2] = 0.0 }, 0.0, 1.0, 0.0, 1.0, 16, 16, maxEdge)
        val walls = listOf(0.0, 1.0).flatMap { edge ->
            listOf(
                // walls along s at t = edge, and along t at s = edge; w goes from the floor to the top
                GridMesher.mesh({ s, w, out -> xy(s, edge, out); out[2] = w * height(out[0], out[1]) }, 0.0, 1.0, 0.0, 1.0, 64, 8, maxEdge),
                GridMesher.mesh({ t, w, out -> xy(edge, t, out); out[2] = w * height(out[0], out[1]) }, 0.0, 1.0, 0.0, 1.0, 64, 8, maxEdge),
            )
        }
        return SpaceGeometry(id, range, surfaces = listOf(top, floorMesh) + walls, heightMin = (-range).toFloat(), heightMax = range.toFloat())
    }

    /** Solid of revolution: outer (and inner) skins plus the end faces at x = a and x = b. */
    private fun revolution(id: Long, p: GraphPlot.Revolution, range: Double): SpaceGeometry {
        val fEnv = p.top.newEnv()
        val gEnv = p.bottom?.newEnv()
        fun f(x: Double): Double { fEnv[0] = x; return p.top.eval(fEnv) }
        fun g(x: Double): Double = if (p.bottom != null && gEnv != null) { gEnv[0] = x; p.bottom.eval(gEnv) } else 0.0
        val a = max(p.a, -range * 1.5)
        val b = min(p.b, range * 1.5)
        val maxEdge = 2 * range
        val parts = ArrayList<Mesh3>()
        if (p.axis == Axis3.X) {
            // (x, r cos φ, r sin φ) for r = f(x) and r = g(x)
            parts += GridMesher.mesh({ x, phi, out -> val r = f(x); out[0] = x; out[1] = r * cos(phi); out[2] = r * sin(phi) }, a, b, 0.0, 2 * PI, 96, 64, maxEdge)
            if (p.bottom != null) {
                parts += GridMesher.mesh({ x, phi, out -> val r = g(x); out[0] = x; out[1] = r * cos(phi); out[2] = r * sin(phi) }, a, b, 0.0, 2 * PI, 96, 64, maxEdge)
            }
            for (end in listOf(a, b)) {
                val outer = f(end)
                val inner = g(end)
                parts += GridMesher.mesh({ s, phi, out -> val r = inner + (outer - inner) * s; out[0] = end; out[1] = r * cos(phi); out[2] = r * sin(phi) }, 0.0, 1.0, 0.0, 2 * PI, 6, 64, maxEdge)
            }
        } else {
            // About the y-axis: (x cos φ, y, x sin φ); the region's top and bottom edges turn into caps,
            // its sides at x = a and x = b into cylinders.
            parts += GridMesher.mesh({ x, phi, out -> out[0] = x * cos(phi); out[1] = f(x); out[2] = x * sin(phi) }, a, b, 0.0, 2 * PI, 96, 64, maxEdge)
            parts += GridMesher.mesh({ x, phi, out -> out[0] = x * cos(phi); out[1] = g(x); out[2] = x * sin(phi) }, a, b, 0.0, 2 * PI, 96, 64, maxEdge)
            for (side in listOf(a, b)) {
                val lo = g(side)
                val hi = f(side)
                parts += GridMesher.mesh({ s, phi, out -> out[0] = side * cos(phi); out[1] = lo + (hi - lo) * s; out[2] = side * sin(phi) }, 0.0, 1.0, 0.0, 2 * PI, 8, 64, maxEdge)
            }
        }
        return SpaceGeometry(id, range, surfaces = parts, heightMin = (-range).toFloat(), heightMax = range.toFloat())
    }

    private fun field(c: CompiledFunction): Field3 {
        val env = c.newEnv()
        return Field3 { x, y, z ->
            env[0] = x; env[1] = y; env[2] = z
            val v = c.eval(env)
            if (v.isInfinite()) Double.NaN else v
        }
    }

    private fun gradient(parts: List<CompiledFunction>): (Double, Double, Double, DoubleArray) -> Unit {
        val envs = parts.map { it.newEnv() }
        return { x, y, z, out ->
            for (i in 0..2) {
                val e = envs[i]
                e[0] = x; e[1] = y; e[2] = z
                out[i] = parts[i].eval(e)
            }
        }
    }

    private companion object {
        const val SURFACE_CELLS = 120
        const val IMPLICIT_CELLS = 52
        const val PARAM_U = 96
        const val PARAM_V = 64
        const val CURVE_SAMPLES = 1200
        const val CURVE_RADIUS = 0.012
        const val POINT_RADIUS = 0.03
    }
}
