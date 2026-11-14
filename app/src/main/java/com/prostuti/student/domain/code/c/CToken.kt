package com.prostuti.student.domain.code.c

/** One C token; [start]/[end] are offsets in the source, [line] is 1-based. */
data class CToken(val kind: CTokenKind, val text: String, val start: Int, val end: Int, val line: Int) {
    fun isPunct(p: String): Boolean = kind == CTokenKind.PUNCT && text == p
    fun isWord(w: String): Boolean = kind == CTokenKind.IDENT && text == w
}
