package com.prostuti.student.ui.screen.playground.graph.space

/** Everything for one frame. Immutable: the GL thread may read it while the UI makes the next one. */
internal class SpaceScene(
    val items: List<SceneItem>,
    val camera: SpaceCamera,
    /** Live half-size of the box (world units); meshes may have been made for a slightly different one. */
    val range: Double,
    val background: Int,
)
