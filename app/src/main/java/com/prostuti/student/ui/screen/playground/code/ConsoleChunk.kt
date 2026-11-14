package com.prostuti.student.ui.screen.playground.code

/** A run of console text of one [kind]; consecutive output of the same kind is merged. */
data class ConsoleChunk(val text: String, val kind: ConsoleKind)
