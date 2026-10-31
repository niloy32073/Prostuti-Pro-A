package com.prostuti.student.domain.graph

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sqrt

/** Formats graph numbers for people: 6 significant digits, no float noise, and exact forms (8/3, π/2, √2). */
object GraphNumberFormat {

    /** 2.6666666667 → "2.666667", 1e-17 → "0", 1234567890 → "1.234568×10⁹". */
    fun format(value: Double, significant: Int = 6): String {
        if (value.isNaN()) return "undefined"
        if (value == Double.POSITIVE_INFINITY) return "∞"
        if (value == Double.NEGATIVE_INFINITY) return "−∞"
        if (abs(value) < 1e-12) return "0"
        val magnitude = abs(value)
        val text = if (magnitude >= 1e-4 && magnitude < 1e9) {
            BigDecimal(value).round(MathContext(significant)).stripTrailingZeros().toPlainString()
        } else {
            scientific(value, significant)
        }
        return text.replace('-', '−')
    }

    private fun scientific(value: Double, significant: Int): String {
        val exponent = floor(kotlin.math.log10(abs(value))).toInt()
        val mantissa = BigDecimal(value / Math.pow(10.0, exponent.toDouble()))
            .round(MathContext(significant)).stripTrailingZeros().toPlainString()
        return mantissa + "×10" + superscript(exponent)
    }

    private fun superscript(n: Int): String = n.toString().map {
        when (it) {
            '-' -> '⁻'
            else -> "⁰¹²³⁴⁵⁶⁷⁸⁹"[it - '0']
        }
    }.joinToString("")

    /**
     * An exact form when [value] is (to 1e-9) a small fraction, a fraction of π, or √n / p√n / q:
     * returns plain text like "8/3", "−π/2", "3√2", or null.
     */
    fun exactForm(value: Double): String? {
        if (!value.isFinite() || abs(value) < 1e-12 || abs(value) > 1e6) return null
        if (abs(value - round(value)) < 1e-9 * maxOf(1.0, abs(value))) return null // plain integer: nothing to add
        fraction(value, 1000)?.let { (p, q) -> return signed(p, "${abs(p)}/$q") }
        fraction(value / PI, 48)?.let { (p, q) ->
            val n = abs(p)
            val head = if (n == 1L) "π" else "${n}π"
            return signed(p, if (q == 1L) head else "$head/$q")
        }
        // p√n / q with small square-free n.
        for (n in listOf(2L, 3L, 5L, 6L, 7L, 10L)) {
            fraction(value / sqrt(n.toDouble()), 24)?.let { (p, q) ->
                val k = abs(p)
                val head = if (k == 1L) "√$n" else "$k√$n"
                return signed(p, if (q == 1L) head else "$head/$q")
            }
        }
        return null
    }

    private fun signed(p: Long, body: String) = if (p < 0) "−$body" else body

    /** Best p/q with q ≤ [maxDenominator] by continued fractions, if it matches to 1e-9 (relative). */
    private fun fraction(value: Double, maxDenominator: Long): Pair<Long, Long>? {
        if (!value.isFinite()) return null
        var h0 = 0L; var h1 = 1L
        var k0 = 1L; var k1 = 0L
        var x = value
        repeat(40) {
            val a = floor(x)
            if (abs(a) > 1e12) return null
            val h2 = a.toLong() * h1 + h0
            val k2 = a.toLong() * k1 + k0
            if (k2 > maxDenominator) return null
            h0 = h1; h1 = h2; k0 = k1; k1 = k2
            if (abs(value - h1.toDouble() / k1) < 1e-9 * maxOf(1.0, abs(value))) {
                return if (k1 < 0) -h1 to -k1 else h1 to k1
            }
            val frac = x - a
            if (frac < 1e-15) return null
            x = 1 / frac
        }
        return null
    }
}
