package com.prostuti.student.domain.code.c.run

import com.prostuti.student.domain.code.CodeParseException
import com.prostuti.student.domain.code.c.CLexer
import com.prostuti.student.domain.code.c.CToken
import com.prostuti.student.domain.code.c.CTokenKind

/**
 * Handles `#include` (ignored: the library is built in), `#define` (object- and function-like
 * macros), `#undef`, conditional compilation (`#if`, `#ifdef`, `#ifndef`, `#elif`, `#else`,
 * `#endif`, with `defined`) and `#error`, and expands macros.
 */
internal class CPreprocessor {

    private class Macro(val params: List<String>?, val body: List<CToken>)

    /** One open `#if`: whether its current branch is kept, and whether an earlier branch was. */
    private class Cond(val line: Int, val outerActive: Boolean, var active: Boolean, var taken: Boolean, var sawElse: Boolean = false)

    private val macros = HashMap<String, Macro>()
    private val conds = ArrayDeque<Cond>()
    private val active get() = conds.lastOrNull()?.active ?: true

    init {
        PREDEFINED.forEach { (name, text) -> macros[name] = Macro(null, lex(text, 0)) }
    }

    fun process(tokens: List<CToken>): List<CToken> {
        val out = ArrayList<CToken>()
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            if (t.kind == CTokenKind.DIRECTIVE) {
                directive(t)
                i++
                continue
            }
            if (!active) { i++; continue }
            i = expand(tokens, i, out, emptySet())
        }
        conds.lastOrNull()?.let { fail(it.line, "this #if has no matching #endif") }
        return out
    }

    /** Expands the token at [i] into [out]; returns the index after what it consumed. */
    private fun expand(tokens: List<CToken>, i: Int, out: MutableList<CToken>, active: Set<String>): Int {
        val t = tokens[i]
        val macro = macros[t.text]?.takeIf { t.kind == CTokenKind.IDENT && t.text !in active }
            ?: run { out += t; return i + 1 }
        if (macro.params == null) {
            substitute(macro.body, emptyMap(), t.line, out, active + t.text)
            return i + 1
        }
        if (i + 1 >= tokens.size || !tokens[i + 1].isPunct("(")) { out += t; return i + 1 }
        // Arguments: split at top-level commas.
        val args = mutableListOf(mutableListOf<CToken>())
        var depth = 0
        var k = i + 2
        while (true) {
            if (k >= tokens.size) fail(t.line, "the macro '${t.text}' call is missing ')'")
            val a = tokens[k]
            if (a.isPunct("(")) depth++
            if (a.isPunct(")")) { if (depth == 0) break; depth-- }
            if (a.isPunct(",") && depth == 0) args += mutableListOf<CToken>() else args.last() += a
            k++
        }
        val actual = if (macro.params.isEmpty() && args.single().isEmpty()) emptyList() else args
        if (actual.size != macro.params.size) {
            fail(t.line, "the macro '${t.text}' needs ${macro.params.size} argument(s), got ${actual.size}")
        }
        substitute(macro.body, macro.params.zip(actual).toMap(), t.line, out, active + t.text)
        return k + 1
    }

    private fun substitute(body: List<CToken>, args: Map<String, List<CToken>>, line: Int, out: MutableList<CToken>, active: Set<String>) {
        val replaced = ArrayList<CToken>()
        for (b in body) {
            val arg = args[b.text]?.takeIf { b.kind == CTokenKind.IDENT }
            if (arg != null) replaced += arg else replaced += b.copy(line = line)
        }
        var j = 0
        while (j < replaced.size) j = expand(replaced, j, out, active)
    }

    private fun directive(t: CToken) {
        val text = t.text.removePrefix("#").trim()
        val word = text.takeWhile { it.isLetter() }
        val rest = text.substring(word.length)
        when (word) {
            "if", "ifdef", "ifndef" -> {
                val outer = active
                val keep = outer && when (word) {
                    "if" -> evalCondition(rest, t.line)
                    else -> {
                        val name = macroName(rest, word, t.line)
                        (name in macros) == (word == "ifdef")
                    }
                }
                conds.addLast(Cond(t.line, outer, keep, keep))
                return
            }
            "elif" -> {
                val c = conds.lastOrNull() ?: fail(t.line, "#elif without #if")
                if (c.sawElse) fail(t.line, "#elif after #else")
                val keep = c.outerActive && !c.taken && evalCondition(rest, t.line)
                c.active = keep
                c.taken = c.taken || keep
                return
            }
            "else" -> {
                val c = conds.lastOrNull() ?: fail(t.line, "#else without #if")
                if (c.sawElse) fail(t.line, "a second #else for the same #if")
                c.sawElse = true
                c.active = c.outerActive && !c.taken
                c.taken = true
                return
            }
            "endif" -> {
                conds.removeLastOrNull() ?: fail(t.line, "#endif without #if")
                return
            }
        }
        if (!active) return
        when (word) {
            "include", "pragma", "" -> Unit
            "define" -> define(rest, t.line)
            "undef" -> macros.remove(rest.trim())
            "error" -> fail(t.line, "#error${rest.trimEnd()}")
            "warning", "line" -> Unit
            else -> fail(t.line, "unknown directive #$word")
        }
    }

    private fun macroName(rest: String, word: String, line: Int): String {
        val name = rest.trim()
        if (name.isEmpty() || !name.all { it.isLetterOrDigit() || it == '_' }) fail(line, "#$word needs one macro name")
        return name
    }

    /** `#if`'s expression: `defined X`, macros expanded, other names are 0, then integer arithmetic. */
    private fun evalCondition(text: String, line: Int): Boolean {
        val raw = lex(text, line)
        if (raw.isEmpty()) fail(line, "#if needs a condition")
        val resolved = ArrayList<CToken>()
        var k = 0
        while (k < raw.size) {
            val t = raw[k]
            if (t.kind == CTokenKind.IDENT && t.text == "defined") {
                val paren = raw.getOrNull(k + 1)?.isPunct("(") == true
                val nameTok = raw.getOrNull(if (paren) k + 2 else k + 1)
                if (nameTok == null || nameTok.kind != CTokenKind.IDENT) fail(line, "defined needs a macro name")
                if (paren && raw.getOrNull(k + 3)?.isPunct(")") != true) fail(line, "defined( is missing ')'")
                resolved += nameTok.copy(kind = CTokenKind.NUMBER, text = if (nameTok.text in macros) "1" else "0")
                k += if (paren) 4 else 2
                continue
            }
            resolved += t
            k++
        }
        val expanded = ArrayList<CToken>()
        var j = 0
        while (j < resolved.size) j = expand(resolved, j, expanded, emptySet())
        return ConditionEval(expanded, line).value() != 0L
    }

    /** Integer constant expressions of `#if`. */
    private inner class ConditionEval(private val toks: List<CToken>, private val line: Int) {
        private var p = 0

        fun value(): Long {
            val v = ternary()
            if (p < toks.size) fail(line, "unexpected '${toks[p].text}' in the #if condition")
            return v
        }

        private fun ternary(): Long {
            val c = binary(0)
            if (!peek("?")) return c
            p++
            val a = ternary()
            if (!peek(":")) fail(line, "missing ':' in the #if condition")
            p++
            val b = ternary()
            return if (c != 0L) a else b
        }

        private fun binary(minPrec: Int): Long {
            var left = unary()
            while (p < toks.size) {
                val op = toks[p].takeIf { it.kind == CTokenKind.PUNCT }?.text ?: break
                val prec = PRECEDENCE[op] ?: break
                if (prec < minPrec) break
                p++
                val right = binary(prec + 1)
                left = when (op) {
                    "||" -> b(left != 0L || right != 0L); "&&" -> b(left != 0L && right != 0L)
                    "|" -> left or right; "^" -> left xor right; "&" -> left and right
                    "==" -> b(left == right); "!=" -> b(left != right)
                    "<" -> b(left < right); ">" -> b(left > right); "<=" -> b(left <= right); ">=" -> b(left >= right)
                    "<<" -> left shl right.toInt(); ">>" -> left shr right.toInt()
                    "+" -> left + right; "-" -> left - right; "*" -> left * right
                    "/" -> if (right == 0L) fail(line, "division by zero in #if") else left / right
                    else -> if (right == 0L) fail(line, "division by zero in #if") else left % right
                }
            }
            return left
        }

        private fun unary(): Long {
            val t = toks.getOrNull(p) ?: fail(line, "the #if condition ends too early")
            p++
            return when {
                t.isPunct("!") -> b(unary() == 0L)
                t.isPunct("-") -> -unary()
                t.isPunct("+") -> unary()
                t.isPunct("~") -> unary().inv()
                t.isPunct("(") -> ternary().also { if (!peek(")")) fail(line, "missing ')' in the #if condition"); p++ }
                t.kind == CTokenKind.NUMBER -> number(t.text)
                t.kind == CTokenKind.CHAR -> t.text.substringAfter('\'').dropLast(1).let { if (it.length == 1) it[0].code.toLong() else 0L }
                t.kind == CTokenKind.IDENT -> 0L          // an unknown name is 0, as in C
                else -> fail(line, "'${t.text}' cannot be used in an #if condition")
            }
        }

        private fun number(text: String): Long {
            val raw = text.lowercase().trimEnd('u', 'l')
            return try {
                when {
                    raw.startsWith("0x") -> raw.substring(2).toLong(16)
                    raw.length > 1 && raw.startsWith("0") -> raw.substring(1).toLong(8)
                    else -> raw.toLong()
                }
            } catch (_: NumberFormatException) {
                fail(line, "#if can only use whole numbers, not '$text'")
            }
        }

        private fun peek(s: String) = p < toks.size && toks[p].isPunct(s)
        private fun b(v: Boolean) = if (v) 1L else 0L
    }

    private fun define(rest: String, line: Int) {
        val trimmed = rest.trimStart()
        val name = trimmed.takeWhile { it.isLetterOrDigit() || it == '_' }
        if (name.isEmpty()) fail(line, "#define needs a name")
        val after = trimmed.substring(name.length)
        if (after.startsWith("(")) {
            val close = after.indexOf(')')
            if (close < 0) fail(line, "#define $name( is missing ')'")
            val params = after.substring(1, close).split(',').map { it.trim() }.filter { it.isNotEmpty() }
            macros[name] = Macro(params, lex(after.substring(close + 1), line))
        } else {
            macros[name] = Macro(null, lex(after, line))
        }
    }

    private fun lex(text: String, line: Int): List<CToken> {
        val tokens = try {
            CLexer(text.replace("\\\n", " ")).tokens()
        } catch (e: CodeParseException) {
            fail(line, "this #define cannot be read")
        }
        if (tokens.any { it.isPunct("#") || it.isPunct("##") }) fail(line, "# and ## in macros are not supported")
        return tokens.map { it.copy(line = line) }
    }

    private fun fail(line: Int, reason: String): Nothing = throw CProgramException(line, reason, isCompile = true)

    private companion object {
        val PRECEDENCE = mapOf(
            "||" to 1, "&&" to 2, "|" to 3, "^" to 4, "&" to 5,
            "==" to 6, "!=" to 6, "<" to 7, ">" to 7, "<=" to 7, ">=" to 7,
            "<<" to 8, ">>" to 8, "+" to 9, "-" to 9, "*" to 10, "/" to 10, "%" to 10,
        )
        val PREDEFINED = listOf(
            "NULL" to "((void*)0)",
            "EOF" to "(-1)",
            "RAND_MAX" to "2147483647",
            "INT_MAX" to "2147483647",
            "INT_MIN" to "(-2147483647-1)",
            "LONG_MAX" to "9223372036854775807L",
            "LONG_MIN" to "(-9223372036854775807L-1)",
            "CHAR_MAX" to "127",
            "CHAR_MIN" to "(-128)",
            "UINT_MAX" to "4294967295U",
            "EXIT_SUCCESS" to "0",
            "EXIT_FAILURE" to "1",
            "true" to "1",
            "false" to "0",
            "M_PI" to "3.14159265358979323846",
            "M_E" to "2.7182818284590452354",
        )
    }
}
