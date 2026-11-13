package com.prostuti.student.ui.screen.playground.graph.space

import com.prostuti.student.domain.graph.space.Lines3
import com.prostuti.student.domain.graph.space.Mesh3

/** One thing the renderer draws: a lit mesh and/or thin lines in a colour (ARGB). */
internal class SceneItem(
    val mesh: Mesh3?,
    val lines: Lines3?,
    val color: Int,
    val opacity: Float = 1f,
    val lit: Boolean = true,
    val heightColor: Boolean = false,
    val heightMin: Float = 0f,
    val heightMax: Float = 0f,
    val lineAlpha: Float = 0.35f,
    /** Clip to the box (graphs) or not (axes, which poke out a little). */
    val clip: Boolean = true,
    /** World-size multiplier: axes are built once and stretched while you pinch. */
    val scale: Float = 1f,
)
