package com.prostuti.student.ui.screen.playground.graph

import com.prostuti.student.domain.graph.GraphSpace
import com.prostuti.student.ui.screen.playground.graph.space.SpaceCamera
import com.prostuti.student.ui.screen.playground.graph.space.SpaceSettings
import kotlinx.serialization.Serializable

/** What survives process death (kept in SavedStateHandle as JSON). */
@Serializable
data class SavedGraph(
    val lines: List<GraphLineState>,
    val settings: GraphSettings,
    val viewport: GraphViewport,
    val nextId: Long,
    val spaceLines: List<GraphLineState> = emptyList(),
    val space: GraphSpace = GraphSpace.PLANE,
    val camera: SpaceCamera = SpaceCamera(),
    val spaceSettings: SpaceSettings = SpaceSettings(),
    /** Set once the graph is saved in "My graphs", so later saves overwrite the same entry. */
    val savedId: Long? = null,
    val savedName: String? = null,
)
