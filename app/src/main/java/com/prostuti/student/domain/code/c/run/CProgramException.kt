package com.prostuti.student.domain.code.c.run

/** A problem in a C program: found before running ([isCompile]) or while running. [line] is 1-based. */
class CProgramException(val line: Int, val reason: String, val isCompile: Boolean) : Exception(reason) {
    /** As a compiler or runtime would print it. */
    fun report(): String = (if (isCompile) "error" else "runtime error") + ": line $line: $reason\n"
}
