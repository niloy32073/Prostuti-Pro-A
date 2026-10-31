package com.prostuti.student.ui.screen.examengine.scratchpad

import com.prostuti.student.domain.scratchpad.StrokeEraser
import com.prostuti.student.domain.scratchpad.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScratchpadViewModelTest {

    private val vm = ScratchpadViewModel(StrokeEraser())
    private val state get() = vm.state.value
    private val line = (0..10).map { StrokePoint(it * 10f, 0f) }

    @Test fun `adding a stroke enables undo`() {
        vm.addStroke(line, 2f)
        assertEquals(1, state.strokes.size)
        assertTrue(state.canUndo)
        assertFalse(state.canRedo)
    }

    @Test fun `partial erase splits and one undo restores the whole line`() {
        vm.addStroke(line, 2f)
        vm.selectEraserMode(EraserMode.PARTIAL)
        assertEquals(ScratchTool.ERASER, state.tool)

        vm.beginErase()
        vm.eraseAlong(StrokePoint(48f, 0f), StrokePoint(52f, 0f), 5f)
        vm.eraseAlong(StrokePoint(52f, 0f), StrokePoint(53f, 0f), 5f)
        vm.endErase()
        assertEquals(2, state.strokes.size)

        vm.undo()
        assertEquals(1, state.strokes.size)
        assertEquals(line, state.strokes.single().points)
        assertTrue(state.canRedo)

        vm.redo()
        assertEquals(2, state.strokes.size)
    }

    @Test fun `stroke eraser removes the line`() {
        vm.addStroke(line, 2f)
        vm.selectEraserMode(EraserMode.STROKE)
        vm.beginErase()
        vm.eraseAlong(StrokePoint(30f, 1f), StrokePoint(30f, 1f), 3f)
        vm.endErase()
        assertTrue(state.strokes.isEmpty())
    }

    @Test fun `erase gesture that touches nothing adds no undo step`() {
        vm.addStroke(line, 2f)
        vm.undo()
        assertFalse(state.canUndo)
        vm.beginErase()
        vm.eraseAlong(StrokePoint(0f, 300f), StrokePoint(10f, 300f), 3f)
        vm.endErase()
        assertFalse(state.canUndo)
        assertTrue(state.canRedo)
    }

    @Test fun `clear is undoable`() {
        vm.addStroke(line, 2f)
        vm.clear()
        assertTrue(state.strokes.isEmpty())
        vm.undo()
        assertEquals(1, state.strokes.size)
    }
}
