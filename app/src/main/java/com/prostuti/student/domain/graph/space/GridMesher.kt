package com.prostuti.student.domain.graph.space

import kotlin.math.sqrt

/**
 * Meshes any surface given as P(u, v) on an (nu × nv) grid: explicit z = f(x, y), parametric
 * surfaces, the walls of integral solids and solids of revolution all go through here.
 *
 * Triangles touching an undefined point are dropped (holes stay holes), and so are triangles with
 * an edge longer than [maxEdge] — that is a jump across a pole (tan, 1/xy), not part of the surface.
 */
internal object GridMesher {

    fun mesh(
        point: ParamPoint,
        u0: Double, u1: Double, v0: Double, v1: Double,
        nu: Int, nv: Int,
        maxEdge: Double,
        normal: ParamPoint? = null,
        wire: LineBuilder? = null,
        wireEvery: Int = 0,
    ): Mesh3 {
        val cols = nu + 1
        val rows = nv + 1
        val px = DoubleArray(cols * rows)
        val py = DoubleArray(cols * rows)
        val pz = DoubleArray(cols * rows)
        val ok = BooleanArray(cols * rows)
        val tmp = DoubleArray(3)
        for (j in 0 until rows) {
            val v = v0 + (v1 - v0) * j / nv
            for (i in 0 until cols) {
                val u = u0 + (u1 - u0) * i / nu
                point.at(u, v, tmp)
                val k = j * cols + i
                px[k] = tmp[0]; py[k] = tmp[1]; pz[k] = tmp[2]
                ok[k] = tmp[0].isFinite() && tmp[1].isFinite() && tmp[2].isFinite()
            }
        }
        val nx = FloatArray(cols * rows)
        val ny = FloatArray(cols * rows)
        val nz = FloatArray(cols * rows)
        for (j in 0 until rows) {
            val v = v0 + (v1 - v0) * j / nv
            for (i in 0 until cols) {
                val k = j * cols + i
                if (!ok[k]) continue
                if (normal != null) {
                    normal.at(u0 + (u1 - u0) * i / nu, v, tmp)
                } else {
                    gridNormal(i, j, cols, rows, px, py, pz, ok, tmp)
                }
                var len = sqrt(tmp[0] * tmp[0] + tmp[1] * tmp[1] + tmp[2] * tmp[2])
                if (!(len > 1e-12) || !len.isFinite()) {
                    gridNormal(i, j, cols, rows, px, py, pz, ok, tmp)
                    len = sqrt(tmp[0] * tmp[0] + tmp[1] * tmp[1] + tmp[2] * tmp[2])
                }
                if (len > 1e-12 && len.isFinite()) {
                    nx[k] = (tmp[0] / len).toFloat(); ny[k] = (tmp[1] / len).toFloat(); nz[k] = (tmp[2] / len).toFloat()
                } else {
                    nz[k] = 1f
                }
            }
        }
        val out = MeshBuilder()
        val limit2 = maxEdge * maxEdge
        fun near(a: Int, b: Int): Boolean {
            val dx = px[a] - px[b]; val dy = py[a] - py[b]; val dz = pz[a] - pz[b]
            return dx * dx + dy * dy + dz * dz <= limit2
        }
        fun emit(k: Int) = out.vertex(px[k].toFloat(), py[k].toFloat(), pz[k].toFloat(), nx[k], ny[k], nz[k])
        for (j in 0 until nv) {
            for (i in 0 until nu) {
                val a = j * cols + i
                val b = a + 1
                val c = a + cols + 1
                val d = a + cols
                if (ok[a] && ok[b] && ok[c] && near(a, b) && near(b, c) && near(a, c)) { emit(a); emit(b); emit(c) }
                if (ok[a] && ok[c] && ok[d] && near(a, c) && near(c, d) && near(a, d)) { emit(a); emit(c); emit(d) }
            }
        }
        if (wire != null && wireEvery > 0) {
            fun seg(a: Int, b: Int) {
                if (ok[a] && ok[b] && near(a, b)) {
                    wire.segment(px[a].toFloat(), py[a].toFloat(), pz[a].toFloat(), px[b].toFloat(), py[b].toFloat(), pz[b].toFloat())
                }
            }
            for (j in 0 until rows) for (i in 0 until nu) if (j % wireEvery == 0 || j == nv) seg(j * cols + i, j * cols + i + 1)
            for (i in 0 until cols) for (j in 0 until nv) if (i % wireEvery == 0 || i == nu) seg(j * cols + i, (j + 1) * cols + i)
        }
        return out.build()
    }

    /** Normal from neighbouring grid points: (∂P/∂u) × (∂P/∂v) by central differences. */
    private fun gridNormal(
        i: Int, j: Int, cols: Int, rows: Int,
        px: DoubleArray, py: DoubleArray, pz: DoubleArray, ok: BooleanArray, out: DoubleArray,
    ) {
        fun idx(ii: Int, jj: Int) = jj * cols + ii
        val k = idx(i, j)
        val l = if (i > 0 && ok[idx(i - 1, j)]) idx(i - 1, j) else k
        val r = if (i < cols - 1 && ok[idx(i + 1, j)]) idx(i + 1, j) else k
        val dn = if (j > 0 && ok[idx(i, j - 1)]) idx(i, j - 1) else k
        val up = if (j < rows - 1 && ok[idx(i, j + 1)]) idx(i, j + 1) else k
        val ax = px[r] - px[l]; val ay = py[r] - py[l]; val az = pz[r] - pz[l]
        val bx = px[up] - px[dn]; val by = py[up] - py[dn]; val bz = pz[up] - pz[dn]
        out[0] = ay * bz - az * by
        out[1] = az * bx - ax * bz
        out[2] = ax * by - ay * bx
    }
}
