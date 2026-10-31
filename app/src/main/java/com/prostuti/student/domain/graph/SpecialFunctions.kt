package com.prostuti.student.domain.graph

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/** Numeric helpers shared by the compiler and the calculus code. Never throw — undefined is NaN. */
internal object SpecialFunctions {

    /**
     * Real power. A negative base with a rational exponent of odd denominator has a real value:
     * (−8)^(1/3) = −2, (−8)^(2/3) = 4 — what a student expects from x^(1/3), unlike Math.pow's NaN.
     */
    fun realPow(base: Double, exponent: Double): Double {
        if (base >= 0.0 || base.isNaN() || exponent.isNaN()) return base.pow(exponent)
        if (exponent == floor(exponent)) return base.pow(exponent)
        val (p, q) = rationalExponent(exponent) ?: return Double.NaN
        if (q % 2 == 0L) return Double.NaN
        val magnitude = (-base).pow(exponent)
        return if (p % 2 == 0L) magnitude else -magnitude
    }

    /** exponent ≈ p/q with a small odd-or-even q, or null when it is not a simple fraction. */
    private fun rationalExponent(exponent: Double): Pair<Long, Long>? {
        for (q in 2L..64L) {
            val p = round(exponent * q)
            if (abs(exponent * q - p) < 1e-9) return p.toLong() to q
        }
        return null
    }

    fun intPow(base: Double, n: Int): Double {
        if (n < 0) return 1.0 / intPow(base, -n)
        var result = 1.0
        var b = base
        var e = n
        while (e > 0) {
            if (e and 1 == 1) result *= b
            b *= b
            e = e shr 1
        }
        return result
    }

    private val lanczos = doubleArrayOf(
        0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
        -176.61502916214059, 12.507343278686905, -0.13857109526572012,
        9.9843695780195716e-6, 1.5056327351493116e-7,
    )

    /** Γ(x) by Lanczos (g = 7), ~15 digits; poles at 0, −1, −2… are NaN. */
    fun gamma(x: Double): Double {
        if (x.isNaN()) return Double.NaN
        if (x <= 0.0 && x == floor(x)) return Double.NaN
        if (x > 171.7) return Double.POSITIVE_INFINITY
        if (x == floor(x) && x <= 30.0) return factorialInt(x.toInt() - 1)
        if (x < 0.5) return PI / (sin(PI * x) * gamma(1.0 - x))
        val z = x - 1.0
        var a = lanczos[0]
        val t = z + 7.5
        for (i in 1 until lanczos.size) a += lanczos[i] / (z + i)
        return sqrt(2 * PI) * t.pow(z + 0.5) * exp(-t) * a
    }

    private fun factorialInt(n: Int): Double {
        var r = 1.0
        for (i in 2..n) r *= i
        return r
    }

    /** x! = Γ(x + 1), so it is smooth between integers like on a scientific calculator's graph. */
    fun factorial(x: Double): Double = gamma(x + 1.0)

    fun nCr(n: Double, r: Double): Double {
        if (n != floor(n) || r != floor(r) || n < 0 || r < 0) return Double.NaN
        if (r > n) return 0.0
        val k = minOf(r, n - r).toInt()
        var result = 1.0
        for (i in 1..k) result = result * (n - k + i) / i
        return round(result)
    }

    fun nPr(n: Double, r: Double): Double {
        if (n != floor(n) || r != floor(r) || n < 0 || r < 0) return Double.NaN
        if (r > n) return 0.0
        var result = 1.0
        for (i in 0 until r.toInt()) result *= (n - i)
        return result
    }

    fun gcd(a: Double, b: Double): Double {
        if (a != floor(a) || b != floor(b)) return Double.NaN
        var x = abs(a)
        var y = abs(b)
        while (y > 0.5) {
            val t = x % y
            x = y
            y = t
        }
        return x
    }

    fun lcm(a: Double, b: Double): Double {
        val g = gcd(a, b)
        return if (g.isNaN() || g == 0.0) g else abs(a * b) / g
    }

    /** Mathematical modulo: the result has the divisor's sign (mod(−1, 3) = 2). */
    fun mod(a: Double, b: Double): Double = if (b == 0.0) Double.NaN else a - b * floor(a / b)

    /** Round half away from zero, as taught in school (2.5 → 3, −2.5 → −3). */
    fun roundHalfAway(x: Double): Double = if (x < 0) -floor(-x + 0.5) else floor(x + 0.5)

    fun sign(x: Double): Double = when {
        x > 0 -> 1.0
        x < 0 -> -1.0
        x == 0.0 -> 0.0
        else -> Double.NaN
    }

    fun log(x: Double, base: Double): Double = ln(x) / ln(base)
}
