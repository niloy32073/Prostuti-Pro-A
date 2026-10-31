package com.prostuti.student.domain.calculator

/** Result of SOLVE: the root found and the left-minus-right residual at it. */
data class SolveOutcome(val value: Double, val leftMinusRight: Double)
