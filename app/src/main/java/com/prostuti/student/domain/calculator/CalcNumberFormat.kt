package com.prostuti.student.domain.calculator

/** The SETUP display formats: Norm 1/2, Fix 0–9 decimals, Sci 1–10 significant digits. */
sealed interface CalcNumberFormat {
    data class Norm(val type: Int = 1) : CalcNumberFormat
    data class Fix(val places: Int) : CalcNumberFormat
    data class Sci(val digits: Int) : CalcNumberFormat
}
