package com.prostuti.student.ui.screen.playground.graph

/** Which bottom sheet is open. */
sealed interface GraphSheet {
    data object Settings : GraphSheet
    data object Examples : GraphSheet
    data class LineSettings(val lineId: Long) : GraphSheet
}
