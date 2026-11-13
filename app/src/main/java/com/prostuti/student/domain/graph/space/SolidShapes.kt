package com.prostuti.student.domain.graph.space

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Lit 3D shapes for things a GPU line can't show well: thin lines are 1 px on most phones, so
 * curves, axes and vectors are drawn as real tubes, points as small spheres.
 */
internal object SolidShapes {

    private const val SIDES = 10

    /** A tube of [radius] along each polyline (xyz triples); parallel-transport frames, so no twisting. */
    fun tube(polylines: List<DoubleArray>, radius: Double, out: MeshBuilder) {
        for (line in polylines) {
            val count = line.size / 3
            if (count < 2) continue
            var nPrev: DoubleArray? = null
            var ringPrev: Array<DoubleArray>? = null
            var normPrev: Array<DoubleArray>? = null
            for (k in 0 until count) {
                val t = tangent(line, k, count) ?: continue
                val nVec = nPrev?.let { reproject(it, t) } ?: anyPerpendicular(t)
                val b = cross(t, nVec)
                val ring = Array(SIDES) { DoubleArray(3) }
                val norms = Array(SIDES) { DoubleArray(3) }
                for (s in 0 until SIDES) {
                    val a = 2 * PI * s / SIDES
                    val c = cos(a)
                    val sn = sin(a)
                    for (d in 0..2) {
                        norms[s][d] = c * nVec[d] + sn * b[d]
                        ring[s][d] = line[3 * k + d] + radius * norms[s][d]
                    }
                }
                if (ringPrev != null && normPrev != null) {
                    for (s in 0 until SIDES) {
                        val s2 = (s + 1) % SIDES
                        quad(ringPrev[s], ringPrev[s2], ring[s2], ring[s], normPrev[s], normPrev[s2], norms[s2], norms[s], out)
                    }
                }
                nPrev = nVec
                ringPrev = ring
                normPrev = norms
            }
        }
    }

    /** An arrow from [from] to [to]: a shaft tube plus a cone head of length [head]. */
    fun arrow(from: DoubleArray, to: DoubleArray, radius: Double, head: Double, out: MeshBuilder) {
        val dir = sub(to, from)
        val len = norm(dir)
        if (len < 1e-12) { sphere(to, radius * 2, out); return }
        val u = scale(dir, 1 / len)
        val h = minOf(head, len * 0.4)
        val base = DoubleArray(3) { to[it] - u[it] * h }
        tube(listOf(from + base), radius, out)
        val nVec = anyPerpendicular(u)
        val b = cross(u, nVec)
        val r = radius * 2.6
        for (s in 0 until SIDES) {
            val a1 = 2 * PI * s / SIDES
            val a2 = 2 * PI * (s + 1) / SIDES
            val p1 = DoubleArray(3) { base[it] + r * (cos(a1) * nVec[it] + sin(a1) * b[it]) }
            val p2 = DoubleArray(3) { base[it] + r * (cos(a2) * nVec[it] + sin(a2) * b[it]) }
            // side: normals lean forward by the cone's slope
            val n1 = DoubleArray(3) { (cos(a1) * nVec[it] + sin(a1) * b[it]) * h + u[it] * r }
            val n2 = DoubleArray(3) { (cos(a2) * nVec[it] + sin(a2) * b[it]) * h + u[it] * r }
            val nt = DoubleArray(3) { n1[it] + n2[it] }
            tri(p1, p2, to, n1, n2, nt, out)
            // base disc
            val back = scale(u, -1.0)
            tri(base, p2, p1, back, back, back, out)
        }
    }

    /** A sphere of [radius] at [center] (subdivided octahedron, 512 triangles). */
    fun sphere(center: DoubleArray, radius: Double, out: MeshBuilder) {
        for (f in UNIT_SPHERE) {
            for (v in f) out.vertex(
                (center[0] + radius * v[0]).toFloat(), (center[1] + radius * v[1]).toFloat(), (center[2] + radius * v[2]).toFloat(),
                v[0].toFloat(), v[1].toFloat(), v[2].toFloat(),
            )
        }
    }

