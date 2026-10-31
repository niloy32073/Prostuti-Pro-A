package com.prostuti.student.ui.screen.playground.graph.component

import com.prostuti.student.ui.screen.playground.graph.MathKeyAction

internal enum class KeyStyle { PLAIN, DIGIT, OPERATOR, ACTION }

/** One key: what it shows, what it does, and its share of the row width. */
internal data class MathKey(
    val label: String,
    val action: MathKeyAction,
    val style: KeyStyle = KeyStyle.PLAIN,
    val weight: Float = 1f,
    /** Stable id for tests and accessibility; defaults to the label. */
    val id: String = label,
)
