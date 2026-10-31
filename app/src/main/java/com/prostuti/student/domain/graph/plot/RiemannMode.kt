package com.prostuti.student.domain.graph.plot

import kotlinx.serialization.Serializable

@Serializable
enum class RiemannMode { NONE, LEFT, RIGHT, MIDPOINT, TRAPEZOID }
