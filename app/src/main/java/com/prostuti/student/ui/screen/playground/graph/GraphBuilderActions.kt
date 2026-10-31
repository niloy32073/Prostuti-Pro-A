package com.prostuti.student.ui.screen.playground.graph

import com.prostuti.student.domain.graph.plot.LineOptions

/** Every user action of the graph builder, so [GraphBuilderContent] stays stateless and testable. */
class GraphBuilderActions(
    val onBack: () -> Unit = {},
    val onFocus: (Long) -> Unit = {},
    val onTextChange: (Long, String, Int, Int) -> Unit = { _, _, _, _ -> },
    val onKey: (MathKeyAction) -> Unit = {},
    val onAddLine: () -> Unit = {},
    val onDeleteLine: (Long) -> Unit = {},
    val onMoveLine: (Long, Int) -> Unit = { _, _ -> },
    val onToggleVisible: (Long) -> Unit = {},
    val onColor: (Long, Int) -> Unit = { _, _ -> },
    val onOptions: (Long, LineOptions) -> Unit = { _, _ -> },
    val onRangeTexts: (Long, String, String) -> Unit = { _, _, _ -> },
    val onSliderRangeTexts: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    val onSliderChange: (Long, Double) -> Unit = { _, _ -> },
    val onTogglePlay: (Long) -> Unit = {},
    val onAddSlider: (String, Long) -> Unit = { _, _ -> },
    val onAddExample: (GraphExample) -> Unit = {},
    val onCanvasSize: (Int, Int) -> Unit = { _, _ -> },
    val onTransform: (Float, Float, Float, Float, Float) -> Unit = { _, _, _, _, _ -> },
    val onTap: (Float, Float, Float) -> Unit = { _, _, _ -> },
    val onTraceDrag: (Float, Float, Float) -> Unit = { _, _, _ -> },
    val onClearTrace: () -> Unit = {},
    val onTangentAtTrace: () -> Unit = {},
    val onZoom: (Float) -> Unit = {},
    val onResetView: () -> Unit = {},
    val onSquareAxes: () -> Unit = {},
    val onSettings: (GraphSettings) -> Unit = {},
    val onApplyRanges: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    val onOpenSheet: (GraphSheet) -> Unit = {},
    val onCloseSheet: () -> Unit = {},
)
