package com.prostuti.student.ui.screen.playground.graph

import androidx.annotation.StringRes
import com.prostuti.student.domain.graph.RegressionModel

/**
 * One example or template: its name, the lines it adds, and optionally a note shown as the first
 * line, slider ranges for its parameters, a window that frames it, a data table, and whether it
 * belongs to the 3D view.
 */
data class GraphExample(
    @StringRes val labelRes: Int,
    val lines: List<String>,
    @StringRes val noteRes: Int? = null,
    val sliders: Map<String, SliderRange> = emptyMap(),
    /** xMin, xMax, yMin, yMax (2D) — or just the box half-size in [0] for 3D. */
    val window: List<Double>? = null,
    val space: Boolean = false,
    val table: TableState? = null,
)
