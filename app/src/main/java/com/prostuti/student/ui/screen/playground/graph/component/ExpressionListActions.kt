package com.prostuti.student.ui.screen.playground.graph.component

/** Callbacks of the expression list, grouped so the screen passes one object down. */
class ExpressionListActions(
    val onFocus: (Long) -> Unit,
    val onTextChange: (Long, String, Int, Int) -> Unit,
    val onToggleVisible: (Long) -> Unit,
    val onOptions: (Long) -> Unit,
    val onDelete: (Long) -> Unit,
    val onSliderChange: (Long, Double) -> Unit,
    val onTogglePlay: (Long) -> Unit,
    val onAddSlider: (name: String, afterId: Long) -> Unit,
    val onAddLine: () -> Unit,
    val onAddTable: () -> Unit = {},
    val onTableChange: (Long, com.prostuti.student.ui.screen.playground.graph.TableState) -> Unit = { _, _ -> },
)
