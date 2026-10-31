package com.prostuti.student.ui.screen.playground.graph

import kotlinx.serialization.Serializable

/** What survives process death (kept in SavedStateHandle as JSON). */
@Serializable
data class SavedGraph(
    val lines: List<GraphLineState>,
    val settings: GraphSettings,
    val viewport: GraphViewport,
    val nextId: Long,
)
