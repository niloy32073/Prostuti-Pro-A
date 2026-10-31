package com.prostuti.student.domain.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor

/** Small numeric helpers shared by the evaluator, the solver and the formatter. */
internal object CalcMath {

    /** The calculator keeps 15 significant digits internally, like the real one. */
    const val INTERNAL_DIGITS = 15

    fun roundSignificant(value: Double, digits: Int): Double {
        if (value == 0.0 || !value.isFinite()) return value
        val rounded = BigDecimal(value).round(MathContext(digits, RoundingMode.HALF_UP)).toDouble()
        return if (rounded == 0.0) 0.0 else rounded   // drops -0.0
    }

    fun isWhole(x: Double): Boolean = x.isFinite() && abs(x) < 1e15 && x == floor(x)

    /**
     * Best fraction p/q for [x] with q ≤ [maxDenominator] (continued fractions), or null when
     * none is within [tolerance] (relative to max(1, |x|)).
     */
    fun approximateFraction(x: Double, maxDenominator: Long, tolerance: Double): Pair<Long, Long>? {
        if (!x.isFinite() || abs(x) >= 1e15) return null
        val sign = if (x < 0) -1 else 1
        val target = abs(x)
        var h0 = 0L; var h1 = 1L   // numerators
        var k0 = 1L; var k1 = 0L   // denominators
        var rest = target
        repeat(40) {
            val a = floor(rest)
            if (a > 1e15) return null
            val aL = a.toLong()
            val h2 = aL * h1 + h0
            val k2 = aL * k1 + k0
            if (k2 > maxDenominator || k2 <= 0 || h2 < 0) return null
            h0 = h1; h1 = h2; k0 = k1; k1 = k2
            if (abs(target - h1.toDouble() / k1) <= tolerance * maxOf(1.0, target)) return (sign * h1) to k1
            val frac = rest - a
            if (frac < 1e-18) return null
            rest = 1 / frac
        }
        return null
    }
}
