package com.prostuti.student.domain.graph.plot

import com.prostuti.student.domain.graph.GraphNumberFormat
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

/** "Nice" grid steps (1, 2, 5 × 10ⁿ, or π/4, π/2, π, 2π… on a π axis) and their labels. */
object GridTicks {

    fun step(unitsPerPx: Double, targetPx: Double = 96.0, piAxis: Boolean = false): GridStep {
        val raw = unitsPerPx * targetPx
        if (piAxis) {
            val inPi = raw / PI
            val candidates = listOf(1.0 / 12, 1.0 / 6, 0.25, 0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0)
            val best = candidates.firstOrNull { it >= inPi } ?: (10.0.pow(ceil(log10(inPi))))
            val divisions = when (best) { 0.25, 0.5 -> 2; 1.0 / 12, 1.0 / 6 -> 2; else -> 4 }
            return GridStep(best * PI, divisions, piMultiple = true)
        }
        val exponent = floor(log10(raw))
        val base = 10.0.pow(exponent)
        val fraction = raw / base
        return when {
            fraction <= 1.0 -> GridStep(base, 5, false)
            fraction <= 2.0 -> GridStep(2 * base, 4, false)
            fraction <= 5.0 -> GridStep(5 * base, 5, false)
            else -> GridStep(10 * base, 5, false)
        }
    }

    /** Multiples of [step] in [min, max]. */
    fun ticks(min: Double, max: Double, step: Double): List<Double> {
        if (!(step > 0) || !min.isFinite() || !max.isFinite()) return emptyList()
        val first = ceil(min / step)
        val last = floor(max / step)
        if (last - first > 2000) return emptyList()
        return (first.toLong()..last.toLong()).map { it * step }
    }

    /** Axis label for [value]: 0.30000000004 → "0.3", 3π/2 on a π axis → "3π/2". */
    fun label(value: Double, step: GridStep): String {
        if (abs(value) < step.major * 1e-6) return "0"
        if (step.piMultiple) {
            val k = value / PI
            // k is a multiple of the step in π units: express as p/q with q from the step.
            val q = when {
                step.major / PI < 0.2 -> 12L
                step.major / PI < 0.4 -> 4L
                step.major / PI < 0.9 -> 2L
                else -> 1L
            }
            var p = round(k * q).toLong()
            var d = q
            val g = gcd(abs(p), d)
            p /= g; d /= g
            val sign = if (p < 0) "−" else ""
            val n = abs(p)
            val head = if (n == 1L) "π" else "${n}π"
            return sign + if (d == 1L) head else "$head/$d"
        }
        val decimals = (-floor(log10(step.major))).toInt().coerceIn(0, 12)
        val rounded = round(value * 10.0.pow(decimals)) / 10.0.pow(decimals)
        return GraphNumberFormat.format(rounded, significant = 8)
    }

    private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) maxOf(a, 1L) else gcd(b, a % b)
}
