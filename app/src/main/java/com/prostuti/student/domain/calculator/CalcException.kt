package com.prostuti.student.domain.calculator

/** Thrown inside the calculator engine; [position] is the token index the error points at. */
class CalcException(val kind: CalcErrorKind, val position: Int? = null) : Exception(kind.label)
