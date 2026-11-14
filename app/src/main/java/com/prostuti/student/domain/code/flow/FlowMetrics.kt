package com.prostuti.student.domain.code.flow

/**
 * Sizes the layout works with, in the drawing's units (pixels on screen). [charWidth] is the width of
 * one code character, which spaces the symbols; [startLabel] / [endLabel] are the entry chart's
 * terminal texts in the app language.
 */
data class FlowMetrics(
    val lineHeight: Float,
    val charWidth: Float,
    val startLabel: String,
    val endLabel: String,
    val maxChars: Int = 28,
    val maxDecisionChars: Int = 24,
    val maxLines: Int = 5,
)
