package com.prostuti.student.ui.screen.playground.graph

import org.junit.Assert.assertEquals
import org.junit.Test

class MathInputEditorTest {

    @Test fun insertsAtTheCursor() {
        val v = MathInputEditor.insert(EditorValue("y = x", 4), "2")
        assertEquals(EditorValue("y = 2x", 5), v)
    }

    @Test fun templatePlacesTheCursorInside() {
        val v = MathInputEditor.insert(EditorValue("", 0), "sin()", 1)
        assertEquals(EditorValue("sin()", 4), v)
    }

    @Test fun templateWrapsTheSelection() {
        val v = MathInputEditor.insert(EditorValue("x+1", 0, 3), "√()", 1)
        assertEquals(EditorValue("√(x+1)", 6), v)
    }

    @Test fun backspaceRemovesWholeFunctionTokens() {
        assertEquals(EditorValue("y = ", 4), MathInputEditor.backspace(EditorValue("y = sin(", 8)))
        // an empty pair around the cursor goes with its function name
        assertEquals(EditorValue("y = ", 4), MathInputEditor.backspace(EditorValue("y = sin()", 8)))
        assertEquals(EditorValue("y = x", 5), MathInputEditor.backspace(EditorValue("y = x2", 6)))
        assertEquals(EditorValue("", 0), MathInputEditor.backspace(EditorValue("", 0)))
    }

    @Test fun backspaceDeletesTheSelection() {
        assertEquals(EditorValue("y = ", 4), MathInputEditor.backspace(EditorValue("y = x^2", 4, 7)))
    }

    @Test fun arrowsMoveAndCollapseTheSelection() {
        assertEquals(EditorValue("abc", 1), MathInputEditor.left(EditorValue("abc", 2)))
        assertEquals(EditorValue("abc", 0), MathInputEditor.left(EditorValue("abc", 0)))
        assertEquals(EditorValue("abc", 3), MathInputEditor.right(EditorValue("abc", 1, 3)))
    }
}
