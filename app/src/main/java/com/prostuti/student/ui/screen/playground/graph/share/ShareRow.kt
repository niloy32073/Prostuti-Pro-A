package com.prostuti.student.ui.screen.playground.graph.share

/** One line under the shared graph: its colour, what was typed, and its result. */
data class ShareRow(val color: Int, val text: String, val detail: String?, val isNote: Boolean = false)
