package com.prostuti.student.domain.code

/**
 * Finds keywords, strings, comments, numbers and calls for syntax colouring. Forgiving: an
 * unfinished string or comment just runs to the end of its line (or of the code).
 */
object CodeHighlighter {

    fun spans(code: String, language: CodeLanguage): List<CodeSpan> {
        val out = mutableListOf<CodeSpan>()
        val python = language == CodeLanguage.PYTHON
        val keywords = if (python) PY_KEYWORDS else C_KEYWORDS
        val types = if (python) PY_BUILTINS else C_TYPES
        var i = 0
        var lineStart = true
        while (i < code.length) {
            val c = code[i]
            val start = i
            when {
                c == '\n' -> { i++; lineStart = true; continue }
                c == ' ' || c == '\t' -> { i++; continue }
                python && c == '#' || !python && code.startsWith("//", i) -> {
                    i = code.indexOf('\n', i).let { if (it < 0) code.length else it }
                    out += CodeSpan(start, i, CodeSpanKind.COMMENT)
                }
                !python && code.startsWith("/*", i) -> {
                    i = code.indexOf("*/", i + 2).let { if (it < 0) code.length else it + 2 }
                    out += CodeSpan(start, i, CodeSpanKind.COMMENT)
                }
                !python && c == '#' && lineStart -> {
                    i = code.indexOf('\n', i).let { if (it < 0) code.length else it }
                    out += CodeSpan(start, i, CodeSpanKind.DIRECTIVE)
                }
                c == '"' || c == '\'' -> {
                    i = stringEnd(code, i, python)
                    out += CodeSpan(start, i, CodeSpanKind.STRING)
                }
                c.isDigit() -> {
                    while (i < code.length && (CodeScan.isIdentPart(code[i]) || code[i] == '.')) i++
                    out += CodeSpan(start, i, CodeSpanKind.NUMBER)
                }
                CodeScan.isIdentStart(c) -> {
                    while (i < code.length && CodeScan.isIdentPart(code[i])) i++
                    val word = code.substring(start, i)
                    if (i < code.length && (code[i] == '"' || code[i] == '\'') && python && word.lowercase() in PY_PREFIXES) {
                        i = stringEnd(code, i, true)
                        out += CodeSpan(start, i, CodeSpanKind.STRING)
                    } else {
                        var j = i
                        while (j < code.length && code[j] == ' ') j++
                        val kind = when {
                            word in keywords -> CodeSpanKind.KEYWORD
                            word in types -> CodeSpanKind.TYPE
                            j < code.length && code[j] == '(' -> CodeSpanKind.CALL
                            else -> null
                        }
                        if (kind != null) out += CodeSpan(start, i, kind)
                    }
                }
                else -> i++
            }
            lineStart = false
        }
        return out
    }

    private fun stringEnd(code: String, start: Int, python: Boolean): Int {
        val q = code[start]
        val triple = python && code.startsWith("$q$q$q", start)
        var i = start + if (triple) 3 else 1
        while (i < code.length) {
            val c = code[i]
            when {
                c == '\\' -> i += 2
                triple && code.startsWith("$q$q$q", i) -> return i + 3
                !triple && c == q -> return i + 1
                !triple && c == '\n' -> return i
                else -> i++
            }
        }
        return code.length
    }

    private val PY_PREFIXES = setOf("r", "b", "f", "u", "rb", "br", "fr", "rf")
    private val PY_KEYWORDS = setOf(
        "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", "continue", "def",
        "del", "elif", "else", "except", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda",
        "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield", "match", "case",
    )
    private val PY_BUILTINS = setOf(
        "print", "input", "int", "float", "str", "bool", "list", "dict", "set", "tuple", "range", "len", "abs",
        "min", "max", "sum", "sorted", "round", "type", "enumerate", "zip", "map", "filter", "open",
    )
    private val C_KEYWORDS = setOf(
        "if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue", "return", "goto",
        "sizeof", "typedef", "struct", "union", "enum", "const", "static", "extern", "register", "volatile",
        "inline", "auto",
    )
    private val C_TYPES = setOf(
        "int", "char", "float", "double", "void", "long", "short", "unsigned", "signed", "bool", "_Bool", "size_t", "FILE",
    )
}
