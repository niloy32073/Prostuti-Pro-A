package com.prostuti.student.ui.screen.examengine.scratchpad

import com.prostuti.student.domain.scratchpad.ScratchStroke

data class ScratchpadUiState(
    val strokes: List<ScratchStroke> = emptyList(),
    val tool: ScratchTool = ScratchTool.PEN,
    val eraserMode: EraserMode = EraserMode.PARTIAL,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
)
