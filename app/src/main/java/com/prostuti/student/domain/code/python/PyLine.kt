package com.prostuti.student.domain.code.python

/**
 * One logical Python line: physical lines joined inside brackets or after a trailing `\`, with
 * comments removed. [indent] counts columns (a tab goes to the next multiple of 8, as in Python).
 */
data class PyLine(val indent: Int, val text: String, val line: Int)