    private val UNIT_SPHERE: List<Array<DoubleArray>> by lazy {
        var faces = listOf(
            arrayOf(d(1.0, 0.0, 0.0), d(0.0, 1.0, 0.0), d(0.0, 0.0, 1.0)), arrayOf(d(0.0, 1.0, 0.0), d(-1.0, 0.0, 0.0), d(0.0, 0.0, 1.0)),
            arrayOf(d(-1.0, 0.0, 0.0), d(0.0, -1.0, 0.0), d(0.0, 0.0, 1.0)), arrayOf(d(0.0, -1.0, 0.0), d(1.0, 0.0, 0.0), d(0.0, 0.0, 1.0)),
            arrayOf(d(0.0, 1.0, 0.0), d(1.0, 0.0, 0.0), d(0.0, 0.0, -1.0)), arrayOf(d(-1.0, 0.0, 0.0), d(0.0, 1.0, 0.0), d(0.0, 0.0, -1.0)),
            arrayOf(d(0.0, -1.0, 0.0), d(-1.0, 0.0, 0.0), d(0.0, 0.0, -1.0)), arrayOf(d(1.0, 0.0, 0.0), d(0.0, -1.0, 0.0), d(0.0, 0.0, -1.0)),
        )
        repeat(3) {
            faces = faces.flatMap { (a, b, c) ->
                val ab = unit(mid(a, b)); val bc = unit(mid(b, c)); val ca = unit(mid(c, a))
                listOf(arrayOf(a, ab, ca), arrayOf(ab, b, bc), arrayOf(ca, bc, c), arrayOf(ab, bc, ca))
            }
        }
        faces
    }

    // ── small vector helpers ───────────────────────────────────────────────────────────────

    private fun d(x: Double, y: Double, z: Double) = doubleArrayOf(x, y, z)
    private fun mid(a: DoubleArray, b: DoubleArray) = DoubleArray(3) { (a[it] + b[it]) / 2 }
    private fun unit(a: DoubleArray) = scale(a, 1 / norm(a))
    private fun sub(a: DoubleArray, b: DoubleArray) = DoubleArray(3) { a[it] - b[it] }
    private fun scale(a: DoubleArray, k: Double) = DoubleArray(3) { a[it] * k }
    private fun norm(a: DoubleArray) = sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2])
    private fun dot(a: DoubleArray, b: DoubleArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
    private fun cross(a: DoubleArray, b: DoubleArray) =
        doubleArrayOf(a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])

    private fun tangent(line: DoubleArray, k: Int, count: Int): DoubleArray? {
        val a = if (k > 0) k - 1 else k
        val b = if (k < count - 1) k + 1 else k
        val t = DoubleArray(3) { line[3 * b + it] - line[3 * a + it] }
        val len = norm(t)
        return if (len > 1e-12) scale(t, 1 / len) else null
    }

    private fun anyPerpendicular(t: DoubleArray): DoubleArray {
        val helper = if (abs(t[2]) < 0.9) d(0.0, 0.0, 1.0) else d(1.0, 0.0, 0.0)
        return unit(cross(cross(t, helper), t))
    }

    private fun reproject(n: DoubleArray, t: DoubleArray): DoubleArray {
        val k = dot(n, t)
        val r = DoubleArray(3) { n[it] - k * t[it] }
        val len = norm(r)
        return if (len > 1e-9) scale(r, 1 / len) else anyPerpendicular(t)
    }

    private fun tri(a: DoubleArray, b: DoubleArray, c: DoubleArray, na: DoubleArray, nb: DoubleArray, nc: DoubleArray, out: MeshBuilder) {
        for ((p, n) in listOf(a to na, b to nb, c to nc)) {
            val len = norm(n).takeIf { it > 0 } ?: 1.0
            out.vertex(p[0].toFloat(), p[1].toFloat(), p[2].toFloat(), (n[0] / len).toFloat(), (n[1] / len).toFloat(), (n[2] / len).toFloat())
        }
    }

    private fun quad(
        a: DoubleArray, b: DoubleArray, c: DoubleArray, e: DoubleArray,
        na: DoubleArray, nb: DoubleArray, nc: DoubleArray, ne: DoubleArray,
        out: MeshBuilder,
    ) {
        tri(a, b, c, na, nb, nc, out)
        tri(a, c, e, na, nc, ne, out)
    }
}
