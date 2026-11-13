package com.prostuti.student.ui.screen.playground.graph.share

import com.prostuti.student.domain.graph.GraphLineKind
import com.prostuti.student.domain.graph.GraphNumberFormat
import com.prostuti.student.ui.screen.playground.graph.GraphBuilderUiState

/** The visible lines of the current view as rows for the shared picture. */
object ShareRows {
    fun from(state: GraphBuilderUiState, colors: List<Int>, undefined: String): List<ShareRow> =
        state.lines.mapNotNull { line ->
            val status = state.status(line.id)
            val color = colors[Math.floorMod(line.colorIndex, colors.size)]
            when {
                line.table != null -> {
                    if (!line.visible) return@mapNotNull null
                    val stats = status.fitRSquared?.let { "R² = " + GraphNumberFormat.format(it, 5) }
                    ShareRow(color, status.fitText ?: "(${status.pointCount})", stats)
                }
                line.text.isBlank() -> null
                line.text.trimStart().startsWith("#") -> ShareRow(color, line.text.trimStart().removePrefix("#").trim(), null, isNote = true)
                !line.visible && status.drawsSomething -> null
                else -> ShareRow(color, GraphShareImage.pretty(line.text), detail(status.kind, status.value, status.exact, undefined))
            }
        }

    private fun detail(kind: GraphLineKind, value: Double?, exact: String?, undefined: String): String? {
        if (value == null || kind == GraphLineKind.PARAMETER) return null
        if (value.isNaN()) return "= $undefined"
        val approx = kind == GraphLineKind.INTEGRAL || kind == GraphLineKind.VOLUME || kind == GraphLineKind.REVOLUTION
        return (if (approx) "≈ " else "= ") + GraphNumberFormat.format(value) + (exact?.let { "  (= $it)" } ?: "")
    }
}
