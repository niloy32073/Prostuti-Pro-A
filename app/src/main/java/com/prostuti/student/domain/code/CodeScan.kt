package com.prostuti.student.domain.code

/** Small text scans shared by the C and Python readers. String and char literals are skipped. */
internal object CodeScan {

    /** Names called as `name(`, not as a member (`a.name(` / `p->name(`). */
    fun calledNames(text: String): Set<String> {
        val out = mutableSetOf<String>()
        forEachWord(text) { name, start, end ->
            var j = end
            while (j < text.length && text[j] == ' ') j++
            if (j < text.length && text[j] == '(' && !isMember(text, start)) out += name
        }
        return out
    }

    /** Every identifier outside literals. */
    fun words(text: String): Set<String> {
        val out = mutableSetOf<String>()
        forEachWord(text) { name, _, _ -> out += name }
        return out
    }

    /** Index just after the literal that starts at [start] (a `'` or `"`; Python triple quotes too). */
    fun skipQuoted(text: String, start: Int): Int {
        val q = text[start]
        val triple = text.startsWith("$q$q$q", start)
        var i = start + if (triple) 3 else 1
        while (i < text.length) {
            val c = text[i]
            when {
                c == '\\' -> i += 2
                triple && text.startsWith("$q$q$q", i) -> return i + 3
                !triple && c == q -> return i + 1
                else -> i++
            }
        }
        return text.length
    }

    fun isIdentStart(c: Char): Boolean = c.isLetter() || c == '_'
    fun isIdentPart(c: Char): Boolean = c.isLetterOrDigit() || c == '_'

    private inline fun forEachWord(text: String, onWord: (String, Int, Int) -> Unit) {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' || c == '\'' -> i = skipQuoted(text, i)
                isIdentStart(c) -> {
                    val s = i
                    while (i < text.length && isIdentPart(text[i])) i++
                    // A word glued to a quote is a Python string prefix (f"...", rb'...'), not a name.
                    if (i < text.length && (text[i] == '"' || text[i] == '\'')) continue
                    onWord(text.substring(s, i), s, i)
                }
                c.isDigit() -> while (i < text.length && (isIdentPart(text[i]) || text[i] == '.')) i++
                else -> i++
            }
        }
    }

    private fun isMember(text: String, start: Int): Boolean {
        var k = start - 1
        while (k >= 0 && text[k] == ' ') k--
        if (k < 0) return false
        return text[k] == '.' || (text[k] == '>' && k > 0 && text[k - 1] == '-')
    }
}
