package com.prostuti.student.domain.graph

import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.max

/**
 * Globally adaptive Gauss–Kronrod (7/15 points) integration, the QUADPACK QAG scheme: the
 * interval with the largest error estimate is split until the total error meets the tolerance.
 * Infinite bounds are mapped onto finite ones. Nodes never touch the end points, so integrable
 * end-point singularities (1/√x on [0, 1]) are fine; a pole inside the interval gives NaN.
 */
object Integrator {

    fun integrate(
        f: RealFunction,
        a: Double,
        b: Double,
        absTolerance: Double = 1e-11,
        relTolerance: Double = 1e-10,
        maxIntervals: Int = 800,
    ): IntegralResult {
        if (a.isNaN() || b.isNaN()) return IntegralResult(Double.NaN, Double.NaN, false)
        if (a == b) return IntegralResult(0.0, 0.0, true)
        if (a > b) return integrate(f, b, a, absTolerance, relTolerance, maxIntervals).let { it.copy(value = -it.value) }
        val aInf = a == Double.NEGATIVE_INFINITY
        val bInf = b == Double.POSITIVE_INFINITY
        return when {
            // x = t / (1 − t²), t ∈ (−1, 1)
            aInf && bInf -> finite({ t ->
                val s = 1 - t * t
                f.value(t / s) * (1 + t * t) / (s * s)
            }, -1.0, 1.0, absTolerance, relTolerance, maxIntervals)
            // x = a + t / (1 − t), t ∈ [0, 1)
            bInf -> finite({ t ->
                val s = 1 - t
                f.value(a + t / s) / (s * s)
            }, 0.0, 1.0, absTolerance, relTolerance, maxIntervals)
            // x = b − (1 − t) / t, t ∈ (0, 1]
            aInf -> finite({ t -> f.value(b - (1 - t) / t) / (t * t) }, 0.0, 1.0, absTolerance, relTolerance, maxIntervals)
            else -> finite(f, a, b, absTolerance, relTolerance, maxIntervals)
        }
    }

    private class Piece(val a: Double, val b: Double, val value: Double, val error: Double)

    private fun finite(
        f: RealFunction,
        a: Double,
        b: Double,
        absTolerance: Double,
        relTolerance: Double,
        maxIntervals: Int,
    ): IntegralResult {
        val queue = PriorityQueue<Piece>(compareByDescending { it.error })
        val first = kronrod(f, a, b)
        if (!first.value.isFinite()) return IntegralResult(Double.NaN, Double.NaN, false)
        queue += first
        var total = first.value
        var totalError = first.error
        var intervals = 1
        while (totalError > max(absTolerance, relTolerance * abs(total)) && intervals < maxIntervals) {
            val worst = queue.poll() ?: break
            val mid = 0.5 * (worst.a + worst.b)
            if (mid <= worst.a || mid >= worst.b) { queue += worst; break } // can't split further
            val left = kronrod(f, worst.a, mid)
            val right = kronrod(f, mid, worst.b)
            if (!left.value.isFinite() || !right.value.isFinite()) return IntegralResult(Double.NaN, Double.NaN, false)
            total += left.value + right.value - worst.value
            totalError += left.error + right.error - worst.error
            queue += left
            queue += right
            intervals++
        }
        // Re-add exactly to undo round-off drift from the running sums.
        var sum = 0.0
        var err = 0.0
        for (p in queue) { sum += p.value; err += p.error }
        return IntegralResult(sum, err, err <= max(absTolerance, relTolerance * abs(sum)) * 10)
    }

    private fun kronrod(f: RealFunction, a: Double, b: Double): Piece {
        val center = 0.5 * (a + b)
        val half = 0.5 * (b - a)
        val fc = f.value(center)
        var kronrod = fc * WGK[7]
        var gauss = fc * WG[3]
        for (j in 0 until 7) {
            val dx = half * XGK[j]
            val sum = f.value(center - dx) + f.value(center + dx)
            kronrod += WGK[j] * sum
            if (j % 2 == 1) gauss += WG[j / 2] * sum
        }
        val value = kronrod * half
        return Piece(a, b, value, abs((kronrod - gauss) * half))
    }

    private val XGK = doubleArrayOf(
        0.991455371120812639206854697526329, 0.949107912342758524526189684047851,
        0.864864423359769072789712788640926, 0.741531185599394439863864773280788,
        0.586087235467691130294144845693013, 0.405845151377397166906606412076961,
        0.207784955007898467600689403773245, 0.0,
    )
    private val WGK = doubleArrayOf(
        0.022935322010529224963732008058970, 0.063092092629978553290700663189204,
        0.104790010322250183839876322541518, 0.140653259715525918745189590510238,
        0.169004726639267902826583426598550, 0.190350578064785409913256402421014,
        0.204432940075298892414161999234649, 0.209482141084727828012999174891714,
    )

    /** 7-point Gauss weights for the odd Kronrod nodes (±0.949…, ±0.741…, ±0.405…) and the centre. */
    private val WG = doubleArrayOf(
        0.129484966168869693270611432679082, 0.279705391489276667901467771423780,
        0.381830050505118944950369775488975, 0.417959183673469387755102040816327,
    )
}
