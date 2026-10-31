package com.prostuti.student.ui.screen.playground.graph

import com.prostuti.student.domain.graph.plot.LineOptions
import kotlinx.serialization.Serializable

/** What the student owns about one line: the text, cursor, colour and drawing options. */
@Serializable
data class GraphLineState(
    val id: Long,
    val text: String = "",
    val selectionStart: Int = text.length,
    val selectionEnd: Int = selectionStart,
    val colorIndex: Int = 0,
    val visible: Boolean = true,
    val options: LineOptions = LineOptions(),
    val slider: SliderRange = SliderRange(),
)
