package com.prostuti.student.domain.graph.plot

import com.prostuti.student.domain.graph.RealFunction

/**
 * Builds the shaded region of ∫_a^b (top − bottom) dx (bottom = the x-axis when absent): one
 * polygon per run where the integrand keeps its sign, split exactly at the crossing, so the part
 * above counts positive and the part below negative — what the integral's value means.
 */
internal object AreaBuilder {

    fun build(
        top: RealFunction,
        bottom: RealFunction?,
        a: Double,
        b: Double,
        viewport: PlotViewport,
        riemann: RiemannMode,
        riemannCount: Int,
    ): AreaShape {
        val lo = maxOf(a, viewport.xMin)
        val hi = minOf(b, viewport.xMax)
        val positive = ArrayList<FloatArray>()
        val negative = ArrayList<FloatArray>()
        if (hi > lo) {
            val steps = ((hi - lo) / viewport.unitsPerPxX).toInt().coerceIn(2, 6000)
            var run = Run()
            var prevX = Double.NaN
            var prevD = Double.NaN
            var prevB = 0.0
            for (i in 0..steps) {
                val x = lo + (hi - lo) * i / steps
                val t = top.value(x)
                val bt = bottom?.value(x) ?: 0.0
                val d = t - bt
                if (!d.isFinite()) {
                    run.close(positive, negative, viewport)
                    run = Run()
                    prevD = Double.NaN
                    continue
                }
                if (prevD.isFinite() && (prevD > 0) != (d > 0) && prevD != 0.0 && d != 0.0) {
                    // Linear crossing between the samples.
                    val s = prevD / (prevD - d)
                    val xc = prevX + (x - prevX) * s
                    val yc = prevB + (bt - prevB) * s
                    run.add(xc, yc, yc)
                    run.close(positive, negative, viewport)
                    run = Run()
                    run.add(xc, yc, yc)
                }
                run.add(x, t, bt)
                run.sign = if (d != 0.0) d > 0 else run.sign
                prevX = x; prevD = d; prevB = bt
            }
            run.close(positive, negative, viewport)
        }
        val guides = FloatList()
        for (x in listOf(a, b)) {
            if (x.isFinite() && x >= viewport.xMin && x <= viewport.xMax) {
                val t = top.value(x)
                val bt = bottom?.value(x) ?: 0.0
                if (t.isFinite() && bt.isFinite()) {
                    guides.add(viewport.px(x)); guides.add(viewport.py(t).clampY(viewport))
                    guides.add(viewport.px(x)); guides.add(viewport.py(bt).clampY(viewport))
                }
            }
        }
        val (polys, sum) = riemann(top, bottom, a, b, viewport, riemann, riemannCount)
        return AreaShape(positive, negative, guides.toArray(), polys, sum)
    }

    private fun Float.clampY(v: PlotViewport): Float = coerceIn(-v.heightPx * 2f, v.heightPx * 3f)

    /** A run of samples with one sign: polygon = top edge forward, bottom edge back. */
    private class Run {
        val xs = ArrayList<Double>()
        val tops = ArrayList<Double>()
        val bottoms = ArrayList<Double>()
        var sign = true

        fun add(x: Double, t: Double, b: Double) { xs += x; tops += t; bottoms += b }

        fun close(positive: MutableList<FloatArray>, negative: MutableList<FloatArray>, v: PlotViewport) {
            if (xs.size < 2) return
            val out = FloatArray(xs.size * 4)
            var k = 0
            for (i in xs.indices) { out[k++] = v.px(xs[i]); out[k++] = v.py(tops[i]).clampY(v) }
            for (i in xs.indices.reversed()) { out[k++] = v.px(xs[i]); out[k++] = v.py(bottoms[i]).clampY(v) }
            if (sign) positive += out else negative += out
        }
    }

    /** Left/right/midpoint rectangles or trapezoids, and their sum (exact, not just drawn). */
    fun riemann(
        top: RealFunction,
        bottom: RealFunction?,
        a: Double,
        b: Double,
        viewport: PlotViewport,
        mode: RiemannMode,
        n: Int,
    ): Pair<List<FloatArray>, Double?> {
        if (mode == RiemannMode.NONE || !a.isFinite() || !b.isFinite() || n < 1 || a == b) return emptyList<FloatArray>() to null
        val count = n.coerceAtMost(MAX_RIEMANN)
        val h = (b - a) / count
        fun d(x: Double) = top.value(x) - (bottom?.value(x) ?: 0.0)
        fun base(x: Double) = bottom?.value(x) ?: 0.0
        val polygons = ArrayList<FloatArray>(count)
        var sum = 0.0
        for (i in 0 until count) {
            val x0 = a + i * h
            val x1 = x0 + h
            if (mode == RiemannMode.TRAPEZOID) {
                val d0 = d(x0)
                val d1 = d(x1)
                sum += (d0 + d1) / 2 * h
                polygons += floatArrayOf(
                    viewport.px(x0), viewport.py(base(x0)).clampY(viewport),
                    viewport.px(x0), viewport.py(base(x0) + d0).clampY(viewport),
                    viewport.px(x1), viewport.py(base(x1) + d1).clampY(viewport),
                    viewport.px(x1), viewport.py(base(x1)).clampY(viewport),
                )
            } else {
                val s = when (mode) {
                    RiemannMode.LEFT -> x0
                    RiemannMode.RIGHT -> x1
                    else -> x0 + h / 2
                }
                val height = d(s)
                val floor = base(s)
                sum += height * h
                polygons += floatArrayOf(
                    viewport.px(x0), viewport.py(floor).clampY(viewport),
                    viewport.px(x0), viewport.py(floor + height).clampY(viewport),
                    viewport.px(x1), viewport.py(floor + height).clampY(viewport),
                    viewport.px(x1), viewport.py(floor).clampY(viewport),
                )
            }
        }
        return polygons to sum
    }

    const val MAX_RIEMANN = 1000
}
