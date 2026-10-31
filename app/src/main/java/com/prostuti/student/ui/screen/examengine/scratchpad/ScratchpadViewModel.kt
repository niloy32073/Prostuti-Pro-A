package com.prostuti.student.ui.screen.examengine.scratchpad

import androidx.lifecycle.ViewModel
import com.prostuti.student.domain.scratchpad.ScratchStroke
import com.prostuti.student.domain.scratchpad.StrokeEraser
import com.prostuti.student.domain.scratchpad.StrokePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The exam's scratch canvas. Its own ViewModel (scoped to the exam's navigation entry), so the
 * drawing survives rotation and tab switches without living in the exam ViewModel.
 * One pen line or one eraser drag is one undo step.
 */
class ScratchpadViewModel(private val eraser: StrokeEraser) : ViewModel() {

    private val _state = MutableStateFlow(ScratchpadUiState())
    val state: StateFlow<ScratchpadUiState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<List<ScratchStroke>>()
    private val redoStack = ArrayDeque<List<ScratchStroke>>()
    private var strokesBeforeErase: List<ScratchStroke>? = null

    fun selectTool(tool: ScratchTool) = _state.update { it.copy(tool = tool) }

    fun selectEraserMode(mode: EraserMode) = _state.update { it.copy(tool = ScratchTool.ERASER, eraserMode = mode) }

    fun addStroke(points: List<StrokePoint>, widthPx: Float) {
        if (points.isEmpty()) return
        val before = _state.value.strokes
        commit(before, before + ScratchStroke(points, widthPx))
    }

    fun beginErase() {
        strokesBeforeErase = _state.value.strokes
    }

    fun eraseAlong(from: StrokePoint, to: StrokePoint, radiusPx: Float) = _state.update { s ->
        val next = when (s.eraserMode) {
            EraserMode.PARTIAL -> eraser.erasePartial(s.strokes, from, to, radiusPx)
            EraserMode.STROKE -> eraser.eraseWhole(s.strokes, from, to, radiusPx)
        }
        s.copy(strokes = next)
    }

    fun endErase() {
        val before = strokesBeforeErase ?: return
        strokesBeforeErase = null
        val after = _state.value.strokes
        if (after != before) commit(before, after)
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_state.value.strokes)
        publish(previous)
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_state.value.strokes)
        publish(next)
    }

    fun clear() {
        val before = _state.value.strokes
        if (before.isNotEmpty()) commit(before, emptyList())
    }

    private fun commit(before: List<ScratchStroke>, after: List<ScratchStroke>) {
        undoStack.addLast(before)
        while (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        redoStack.clear()
        publish(after)
    }

    private fun publish(strokes: List<ScratchStroke>) = _state.update {
        it.copy(strokes = strokes, canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty())
    }

    private companion object {
        const val MAX_UNDO = 50
    }
}
