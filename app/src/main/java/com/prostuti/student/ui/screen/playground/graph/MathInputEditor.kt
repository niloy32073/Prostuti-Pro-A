package com.prostuti.student.ui.screen.playground.graph

/** Pure text editing for the math keyboard, so typing behaves the same everywhere and is testable. */
object MathInputEditor {

    /** Keyboard tokens deleted as one unit by backspace ("sin(" goes in one press, like it came in). */
    private val tokens = listOf(
        "sin⁻¹(", "cos⁻¹(", "tan⁻¹(", "d/dx(", "nthroot(", "floor(", "round(",
        "sinh(", "cosh(", "tanh(", "ceil(", "sqrt(", "cbrt(", "log_", "nCr(", "nPr(", "mod(", "min(", "max(",
        "sin(", "cos(", "tan(", "sec(", "csc(", "cot(", "log(", "abs(", "exp(",
        "ln(", "√(", "∛(", "e^(", "^(", "dx", "dt", "dθ",
    )

    fun insert(value: EditorValue, text: String, cursorBack: Int = 0): EditorValue {
        val before = value.text.substring(0, value.min)
        val selected = value.text.substring(value.min, value.max)
        val after = value.text.substring(value.max)
        val back = cursorBack.coerceIn(0, text.length)
        val head = text.substring(0, text.length - back)
        val tail = text.substring(text.length - back)
        return if (selected.isNotEmpty() && back > 0) {
            // Wrap: "x+1" selected, √( ) pressed → √(x+1) with the cursor after it.
            val newText = before + head + selected + tail + after
            EditorValue(newText, (before + head + selected + tail).length)
        } else {
            val newText = before + head + tail + after
            EditorValue(newText, (before + head).length)
        }
    }

    fun backspace(value: EditorValue): EditorValue {
        if (value.min != value.max) {
            return EditorValue(value.text.removeRange(value.min, value.max), value.min)
        }
        val cursor = value.min
        if (cursor == 0) return value
        val text = value.text
        // An empty pair "()" or "{}" around the cursor goes together.
        val prev = text[cursor - 1]
        val next = text.getOrNull(cursor)
        if ((prev == '(' && next == ')') || (prev == '{' && next == '}') || (prev == '|' && next == '|')) {
            val start = tokens.firstOrNull { text.startsWith(it, cursor - it.length) && it.endsWith(prev) }
                ?.let { cursor - it.length } ?: (cursor - 1)
            return EditorValue(text.removeRange(start, cursor + 1), start)
        }
        val token = tokens.firstOrNull { cursor >= it.length && text.startsWith(it, cursor - it.length) }
        val length = token?.length ?: 1
        return EditorValue(text.removeRange(cursor - length, cursor), cursor - length)
    }

    fun left(value: EditorValue): EditorValue =
        if (value.min != value.max) EditorValue(value.text, value.min) else EditorValue(value.text, (value.min - 1).coerceAtLeast(0))

    fun right(value: EditorValue): EditorValue =
        if (value.min != value.max) EditorValue(value.text, value.max) else EditorValue(value.text, (value.max + 1).coerceAtMost(value.text.length))
}
