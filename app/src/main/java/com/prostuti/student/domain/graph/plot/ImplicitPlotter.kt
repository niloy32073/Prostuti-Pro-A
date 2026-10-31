package com.prostuti.student.domain.graph.plot

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Quadtree marching squares for F(x, y) = 0 and F(x, y) < 0.
 *
 * A coarse grid (one cell per [BASE_CELL_PX] px) is evaluated once; cells the curve crosses are
 * split down to ~3 px, and each crossing is solved on the real function, so circles, hyperbolas
 * and r = cos 3θ come out smooth. A sign change across a pole (1/x − y at x = 0) is rejected,
 * because there |F| grows instead of shrinking to zero.
 */
internal class ImplicitPlotter(
    private val fieldFn: Field2,
    private val viewport: PlotViewport,
    private val fill: Boolean,
    private val strict: Boolean,
) {
    private fun field(x: Double, y: Double): Double = fieldFn.value(x, y)

    private val segments = FloatList()
    private val rects = FloatList()
    private val polygons = ArrayList<FloatArray>()

    private fun inside(v: Double): Boolean = if (strict) v < 0 else v <= 0

    fun plot(): ImplicitResult {
        val cols = ceil(viewport.widthPx / BASE_CELL_PX).toInt().coerceAtLeast(1)
        val rows = ceil(viewport.heightPx / BASE_CELL_PX).toInt().coerceAtLeast(1)
        val cellW = (viewport.xMax - viewport.xMin) / cols
        val cellH = (viewport.yMax - viewport.yMin) / rows
        val values = Array(rows + 1) { r ->
            val y = viewport.yMax - r * cellH
            DoubleArray(cols + 1) { c -> field(viewport.xMin + c * cellW, y) }
        }
        val fullInside = Array(rows) { BooleanArray(cols) }
        for (r in 0 until rows) {
            val yTop = viewport.yMax - r * cellH
            val yBottom = yTop - cellH
            for (c in 0 until cols) {
                val xLeft = viewport.xMin + c * cellW
                val full = cell(
                    xLeft, yBottom, xLeft + cellW, yTop,
                    values[r + 1][c], values[r + 1][c + 1], values[r][c + 1], values[r][c],
                    depth = 0,
                    emitFullRect = false,
                )
                fullInside[r][c] = full
            }
        }
        if (fill) mergeFullCells(fullInside, cols, rows)
        return ImplicitResult(segments.toArray(), rects.toArray(), polygons)
    }

    /** Merges runs of fully-inside coarse cells per row into one rectangle each. */
    private fun mergeFullCells(full: Array<BooleanArray>, cols: Int, rows: Int) {
        val cw = viewport.widthPx.toFloat() / cols
        val ch = viewport.heightPx.toFloat() / rows
        for (r in 0 until rows) {
            var c = 0
            while (c < cols) {
                if (!full[r][c]) { c++; continue }
                val start = c
                while (c < cols && full[r][c]) c++
                rects.add(start * cw); rects.add(r * ch); rects.add(c * cw); rects.add((r + 1) * ch)
            }
        }
    }

    /**
     * Corners: v00 (x0,y0) bottom-left, v10 (x1,y0) bottom-right, v11 (x1,y1) top-right, v01 (x0,y1) top-left.
     * Returns true when the whole cell is inside and [emitFullRect] is false (the caller merges it).
     */
    private fun cell(
        x0: Double, y0: Double, x1: Double, y1: Double,
        v00: Double, v10: Double, v11: Double, v01: Double,
        depth: Int,
        emitFullRect: Boolean,
    ): Boolean {
        val xm = 0.5 * (x0 + x1)
        val ym = 0.5 * (y0 + y1)
        val vc = field(xm, ym)
        val corners = doubleArrayOf(v00, v10, v11, v01)
        val anyNaN = corners.any { it.isNaN() } || vc.isNaN()
        val pos = corners.count { it > 0 } + (if (vc > 0) 1 else 0)
        val neg = corners.count { it < 0 } + (if (vc < 0) 1 else 0)
        val mixed = (pos > 0 && neg > 0) || corners.any { it == 0.0 }
        if (!mixed && !anyNaN) {
            val allInside = inside(v00) && inside(v10) && inside(v11) && inside(v01) && inside(vc)
            // A thin feature can hide between corners of the same sign: look closer near zero.
            val maxAbs = max(max(abs(v00), abs(v10)), max(abs(v11), abs(v01)))
            if (depth < 1 && abs(vc) < 0.15 * maxAbs) {
                split(x0, y0, x1, y1, xm, ym, v00, v10, v11, v01, vc, depth)
                return false
            }
            if (fill && allInside) {
                if (emitFullRect) rect(x0, y0, x1, y1) else return true
            }
            return false
        }
        if (anyNaN && corners.all { it.isNaN() } && vc.isNaN()) return false
        if (depth < MAX_DEPTH) {
            split(x0, y0, x1, y1, xm, ym, v00, v10, v11, v01, vc, depth)
            return false
        }
        leaf(x0, y0, x1, y1, v00, v10, v11, v01, vc)
        return false
    }

    /** Splits into four children; a fully-inside child emits its own rect. */
    private fun split(
        x0: Double, y0: Double, x1: Double, y1: Double, xm: Double, ym: Double,
        v00: Double, v10: Double, v11: Double, v01: Double, vc: Double,
        depth: Int,
    ) {
        val vb = field(xm, y0)
        val vr = field(x1, ym)
        val vt = field(xm, y1)
        val vl = field(x0, ym)
        val d = depth + 1
        cell(x0, y0, xm, ym, v00, vb, vc, vl, d, emitFullRect = true)
        cell(xm, y0, x1, ym, vb, v10, vr, vc, d, emitFullRect = true)
        cell(xm, ym, x1, y1, vc, vr, v11, vt, d, emitFullRect = true)
        cell(x0, ym, xm, y1, vl, vc, vt, v01, d, emitFullRect = true)
    }

    private fun rect(x0: Double, y0: Double, x1: Double, y1: Double) {
        rects.add(viewport.px(x0)); rects.add(viewport.py(y1)); rects.add(viewport.px(x1)); rects.add(viewport.py(y0))
    }

    private fun leaf(x0: Double, y0: Double, x1: Double, y1: Double, v00: Double, v10: Double, v11: Double, v01: Double, vc: Double) {
        val xs = doubleArrayOf(x0, x1, x1, x0)
        val ys = doubleArrayOf(y0, y0, y1, y1)
        val vs = doubleArrayOf(v00, v10, v11, v01)
        if (vs.any { it.isNaN() }) {
            // Domain edge inside a pixel-sized cell: shade the defined inside corners only.
            if (fill) leafPolygon(xs, ys, vs.map { if (it.isNaN()) 1.0 else it }.toDoubleArray())
            return
        }
        // Edge crossings e0: 0→1, e1: 1→2, e2: 2→3, e3: 3→0, each pinned down on the real function.
        val cx = DoubleArray(4)
        val cy = DoubleArray(4)
        val has = BooleanArray(4)
        val valid = BooleanArray(4)
        for (e in 0 until 4) {
            val a = e
            val b = (e + 1) % 4
            if ((vs[a] > 0) != (vs[b] > 0) || vs[a] == 0.0) {
                has[e] = true
                valid[e] = refine(xs[a], ys[a], vs[a], xs[b], ys[b], vs[b], cx, cy, e)
            }
        }
        val crossings = has.count { it }
        fun seg(e1: Int, e2: Int) {
            if (valid[e1] && valid[e2]) segment(cx[e1], cy[e1], cx[e2], cy[e2])
        }
        if (crossings == 2) {
            val (e1, e2) = has.indices.filter { has[it] }
            seg(e1, e2)
        } else if (crossings == 4) {
            if ((vc > 0) == (v00 > 0)) { seg(0, 1); seg(2, 3) } else { seg(3, 0); seg(1, 2) }
        }
        if (fill) leafPolygon(xs, ys, vs)
    }

    private fun segment(ax: Double, ay: Double, bx: Double, by: Double) {
        segments.add(viewport.px(ax)); segments.add(viewport.py(ay))
        segments.add(viewport.px(bx)); segments.add(viewport.py(by))
    }

    /**
     * Bisects the sign change on edge a→b and stores the point in (cx[e], cy[e]). Returns false for
     * a pole: near a real root |F| shrinks to ~0, near 1/x it grows past the end-point values.
     */
    private fun refine(
        ax: Double, ay: Double, av: Double, bx: Double, by: Double, bv: Double,
        cx: DoubleArray, cy: DoubleArray, e: Int,
    ): Boolean {
        if (av == 0.0) { cx[e] = ax; cy[e] = ay; return true }
        // Illinois (modified regula falsi): converges in a handful of steps on smooth fields.
        var a = 0.0
        var b = 1.0
        var fa = av
        var fb = bv
        var side = 0
        var t = 0.5
        for (step in 0 until REFINE_STEPS) {
            t = (a * fb - b * fa) / (fb - fa)
            if (!t.isFinite() || t <= a || t >= b) t = 0.5 * (a + b)
            val ft = field(ax + (bx - ax) * t, ay + (by - ay) * t)
            if (ft.isNaN()) return false
            if (ft == 0.0) break
            if ((ft > 0) == (fb > 0)) {
                b = t; fb = ft
                if (side == -1) fa /= 2
                side = -1
            } else {
                a = t; fa = ft
                if (side == 1) fb /= 2
                side = 1
            }
            if (b - a < 1e-6) break
        }
        cx[e] = ax + (bx - ax) * t
        cy[e] = ay + (by - ay) * t
        val atRoot = abs(field(cx[e], cy[e]))
        return atRoot.isFinite() && atRoot <= minOf(abs(av), abs(bv))
    }

    /** The inside part of a leaf cell: walk the corners, keeping inside corners and crossing points. */
    private fun leafPolygon(xs: DoubleArray, ys: DoubleArray, vs: DoubleArray) {
        val out = FloatList()
        for (i in 0 until 4) {
            val j = (i + 1) % 4
            val inI = inside(vs[i])
            if (inI) { out.add(viewport.px(xs[i])); out.add(viewport.py(ys[i])) }
            if (inI != inside(vs[j])) {
                val t = if (vs[i] == vs[j]) 0.5 else vs[i] / (vs[i] - vs[j])
                out.add(viewport.px(xs[i] + (xs[j] - xs[i]) * t))
                out.add(viewport.py(ys[i] + (ys[j] - ys[i]) * t))
            }
        }
        if (out.size >= 6) polygons += out.toArray()
    }

    private companion object {
        const val BASE_CELL_PX = 12.0
        const val MAX_DEPTH = 2
        const val REFINE_STEPS = 10
    }
}
