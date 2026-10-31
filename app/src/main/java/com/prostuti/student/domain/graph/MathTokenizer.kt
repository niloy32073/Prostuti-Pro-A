package com.prostuti.student.domain.graph

/**
 * Splits a typed line into tokens. Accepts both keyboard symbols (×, ÷, −, √, π, θ, ², ≤, ∫, Σ)
 * and plain ASCII (*, /, -, sqrt, pi, theta, ^2, <=, int).
 *
 * Letter runs are split Desmos-style: known words first (longest wins), otherwise one letter per
 * variable — so "xsinx" is x·sin(x) and "ab" is a·b. A single letter may carry a subscript: a_1, b_{12}.
 */
internal object MathTokenizer {

    private val superscripts = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9',
    )

    fun tokenize(text: String): List<MathToken> {
        val out = ArrayList<MathToken>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || (c == '.' && text.getOrNull(i + 1)?.isDigit() == true) -> i = number(text, i, out)
                c in superscripts || c == '⁻' -> i = superscript(text, i, out)
                c.isLetter() && c !in SPECIAL_LETTERS -> i = letters(text, i, out)
                else -> i = symbol(text, i, out)
            }
        }
        return out
    }

    private fun number(text: String, start: Int, out: MutableList<MathToken>): Int {
        var i = start
        var seenDot = false
        while (i < text.length && (text[i].isDigit() || (text[i] == '.' && !seenDot))) {
            if (text[i] == '.') {
                // "2.x" — the dot is not part of the number if no digit follows it.
                if (text.getOrNull(i + 1)?.isDigit() != true) break
                seenDot = true
            }
            i++
        }
        val raw = text.substring(start, i)
        out += MathToken(TokenType.NUMBER, start, i, raw, raw.toDouble())
        return i
    }

    /** x²³ → x ^ 23, x⁻¹ → x ^ - 1. */
    private fun superscript(text: String, start: Int, out: MutableList<MathToken>): Int {
        out += MathToken(TokenType.POWER, start, start)
        var i = start
        if (text[i] == '⁻') {
            out += MathToken(TokenType.MINUS, i, i + 1)
            i++
        }
        val digitsStart = i
        val digits = StringBuilder()
        while (i < text.length && text[i] in superscripts) {
            digits.append(superscripts.getValue(text[i]))
            i++
        }
        if (digits.isEmpty()) throw GraphParseException(GraphError(GraphErrorKind.SYNTAX, start))
        out += MathToken(TokenType.NUMBER, digitsStart, i, digits.toString(), digits.toString().toDouble())
        return i
    }

    private fun letters(text: String, start: Int, out: MutableList<MathToken>): Int {
        var end = start
        while (end < text.length && text[end].isLetter() && text[end] !in SPECIAL_LETTERS) end++
        var i = start
        while (i < end) {
            val word = MathFunctions.words.firstOrNull { text.startsWith(it, i) && i + it.length <= end }
            if (word != null) {
                val name = MathFunctions.wordSymbols[word] ?: word
                out += MathToken(TokenType.IDENT, i, i + word.length, name)
                i += word.length
                continue
            }
            // One letter, maybe with a subscript (only on the run's last letter: "ab_1" is a·b_1).
            if (i == end - 1 && text.getOrNull(end) == '_') {
                val sub = subscript(text, end + 1)
                if (sub != null) {
                    out += MathToken(TokenType.IDENT, i, sub.second, text[i] + "_" + sub.first)
                    return sub.second
                }
            }
            out += MathToken(TokenType.IDENT, i, i + 1, text[i].toString())
            i++
        }
        return end
    }

    /** `_1`, `_12`, `_{12}`, `_a` → the subscript text and the index after it. */
    private fun subscript(text: String, start: Int): Pair<String, Int>? {
        if (start >= text.length) return null
        if (text[start] == '{') {
            val close = text.indexOf('}', start)
            if (close < 0) return null
            val inner = text.substring(start + 1, close)
            return if (inner.isNotEmpty() && inner.all { it.isLetterOrDigit() }) inner to close + 1 else null
        }
        var i = start
        while (i < text.length && text[i].isLetterOrDigit()) i++
        return if (i > start) text.substring(start, i) to i else null
    }

    private fun symbol(text: String, i: Int, out: MutableList<MathToken>): Int {
        val c = text[i]
        val next = text.getOrNull(i + 1)
        fun add(type: TokenType, length: Int = 1, name: String = ""): Int {
            out += MathToken(type, i, i + length, name)
            return i + length
        }
        return when (c) {
            '+' -> add(TokenType.PLUS)
            '-', '−', '–' -> add(TokenType.MINUS)
            '*', '×', '·', '⋅', '∗' -> add(TokenType.TIMES)
            '/', '÷' -> add(TokenType.DIVIDE)
            '^' -> add(TokenType.POWER)
            '!' -> add(TokenType.FACTORIAL)
            '\'', '′' -> add(TokenType.PRIME)
            '″' -> { out += MathToken(TokenType.PRIME, i, i + 1); add(TokenType.PRIME) }
            ',' -> add(TokenType.COMMA)
            ':' -> add(TokenType.COLON)
            '|' -> add(TokenType.BAR)
            '_' -> add(TokenType.UNDERSCORE)
            '(', '[' -> add(TokenType.OPEN_PAREN)
            ')', ']' -> add(TokenType.CLOSE_PAREN)
            '{' -> add(TokenType.OPEN_BRACE)
            '}' -> add(TokenType.CLOSE_BRACE)
            '=' -> add(TokenType.EQ)
            '<' -> if (next == '=') add(TokenType.LE, 2) else add(TokenType.LT)
            '>' -> if (next == '=') add(TokenType.GE, 2) else add(TokenType.GT)
            '≤', '⩽' -> add(TokenType.LE)
            '≥', '⩾' -> add(TokenType.GE)
            '∫' -> add(TokenType.INTEGRAL)
            'Σ', '∑' -> add(TokenType.SUM)
            '∏', 'Π' -> add(TokenType.PRODUCT)
            '√' -> add(TokenType.IDENT, name = "sqrt")
            '∛' -> add(TokenType.IDENT, name = "cbrt")
            'π', 'τ', 'θ', '∞' -> add(TokenType.IDENT, name = c.toString())
            else -> throw GraphParseException(GraphError(GraphErrorKind.UNKNOWN_CHARACTER, i, listOf(c.toString())))
        }
    }

    /** Letters that are single-symbol tokens even inside a letter run (π, θ…), never part of a word. */
    private val SPECIAL_LETTERS = setOf('π', 'τ', 'θ', 'Σ', 'Π')
}
