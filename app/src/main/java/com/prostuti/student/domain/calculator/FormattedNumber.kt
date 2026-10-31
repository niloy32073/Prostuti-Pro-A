package com.prostuti.student.domain.calculator

/** A displayed number: "1.25" or "1.25" × 10^[exponent]. */
data class FormattedNumber(val mantissa: String, val exponent: Int? = null) {
    override fun toString(): String = if (exponent == null) mantissa else "${mantissa}E$exponent"
}
