package com.prostuti.student.ui.screen.playground.graph.space

import kotlinx.serialization.Serializable

/** 3D view settings from the settings sheet. */
@Serializable
data class SpaceSettings(
    val range: Double = DEFAULT_RANGE,
    val showBox: Boolean = true,
    val showAxes: Boolean = true,
    val showGrid: Boolean = true,
    val showNumbers: Boolean = true,
    /** Turn the graph by turning the phone (off by default). */
    val gyroscope: Boolean = false,
) {
    companion object {
        const val DEFAULT_RANGE = 5.0
    }
}
