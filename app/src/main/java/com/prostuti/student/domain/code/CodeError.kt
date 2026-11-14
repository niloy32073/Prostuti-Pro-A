package com.prostuti.student.domain.code

/** The first problem found in a program, at a 1-based [line]. */
data class CodeError(val line: Int, val kind: CodeErrorKind, val detail: String = "")
