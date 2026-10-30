package com.prostuti.student.ui.component

/**
 * Rewrites brace-less LaTeX arguments into explicit `{}` groups so our renderer
 * reads them the same way KaTeX/MathJax do on the web.
 *
 * In real TeX an argument without braces is exactly ONE token (one character or
 * one control word). Our parser instead grabs a whole run of text, so
 * `\dfrac12` became numerator "12" with an empty denominator.
 *
 *   \dfrac12      → \dfrac{1}{2}
 *   \frac\pi4     → \frac{\pi}{4}
 *   \sqrt[3]8     → \sqrt[3]{8}
 *   x^23          → x^{2}3        (same as KaTeX)
 *
 * Single linear pass, no regex — cost is negligible next to the LaTeX layout itself.
 */
internal object LatexNormalizer {

    private val twoArgCommands = setOf(
        "frac", "dfrac", "tfrac", "cfrac", "binom", "dbinom", "tbinom"
    )

    private val oneArgCommands = setOf(
        "vec", "hat", "bar", "dot", "ddot", "tilde", "widehat", "widetilde",
        "overline", "underline", "overrightarrow", "overleftarrow",
        "mathbf", "mathrm", "mathit", "mathbb", "mathcal", "boldsymbol"
    )

    /**
     * Commands missing from our renderer's symbol table, rewritten before anything
     * else. An unknown control word is drawn as the raw `\commandname` in the
     * error color instead of the intended symbol.
     *
     * `\rightleftharpoons` (⇌): the bundled KaTeX_Main font has the U+21CC glyph,
     * so draw the character itself. `\mathrm` pins it to Main (KaTeX_Math has no
     * such glyph) and `\;` gives it the thick spacing of a relation, as in KaTeX.
     * The library's `\ce{<=>}` is no substitute: it draws ⇔, not ⇌.
     */
    private val unsupportedCommands = listOf(
        Regex("""\\rightleftharpoons(?![A-Za-z])""") to "\\;\\mathrm{⇌}\\;"
    )

    /**
     * `%` starts a TeX comment — our tokenizer eats everything up to the next
     * newline when it sees a bare `%`, and `\%` (the standard escape for a
     * literal percent sign) isn't a registered command either, so it falls
     * through to the same "unknown command" error rendering. There is no way
     * to get a literal `%` glyph *inside* a formula with this renderer.
     *
     * Splits [formula] on every literal `%` (bare or `\`-escaped) so the
     * caller can render the pieces as separate formulas and drop a plain `%`
     * text character — outside LaTeX entirely — between them.
     *
     * Groups open at a split are closed and reopened so every piece stays
     * balanced: `\text{10\% NaCl}` → `\text{10}`, `\text{ NaCl}`. Groups left
     * empty by this are dropped. A `%` inside a multi-argument command such as
     * `\frac{a\%}{b}` can't be split cleanly and still renders wrong.
     */
    private val groupWrappers = setOf(
        "text", "mbox", "textrm", "textbf", "textit", "mathrm", "mathbf", "mathit",
        "mathsf", "mathtt", "boldsymbol", "ce", "cf"
    )

    fun splitOnPercent(formula: String): List<String> {
        if (formula.indexOf('%') < 0) return listOf(formula)

        val pieces = mutableListOf<String>()
        val buf = StringBuilder()
        val openers = ArrayList<String>()     // text that opened each still-open group
        val contentStart = ArrayList<Int>()   // buf length right after that opener
        val reopened = ArrayList<Boolean>()   // group was reopened by a split
        var wrapperStart = -1                 // start of a wrapper command awaiting its `{`

        // Author-written empty groups (`{}^{14}C`) are meaningful, so only groups
        // a split created or cut are dropped when empty.
        fun closeInnermost(atSplit: Boolean) {
            val last = openers.lastIndex
            if ((atSplit || reopened[last]) && contentStart[last] == buf.length) {
                buf.setLength(buf.length - openers[last].length)
            } else {
                buf.append('}')
            }
            openers.removeAt(last); contentStart.removeAt(last); reopened.removeAt(last)
        }

        fun split() {
            val saved = openers.toList()
            while (openers.isNotEmpty()) closeInnermost(atSplit = true)
            pieces.add(buf.toString())
            buf.setLength(0)
            for (opener in saved) {
                buf.append(opener)
                openers.add(opener); contentStart.add(buf.length); reopened.add(true)
            }
            wrapperStart = -1
        }

        var i = 0
        while (i < formula.length) {
            val c = formula[i]
            when {
                c == '\\' && i + 1 < formula.length && formula[i + 1] == '%' -> { split(); i += 2 }
                c == '%' -> { split(); i++ }
                c == '\\' -> {
                    val end = controlSequenceEnd(formula, i)
                    wrapperStart = if (formula.substring(i + 1, end) in groupWrappers) buf.length else -1
                    buf.append(formula, i, end)
                    i = end
                }
                c == '{' -> {
                    val start = if (wrapperStart >= 0) wrapperStart else buf.length
                    buf.append('{')
                    openers.add(buf.substring(start)); contentStart.add(buf.length); reopened.add(false)
                    wrapperStart = -1
                    i++
                }
                c == '}' && openers.isNotEmpty() -> { closeInnermost(atSplit = false); wrapperStart = -1; i++ }
                else -> {
                    if (!c.isWhitespace()) wrapperStart = -1
                    buf.append(c)
                    i++
                }
            }
        }
        pieces.add(buf.toString())
        return pieces
    }

