package com.prostuti.student.domain.code.python

import com.prostuti.student.domain.code.CodeScan

/** Bracket- and string-aware scans over one logical Python line. */
internal object PyText {

    /** Index of the `:` that ends a compound statement's header, or -1 (skips `:=`, slices, lambdas). */
    fun headerColon(text: String): Int {
        var depth = 0
        var lambdas = 0
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' || c == '\'' -> { i = CodeScan.skipQuoted(text, i); continue }
                c == '(' || c == '[' || c == '{' -> depth++
                c == ')' || c == ']' || c == '}' -> depth--
                CodeScan.isIdentStart(c) -> {
                    val s = i
                    while (i < text.length && CodeScan.isIdentPart(text[i])) i++
                    if (depth == 0 && text.substring(s, i) == "lambda") lambdas++
                    continue
                }
                c == ':' && depth == 0 -> {
                    if (i + 1 < text.length && text[i + 1] == '=') { i += 2; continue }
                    if (lambdas > 0) lambdas-- else return i
                }
            }
            i++
        }
        return -1
    }

    /** Splits on [sep] outside brackets and literals; empty parts are dropped. */
    fun splitTopLevel(text: String, sep: Char): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' || c == '\'' -> { i = CodeScan.skipQuoted(text, i); continue }
                c == '(' || c == '[' || c == '{' -> depth++
                c == ')' || c == ']' || c == '}' -> depth--
                c == sep && depth == 0 -> { parts += text.substring(start, i); start = i + 1 }
            }
            i++
        }
        parts += text.substring(start)
        return parts.map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** The leading identifier ("" when the line starts with something else). */
    fun firstWord(text: String): String {
        var i = 0
        while (i < text.length && CodeScan.isIdentPart(text[i])) i++
        return if (i > 0 && CodeScan.isIdentStart(text[0])) text.substring(0, i) else ""
    }

    /** True when the whole statement is one string literal (a docstring), optionally prefixed. */
    fun isStringOnly(text: String): Boolean {
        var i = 0
        while (i < text.length && text[i].isLetter() && i < 2) i++
        if (i >= text.length || (text[i] != '"' && text[i] != '\'')) return false
        return CodeScan.skipQuoted(text, i) == text.length
    }

    /** True when `in` appears as a top-level word (a `for` header's separator). */
    fun hasTopLevelIn(text: String): Boolean {
        var depth = 0
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' || c == '\'' -> { i = CodeScan.skipQuoted(text, i); continue }
                c == '(' || c == '[' || c == '{' -> depth++
                c == ')' || c == ']' || c == '}' -> depth--
                CodeScan.isIdentStart(c) -> {
                    val s = i
                    while (i < text.length && CodeScan.isIdentPart(text[i])) i++
                    if (depth == 0 && text.substring(s, i) == "in") return true
                    continue
                }
            }
            i++
        }
        return false
    }
}
