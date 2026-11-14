package com.prostuti.student.domain.code.c

import com.prostuti.student.domain.code.CodeError
import com.prostuti.student.domain.code.CodeErrorKind
import com.prostuti.student.domain.code.CodeParseException
import com.prostuti.student.domain.code.CodeScan

/**
 * Splits C source into tokens. Comments are dropped; preprocessor lines (`#include …`) too, unless
 * [keepDirectives] keeps each as one [CTokenKind.DIRECTIVE] token (the runner reads `#define`).
 */
internal class CLexer(private val src: String, private val keepDirectives: Boolean = false) {
    private var i = 0
    private var line = 1
    private var atLineStart = true

    fun tokens(): List<CToken> {
        val out = mutableListOf<CToken>()
        while (i < src.length) {
            val c = src[i]
            when {
                c == '\n' -> { i++; line++; atLineStart = true }
                c.isWhitespace() -> i++
                src.startsWith("//", i) -> while (i < src.length && src[i] != '\n') i++
                src.startsWith("/*", i) -> blockComment()
                c == '#' && atLineStart -> {
                    val start = i
                    val startLine = line
                    directive()
                    if (keepDirectives) out += CToken(CTokenKind.DIRECTIVE, src.substring(start, i), start, i, startLine)
                }
                else -> { out += token(); atLineStart = false }
            }
        }
        return out
    }

    private fun token(): CToken {
        val start = i
        val c = src[i]
        val kind = when {
            CodeScan.isIdentStart(c) -> {
                while (i < src.length && CodeScan.isIdentPart(src[i])) i++
                // L"…", u8"…" string prefixes belong to the literal.
                if (i < src.length && (src[i] == '"' || src[i] == '\'') && src.substring(start, i) in PREFIXES) return quoted(start)
                CTokenKind.IDENT
            }
            c.isDigit() || (c == '.' && i + 1 < src.length && src[i + 1].isDigit()) -> { number(start); CTokenKind.NUMBER }
            c == '"' || c == '\'' -> return quoted(start)
            else -> {
                val p = PUNCTUATORS.firstOrNull { src.startsWith(it, i) } ?: c.toString()
                i += p.length
                CTokenKind.PUNCT
            }
        }
        return CToken(kind, src.substring(start, i), start, i, line)
    }

    private fun number(start: Int) {
        val hex = src.startsWith("0x", start) || src.startsWith("0X", start)
        val exponents = if (hex) "pP" else "eE"
        while (i < src.length) {
            val c = src[i]
            if ((c == '+' || c == '-') && src[i - 1] in exponents) { i++; continue }
            if (CodeScan.isIdentPart(c) || c == '.') i++ else break
        }
    }

    private fun quoted(start: Int): CToken {
        val startLine = line
        while (src[i] != '"' && src[i] != '\'') i++
        val q = src[i]
        i++
        while (i < src.length) {
            when (src[i]) {
                '\\' -> { if (i + 1 < src.length && src[i + 1] == '\n') line++; i += 2 }
                q -> { i++; return CToken(if (q == '"') CTokenKind.STRING else CTokenKind.CHAR, src.substring(start, i), start, i, startLine) }
                '\n' -> fail(startLine, CodeErrorKind.UNTERMINATED_STRING)
                else -> i++
            }
        }
        fail(startLine, CodeErrorKind.UNTERMINATED_STRING)
    }

    private fun blockComment() {
        val startLine = line
        val end = src.indexOf("*/", i + 2)
        if (end < 0) fail(startLine, CodeErrorKind.UNTERMINATED_COMMENT)
        line += src.substring(i, end).count { it == '\n' }
        i = end + 2
    }

    /** Skips a preprocessor line, with `\` continuations. */
    private fun directive() {
        while (i < src.length && src[i] != '\n') {
            if (src[i] == '\\' && i + 1 < src.length && src[i + 1] == '\n') { i += 2; line++ } else i++
        }
    }

    private fun fail(at: Int, kind: CodeErrorKind): Nothing = throw CodeParseException(CodeError(at, kind))

    private companion object {
        val PREFIXES = setOf("L", "u", "U", "u8")
        // Longest first, so `<<=` wins over `<<` and `<`.
        val PUNCTUATORS = listOf(
            "<<=", ">>=", "...",
            "->", "++", "--", "<<", ">>", "<=", ">=", "==", "!=", "&&", "||",
            "+=", "-=", "*=", "/=", "%=", "&=", "^=", "|=", "##", "::",
        )
    }
}