    fun normalize(formula: String): String {
        var src = formula
        for ((pattern, replacement) in unsupportedCommands) {
            if (pattern.containsMatchIn(src)) {
                // The (String, String) overload treats `\` in the replacement as a
                // Matcher escape char and silently drops it, so use the literal
                // match-transform overload instead.
                src = pattern.replace(src) { replacement }
            }
        }
        // Fast path: nothing that can take a brace-less argument.
        if (src.indexOf('\\') < 0 && src.indexOf('^') < 0 && src.indexOf('_') < 0) {
            return src
        }
        val out = StringBuilder(src.length + 16)
        process(src, out)
        return out.toString()
    }

    private fun process(src: String, out: StringBuilder) {
        var i = 0
        while (i < src.length) {
            val c = src[i]
            when {
                c == '\\' -> {
                    val end = controlSequenceEnd(src, i)
                    val name = src.substring(i + 1, end)
                    // `\ ` (control space) isn't supported by our parser; `\;` is the closest width.
                    if (name.isNotEmpty() && name[0].isWhitespace()) {
                        out.append("\\;")
                    } else {
                        out.append(src, i, end)
                    }
                    i = end
                    when {
                        name in twoArgCommands -> {
                            i = argument(src, i, out)
                            i = argument(src, i, out)
                        }
                        name == "sqrt" -> {
                            i = optionalBracket(src, i, out)
                            i = argument(src, i, out)
                        }
                        name in oneArgCommands -> i = argument(src, i, out)
                    }
                }
                c == '^' || c == '_' -> {
                    out.append(c)
                    i = argument(src, i + 1, out)
                }
                else -> {
                    out.append(c)
                    i++
                }
            }
        }
    }

    /** Index just past `\name` (letters) or `\x` (single non-letter symbol). */
    private fun controlSequenceEnd(src: String, start: Int): Int {
        var j = start + 1
        if (j >= src.length) return j
        if (!src[j].isLetter()) return j + 1
        while (j < src.length && src[j].isLetter()) j++
        return j
    }

    /** Writes one argument as a `{...}` group and returns the index after it. */
    private fun argument(src: String, start: Int, out: StringBuilder): Int {
        var i = start
        while (i < src.length && src[i].isWhitespace()) i++
        if (i >= src.length) return i

        val c = src[i]
        return when {
            c == '{' -> {
                val close = matchingBrace(src, i)
                out.append('{')
                process(src.substring(i + 1, close), out)
                out.append('}')
                (close + 1).coerceAtMost(src.length)
            }
            // Nothing valid to wrap — leave it to the parser.
            c == '}' || c == '&' || c == '^' || c == '_' -> i
            c == '\\' -> {
                val end = controlSequenceEnd(src, i)
                out.append('{').append(src, i, end).append('}')
                end
            }
            else -> {
                val end = i + Character.charCount(src.codePointAt(i))
                out.append('{').append(src, i, end).append('}')
                end
            }
        }
    }

    /** Copies an optional `[...]` (e.g. root index of \sqrt) as-is. */
    private fun optionalBracket(src: String, start: Int, out: StringBuilder): Int {
        var i = start
        while (i < src.length && src[i].isWhitespace()) i++
        if (i >= src.length || src[i] != '[') return start
        val close = src.indexOf(']', i)
        if (close < 0) return start
        out.append('[')
        process(src.substring(i + 1, close), out)
        out.append(']')
        return close + 1
    }

    /** Index of the `}` matching the `{` at [open]; src.length if unbalanced. */
    private fun matchingBrace(src: String, open: Int): Int {
        var depth = 0
        var i = open
        while (i < src.length) {
            when (src[i]) {
                '\\' -> { i += 2; continue }   // skip escaped \{ \}
                '{' -> depth++
                '}' -> { depth--; if (depth == 0) return i }
            }
            i++
        }
        return src.length
    }
}
