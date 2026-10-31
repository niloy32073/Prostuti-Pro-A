package com.prostuti.student.domain.graph.plot

import com.prostuti.student.domain.graph.RealFunction
import com.prostuti.student.domain.graph.RootFinder
import kotlin.math.abs
import kotlin.math.max

/**
 * Roots, minima/maxima, the y-intercept and intersections of y = f(x) curves on [xMin, xMax].
 * Candidates come from sign changes on a dense grid, are polished with Brent's method, then
 * checked — a "root" where |f| is large is a pole (tan x at π/2), not a root, and is dropped.
 */
object PointOfInterestFinder {

    private const val SAMPLES = 800
    private const val MAX_POINTS = 60

    fun forCurve(f: RealFunction, derivative: RealFunction?, xMin: Double, xMax: Double, yScale: Double): List<PointOfInterest> {
        val out = ArrayList<PointOfInterest>()
        val tolerance = 1e-7 * max(1.0, yScale)
        for (x in signChangeRoots(f, xMin, xMax)) {
            val y = f.value(x)
            if (y.isFinite() && abs(y) <= tolerance) out += PointOfInterest(x, 0.0, PointKind.ROOT)
        }
        if (derivative != null) {
            for (x in signChangeRoots(derivative, xMin, xMax)) {
                val y = f.value(x)
                if (!y.isFinite()) continue
                val h = (xMax - xMin) * 1e-4
                val left = f.value(x - h)
                val right = f.value(x + h)
                val kind = when {
                    left > y && right > y -> PointKind.MINIMUM
                    left < y && right < y -> PointKind.MAXIMUM
                    else -> continue
                }
                // A double root (x² at 0) touches the axis without a sign change: it is both.
                if (abs(y) <= tolerance && out.none { it.kind == PointKind.ROOT && abs(it.x - x) < h }) {
                    out += PointOfInterest(x, 0.0, PointKind.ROOT)
                }
                out += PointOfInterest(x, y, kind)
            }
        }
        if (xMin < 0 && xMax > 0) {
            val y0 = f.value(0.0)
            if (y0.isFinite()) out += PointOfInterest(0.0, y0, PointKind.Y_INTERCEPT)
        }
        return dedupe(out, (xMax - xMin) * 1e-6).take(MAX_POINTS)
    }

    fun intersections(f: RealFunction, g: RealFunction, xMin: Double, xMax: Double, yScale: Double): List<PointOfInterest> {
        val diff = RealFunction { x -> f.value(x) - g.value(x) }
        val tolerance = 1e-7 * max(1.0, yScale)
        val out = ArrayList<PointOfInterest>()
        for (x in signChangeRoots(diff, xMin, xMax)) {
            val d = diff.value(x)
            val y = f.value(x)
            if (d.isFinite() && abs(d) <= tolerance && y.isFinite()) out += PointOfInterest(x, y, PointKind.INTERSECTION)
        }
        return dedupe(out, (xMax - xMin) * 1e-6).take(MAX_POINTS)
    }

    private fun signChangeRoots(f: RealFunction, xMin: Double, xMax: Double): List<Double> {
        val roots = ArrayList<Double>()
        var x0 = xMin
        var v0 = f.value(x0)
        for (i in 1..SAMPLES) {
            val x1 = xMin + (xMax - xMin) * i / SAMPLES
            val v1 = f.value(x1)
            when {
                v1 == 0.0 -> roots += x1
                v0.isFinite() && v1.isFinite() && (v0 < 0) != (v1 < 0) && v0 != 0.0 ->
                    RootFinder.brent(f, x0, x1, v0, v1)?.let { roots += it }
            }
            x0 = x1
            v0 = v1
            if (roots.size > MAX_POINTS) break
        }
        return roots
    }

    private fun dedupe(points: List<PointOfInterest>, eps: Double): List<PointOfInterest> {
        val out = ArrayList<PointOfInterest>()
        for (p in points) {
            if (out.none { it.kind == p.kind && abs(it.x - p.x) <= eps }) out += p
        }
        return out
    }
}
