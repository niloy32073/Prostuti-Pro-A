package com.prostuti.student.domain.code

/** A coloured stretch of code, [start] inclusive to [end] exclusive. */
data class CodeSpan(val start: Int, val end: Int, val kind: CodeSpanKind)
