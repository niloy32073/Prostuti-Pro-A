package com.prostuti.student.domain.code.flow

/** Wraps node text into short lines, breaking at spaces, then after operators, then anywhere. */
internal object TextWrap {
    private const val BREAK_AFTER = ",;+-*/%=<>&|("

    fun wrap(text: String, maxChars: Int, maxLines: Int): List<String> {
        var rest = text.replace('\n', ' ').replace('\t', ' ').trim()
        val out = mutableListOf<String>()
        while (rest.isNotEmpty()) {
            if (rest.length <= maxChars) { out += rest; break }
            val space = rest.lastIndexOf(' ', maxChars)
            val cut = when {
                space >= maxChars / 3 -> space
                else -> (maxChars - 1 downTo maxChars / 3).firstOrNull { rest[it - 1] in BREAK_AFTER } ?: maxChars
            }
            out += rest.substring(0, cut).trimEnd()
            rest = rest.substring(cut).trimStart()
            if (out.size == maxLines && rest.isNotEmpty()) {
                out[out.lastIndex] = out.last().take(maxChars - 1) + "…"
                break
            }
        }
        return out.ifEmpty { listOf("") }
    }
}
