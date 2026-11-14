package com.prostuti.student.domain.code.python

import com.prostuti.student.domain.code.CodeError
import com.prostuti.student.domain.code.CodeErrorKind
import com.prostuti.student.domain.code.CodeParseException

/** Splits Python source into [PyLine]s; blank and comment-only lines are dropped. */
internal class PyLineReader(source: String) {
    private val src = source.replace("\r\n", "\n").replace('\r', '\n')
    private var i = 0
    private var line = 1

    fun read(): List<PyLine> {
        val out = mutableListOf<PyLine>()
        while (i < src.length) {
            val startLine = line
            var indent = 0
            while (i < src.length && (src[i] == ' ' || src[i] == '\t' || src[i] == '\u000c')) {
                indent = when (src[i]) {
                    '\t' -> (indent / 8 + 1) * 8
                    ' ' -> indent + 1
                    else -> 0
                }
                i++
            }
            if (i >= src.length) break
            when (src[i]) {
                '\n' -> { i++; line++; continue }
                '#' -> { skipComment(); continue }
            }
            val text = readLogical()
            if (text.isNotEmpty()) out += PyLine(indent, text, startLine)
        }
        return out
    }

    /** Reads up to the newline that ends the logical line (consumed). */
    private fun readLogical(): String {
        val sb = StringBuilder()
        val open = ArrayDeque<Pair<Char, Int>>()
        while (i < src.length) {
            val c = src[i]
            when {
                c == '#' -> skipComment()
                c == '"' || c == '\'' -> readString(sb)
                c == '\\' && i + 1 < src.length && src[i + 1] == '\n' -> { sb.append(' '); i += 2; line++; skipBlanks() }
                c == '\n' -> {
                    i++; line++
                    if (open.isEmpty()) return sb.toString().trim()
                    sb.append(' ')
                    skipBlanks()
                }
                c == '(' || c == '[' || c == '{' -> { open.addLast(c to line); sb.append(c); i++ }
                c == ')' || c == ']' || c == '}' -> {
                    val last = open.removeLastOrNull()
                    if (last == null || PAIRS[last.first] != c) fail(line, CodeErrorKind.UNEXPECTED_CLOSE, c.toString())
                    sb.append(c); i++
                }
                else -> { sb.append(c); i++ }
            }
        }
        open.firstOrNull()?.let { (c, at) -> fail(at, CodeErrorKind.UNCLOSED_BRACKET, c.toString()) }
        return sb.toString().trim()
    }

    /** Drops a continuation line's indent, so joined lines read as one. */
    private fun skipBlanks() {
        while (i < src.length && (src[i] == ' ' || src[i] == '\t')) i++
    }

    private fun skipComment() {
        while (i < src.length && src[i] != '\n') i++
    }

    private fun readString(sb: StringBuilder) {
        val q = src[i]
        val startLine = line
        val triple = src.startsWith("$q$q$q", i)
        val quote = if (triple) "$q$q$q" else q.toString()
        sb.append(quote); i += quote.length
        while (i < src.length) {
            val c = src[i]
            when {
                c == '\\' && i + 1 < src.length -> {
                    if (src[i + 1] == '\n') line++
                    sb.append(c).append(src[i + 1]); i += 2
                }
                src.startsWith(quote, i) -> { sb.append(quote); i += quote.length; return }
                c == '\n' -> {
                    if (!triple) fail(startLine, CodeErrorKind.UNTERMINATED_STRING)
                    sb.append(c); i++; line++
                }
                else -> { sb.append(c); i++ }
            }
        }
        fail(startLine, CodeErrorKind.UNTERMINATED_STRING)
    }

    private fun fail(at: Int, kind: CodeErrorKind, detail: String = ""): Nothing =
        throw CodeParseException(CodeError(at, kind, detail))

    private companion object {
        val PAIRS = mapOf('(' to ')', '[' to ']', '{' to '}')
    }
}
