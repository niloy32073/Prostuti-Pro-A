package com.prostuti.student.domain.graph

/** [converged] is false when the error target was not reached (divergent or very rough integrands). */
data class IntegralResult(val value: Double, val error: Double, val converged: Boolean)
