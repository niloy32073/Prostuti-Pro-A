package com.prostuti.student.domain.graph.plot

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Adaptive sampler for parametric (x(t), y(t)) and polar curves. Refines until neighbouring points
 * are under 2 px apart and the midpoint sits on the chord; breaks the line on undefined parts and
 * on jumps that do not shrink with refinement.
 */
internal class CurveSampler(
    private val viewport: PlotViewport,
    private val point: CurvePoint,
) {
    private val builder = CurveBuilder()
    private val w = viewport.widthPx.toFloat()
    private val h = viewport.heightPx.toFloat()
    private val tmp = FloatArray(2)

    fun sample(tMin: Double, tMax: Double, baseSamples: Int = 600): List<FloatArray> {
        if (!(tMax > tMin) || !tMin.isFinite() || !tMax.isFinite()) return emptyList()
        var t0 = tMin
        var p0 = at(t0)
        if (p0 != null) builder.add(p0.first, p0.second)
        for (i in 1..baseSamples) {
            val t1 = if (i == baseSamples) tMax else tMin + (tMax - tMin) * i / baseSamples
            val p1 = at(t1)
            segment(t0, p0, t1, p1, 0)
            t0 = t1
            p0 = p1
            if (builder.pointCount > MAX_POINTS) break
        }
        return builder.finish()
    }

    private fun at(t: Double): Pair<Float, Float>? {
        point.at(t, tmp)
        val x = tmp[0]
        val y = tmp[1]
        if (!x.isFinite() || !y.isFinite()) return null
        val clampX = w * 3
        val clampY = h * 3
        return x.coerceIn(-clampX, w + clampX) to y.coerceIn(-clampY, h + clampY)
    }

    private fun outside(p: Pair<Float, Float>, side: Int): Boolean = when (side) {
        0 -> p.first < 0
        1 -> p.first > w
        2 -> p.second < 0
        else -> p.second > h
    }

    private fun segment(t0: Double, p0: Pair<Float, Float>?, t1: Double, p1: Pair<Float, Float>?, depth: Int) {
        if (p0 == null && p1 == null) { builder.breakLine(); return }
        if (p0 == null || p1 == null) {
            // Bisect to the edge of the defined part.
            var good = if (p0 != null) t0 else t1
            var bad = if (p0 != null) t1 else t0
            var goodP = p0 ?: p1!!
            repeat(30) {
                val m = 0.5 * (good + bad)
                val pm = at(m)
                if (pm != null) { good = m; goodP = pm } else bad = m
            }
            if (p0 != null) {
                segment(t0, p0, good, goodP, depth + 1)
                builder.breakLine()
            } else {
                builder.breakLine()
                builder.add(goodP.first, goodP.second)
                segment(good, goodP, t1, p1, depth + 1)
            }
            return
        }
        val dist = hypot(p1.first - p0.first, p1.second - p0.second)
        if (depth >= MAX_DEPTH) {
            if (dist > 3f) builder.breakLine()
            builder.add(p1.first, p1.second)
            return
        }
        val tm = 0.5 * (t0 + t1)
        val pm = at(tm)
        if (pm == null) {
            segment(t0, p0, tm, null, depth + 1)
            segment(tm, null, t1, p1, depth + 1)
            return
        }
        val sameSideOutside = (0..3).any { s -> outside(p0, s) && outside(p1, s) && outside(pm, s) }
        val chord = abs(pm.first - 0.5f * (p0.first + p1.first)) + abs(pm.second - 0.5f * (p0.second + p1.second))
        if (sameSideOutside || (dist < 2f && chord < 0.4f) || builder.pointCount > MAX_POINTS) {
            builder.add(p1.first, p1.second)
        } else {
            segment(t0, p0, tm, pm, depth + 1)
            segment(tm, pm, t1, p1, depth + 1)
        }
    }

    private companion object {
        const val MAX_DEPTH = 10
        const val MAX_POINTS = 60_000
    }
}
