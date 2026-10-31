package com.prostuti.student.ui.screen.examengine.calculator

/** The input line: tokens plus a cursor that sits between tokens (0 = before the first). */
data class ExpressionEditor(val tokens: List<String> = emptyList(), val cursor: Int = tokens.size) {

    val isEmpty: Boolean get() = tokens.isEmpty()

    fun insert(token: String): ExpressionEditor {
        if (tokens.size >= MAX_TOKENS) return this
        val next = tokens.toMutableList().apply { add(cursor, token) }
        return ExpressionEditor(next, cursor + 1)
    }

    /** Deletes the token left of the cursor (at the very start, the one right of it). */
    fun delete(): ExpressionEditor = when {
        tokens.isEmpty() -> this
        cursor == 0 -> ExpressionEditor(tokens.drop(1), 0)
        else -> ExpressionEditor(tokens.toMutableList().apply { removeAt(cursor - 1) }, cursor - 1)
    }

    /** The cursor wraps around at both ends, like the real calculator. */
    fun moveLeft(): ExpressionEditor = copy(cursor = if (cursor == 0) tokens.size else cursor - 1)
    fun moveRight(): ExpressionEditor = copy(cursor = if (cursor == tokens.size) 0 else cursor + 1)

    companion object {
        const val MAX_TOKENS = 99
    }
}
