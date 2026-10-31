package com.prostuti.student.ui.screen.playground.graph

/** A line's text with its selection (start == end is a plain cursor). */
data class EditorValue(val text: String, val selectionStart: Int, val selectionEnd: Int = selectionStart) {
    val min: Int get() = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
    val max: Int get() = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
}
