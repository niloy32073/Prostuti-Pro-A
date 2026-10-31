package com.prostuti.student.ui.screen.playground.graph

import com.prostuti.student.domain.calculator.AngleUnit
import kotlinx.serialization.Serializable

/** Graph-wide settings from the settings sheet. */
@Serializable
data class GraphSettings(
    val angleUnit: AngleUnit = AngleUnit.RAD,
    val showGrid: Boolean = true,
    val showAxes: Boolean = true,
    val showNumbers: Boolean = true,
    val piXAxis: Boolean = false,
)
