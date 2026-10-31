package com.prostuti.student.domain.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor

/** Turns values into what the LCD shows: 10 display digits, ×10ⁿ when needed, fractions, D°M'S. */
class CalcNumberFormatter {

    fun format(value: Double, format: CalcNumberFormat): FormattedNumber {
        if (value == 0.0) {
            return when (format) {
                is CalcNumberFormat.Fix -> FormattedNumber(BigDecimal.ZERO.setScale(format.places).toPlainString())
                is CalcNumberFormat.Sci -> FormattedNumber(BigDecimal.ZERO.setScale(format.digits - 1).toPlainString(), 0)
                is CalcNumberFormat.Norm -> FormattedNumber("0")
            }
        }
        return when (format) {
            is CalcNumberFormat.Norm -> norm(value, format.type)
            is CalcNumberFormat.Fix -> fix(value, format.places)
            is CalcNumberFormat.Sci -> sci(value, format.digits, strip = false)
        }
    }

    /** Engineering notation: exponent a multiple of 3, moved by [step] groups of three. */
    fun formatEngineering(value: Double, step: Int): FormattedNumber {
        if (value == 0.0) return FormattedNumber("0", 0)
        val rounded = round(value, DISPLAY_DIGITS)
        val exponent = Math.floorDiv(exponentOf(rounded), 3) * 3 + 3 * step
        return FormattedNumber(plain(rounded.movePointLeft(exponent)), exponent)
    }

    /** The exact fraction for [value] when it has one that fits the display (≤ 10 digits). */
    fun fraction(value: Double): CalcFraction? {
        if (value == 0.0 || CalcMath.isWhole(value) || abs(value) >= 1e10 || abs(value) < 1e-10) return null
        val (p, q) = CalcMath.approximateFraction(value, 10_000_000_000L, 1e-12) ?: return null
        if (q <= 1) return null
        val digits = abs(p).toString().length + q.toString().length
        if (digits > DISPLAY_DIGITS) return null
        return CalcFraction(negative = p < 0, numerator = abs(p), denominator = q)
    }

    /** Degrees-minutes-seconds text, e.g. 12.5 → 12°30°0°. */
    fun sexagesimal(value: Double): String {
        val negative = value < 0
        var rest = abs(value)
        val degrees = floor(rest)
        rest = (rest - degrees) * 60
        val minutes = floor(rest)
        var seconds = CalcMath.roundSignificant((rest - minutes) * 60, 10)
        var m = minutes.toLong()
        var d = degrees.toLong()
        if (seconds >= 60) { seconds -= 60; m += 1 }
        if (m >= 60) { m -= 60; d += 1 }
        val secondsText = plain(BigDecimal(seconds).setScale(2, RoundingMode.HALF_UP))
        return (if (negative) "-" else "") + "$d°$m°$secondsText°"
    }

    private fun norm(value: Double, type: Int): FormattedNumber {
        val rounded = round(value, DISPLAY_DIGITS)
        val magnitude = rounded.abs()
        val lowerLimit = if (type == 2) BigDecimal("1E-9") else BigDecimal("0.01")
        val useSci = magnitude < lowerLimit || magnitude >= BigDecimal("1E10")
        return if (useSci) sci(value, DISPLAY_DIGITS, strip = true) else FormattedNumber(plain(rounded))
    }

    private fun fix(value: Double, places: Int): FormattedNumber {
        if (abs(value) >= 1e10) return sci(value, DISPLAY_DIGITS, strip = true)
        val scaled = BigDecimal(value).setScale(places, RoundingMode.HALF_UP)
        return FormattedNumber(scaled.toPlainString().let { if (it.startsWith("-") && scaled.signum() == 0) it.drop(1) else it })
    }

    private fun sci(value: Double, digits: Int, strip: Boolean): FormattedNumber {
        val rounded = round(value, digits)
        val exponent = exponentOf(rounded)
        val mantissa = rounded.movePointLeft(exponent)
        val text = if (strip) plain(mantissa) else mantissa.setScale(digits - 1, RoundingMode.HALF_UP).toPlainString()
        return FormattedNumber(text, exponent)
    }

    private fun round(value: Double, digits: Int): BigDecimal =
        BigDecimal(value).round(MathContext(digits, RoundingMode.HALF_UP))

    private fun exponentOf(x: BigDecimal): Int = x.precision() - x.scale() - 1

    private fun plain(x: BigDecimal): String {
        val stripped = x.stripTrailingZeros()
        return if (stripped.signum() == 0) "0" else stripped.toPlainString()
    }

    private companion object {
        const val DISPLAY_DIGITS = 10
    }
}
