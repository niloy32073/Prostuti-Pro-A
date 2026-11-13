package com.prostuti.student.domain.graph.space

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * F(x, y, z) = 0 by marching tetrahedra: every grid cube is cut into six tetrahedra (Kuhn split,
 * which matches up between neighbours), so the surface has no cracks or ambiguous cases.
 * Each vertex is solved on the real function along its edge (a few Illinois steps) instead of
 * straight-line guessing, and an edge whose |F| grows towards the "root" is a pole and is dropped.
 */
internal class ImplicitMesher(
    private val field: Field3,
    /** Exact gradient if known; otherwise central differences. */
    private val gradient: ((Double, Double, Double, DoubleArray) -> Unit)?,
    private val range: Double,
    private val n: Int,
) {
    private val points = n + 1
    private val lo = -range * 1.02
    private val step = (2 * range * 1.02) / n

    private fun coord(i: Int) = lo + i * step
    private fun index(i: Int, j: Int, k: Int) = i + points * (j + points * k)

    private val values = DoubleArray(points * points * points)

    // Edge vertices shared between tetrahedra: key = pair of grid indices.
    private val edgeIndex = HashMap<Long, Int>()
    private var vx = FloatArray(4096)
    private var vy = FloatArray(4096)
    private var vz = FloatArray(4096)
    private var nxs = FloatArray(4096)
    private var nys = FloatArray(4096)
    private var nzs = FloatArray(4096)
    private var vertexCount = 0

    fun mesh(): Mesh3 {
        for (k in 0 until points) {
            val z = coord(k)
            for (j in 0 until points) {
                val y = coord(j)
                for (i in 0 until points) values[index(i, j, k)] = field.value(coord(i), y, z)
            }
        }
        val out = MeshBuilder()
        val corner = IntArray(8)
        for (k in 0 until n) for (j in 0 until n) for (i in 0 until n) {
            for (c in 0 until 8) corner[c] = index(i + (c and 1), j + ((c shr 1) and 1), k + ((c shr 2) and 1))
            var neg = 0
            var pos = 0
            for (c in corner) {
                val v = values[c]
                if (v < 0) neg++ else if (v > 0) pos++ else { neg++; pos++ }
            }
            if (neg == 0 || pos == 0) continue
            for (tet in TETS) tetrahedron(corner[tet[0]], corner[tet[1]], corner[tet[2]], corner[tet[3]], out)
        }
        return out.build()
    }

    private fun tetrahedron(a: Int, b: Int, c: Int, d: Int, out: MeshBuilder) {
        val ids = intArrayOf(a, b, c, d)
        if (ids.any { values[it].isNaN() }) return
        val inside = ids.filter { values[it] < 0 }
        val outside = ids.filter { values[it] >= 0 }
        when (inside.size) {
            1, 3 -> {
                val lone = if (inside.size == 1) inside[0] else outside[0]
                val others = ids.filter { it != lone }
                triangle(edge(lone, others[0]), edge(lone, others[1]), edge(lone, others[2]), out)
            }
            2 -> {
                val (p, q) = inside
                val (r, s) = outside
                val e1 = edge(p, r)
                val e2 = edge(p, s)
                val e3 = edge(q, s)
                val e4 = edge(q, r)
                triangle(e1, e2, e3, out)
                triangle(e1, e3, e4, out)
            }
        }
    }

    private fun triangle(a: Int, b: Int, c: Int, out: MeshBuilder) {
        if (a < 0 || b < 0 || c < 0) return
        for (v in intArrayOf(a, b, c)) out.vertex(vx[v], vy[v], vz[v], nxs[v], nys[v], nzs[v])
    }

    /** The surface point on the edge between grid points [p] and [q], or -1 for a pole. */
    private fun edge(p: Int, q: Int): Int {
        val key = if (p < q) p.toLong() * values.size + q else q.toLong() * values.size + p
        edgeIndex[key]?.let { return it }
        val result = solveEdge(p, q)
        edgeIndex[key] = result
        return result
    }

    private fun solveEdge(p: Int, q: Int): Int {
        val ax = coord(p % points); val ay = coord((p / points) % points); val az = coord(p / (points * points))
        val bx = coord(q % points); val by = coord((q / points) % points); val bz = coord(q / (points * points))
        val fa0 = values[p]
        val fb0 = values[q]
        var t: Double
        if (fa0 == 0.0) {
            t = 0.0
        } else {
            var lo = 0.0; var hi = 1.0; var fa = fa0; var fb = fb0; var side = 0
            t = fa / (fa - fb)
            repeat(REFINE_STEPS) {
                val ft = field.value(ax + (bx - ax) * t, ay + (by - ay) * t, az + (bz - az) * t)
                if (ft.isNaN()) return -1
                if (ft == 0.0) return@repeat
                if ((ft < 0) == (fa < 0)) {
                    lo = t; fa = ft
                    if (side == 1) fb /= 2
                    side = 1
                } else {
                    hi = t; fb = ft
                    if (side == -1) fa /= 2
                    side = -1
                }
                t = (lo * fb - hi * fa) / (fb - fa)
                if (!t.isFinite() || t <= lo || t >= hi) t = 0.5 * (lo + hi)
            }
        }
        val x = ax + (bx - ax) * t
        val y = ay + (by - ay) * t
        val z = az + (bz - az) * t
        val at = abs(field.value(x, y, z))
        if (!at.isFinite() || at > minOf(abs(fa0), abs(fb0))) return -1 // a pole, not a root
        val g = DoubleArray(3)
        if (gradient != null) {
            gradient.invoke(x, y, z, g)
        }
        if (gradient == null || !(g[0].isFinite() && g[1].isFinite() && g[2].isFinite()) || g[0] == 0.0 && g[1] == 0.0 && g[2] == 0.0) {
            val h = step * 0.25
            g[0] = field.value(x + h, y, z) - field.value(x - h, y, z)
            g[1] = field.value(x, y + h, z) - field.value(x, y - h, z)
            g[2] = field.value(x, y, z + h) - field.value(x, y, z - h)
        }
        val len = sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2])
        ensure()
        val v = vertexCount++
        vx[v] = x.toFloat(); vy[v] = y.toFloat(); vz[v] = z.toFloat()
        if (len > 1e-300 && len.isFinite()) {
            nxs[v] = (g[0] / len).toFloat(); nys[v] = (g[1] / len).toFloat(); nzs[v] = (g[2] / len).toFloat()
        } else {
            nzs[v] = 1f
        }
        return v
    }

    private fun ensure() {
        if (vertexCount < vx.size) return
        val size = vx.size * 2
        vx = vx.copyOf(size); vy = vy.copyOf(size); vz = vz.copyOf(size)
        nxs = nxs.copyOf(size); nys = nys.copyOf(size); nzs = nzs.copyOf(size)
    }

    private companion object {
        const val REFINE_STEPS = 6

        /** Corner c of a cube is (c & 1, c >> 1 & 1, c >> 2 & 1); six tetrahedra around the 0–7 diagonal. */
        val TETS = arrayOf(
            intArrayOf(0, 1, 3, 7), intArrayOf(0, 3, 2, 7), intArrayOf(0, 2, 6, 7),
            intArrayOf(0, 6, 4, 7), intArrayOf(0, 4, 5, 7), intArrayOf(0, 5, 1, 7),
        )
    }
}
