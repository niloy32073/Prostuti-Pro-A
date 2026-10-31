package com.prostuti.student.ui.screen.playground.graph

import kotlinx.serialization.Serializable

@Serializable
data class SliderRange(val min: Double = -10.0, val max: Double = 10.0, val step: Double = 0.1)
