package com.prostuti.student.domain.calculator

/** An exact fraction result, e.g. -1/3 → negative = true, numerator = 1, denominator = 3. */
data class CalcFraction(val negative: Boolean, val numerator: Long, val denominator: Long)
