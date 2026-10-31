package com.prostuti.student.ui.screen.playground.graph

/** What a math-keyboard key does to the focused line. */
sealed interface MathKeyAction {
    /**
     * Inserts [text]; the cursor ends [cursorBack] characters before its end ("sin()" with 1 lands
     * inside the brackets). With a selection, the selection goes where the cursor would land.
     */
    data class Insert(val text: String, val cursorBack: Int = 0) : MathKeyAction
    data object Backspace : MathKeyAction
    data object Left : MathKeyAction
    data object Right : MathKeyAction
    data object Enter : MathKeyAction
    data object Hide : MathKeyAction
    data object SystemKeyboard : MathKeyAction
    data class Page(val page: KeyboardPage) : MathKeyAction
}
