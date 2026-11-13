package com.prostuti.student.ui.screen.playground.graph

import com.prostuti.student.domain.graph.RegressionModel
import kotlinx.serialization.Serializable

/** A data table line as typed: columns of cell texts and the chosen best fit. */
@Serializable
data class TableState(
    val xs: List<String> = List(4) { "" },
    val ys: List<String> = List(4) { "" },
    val model: RegressionModel = RegressionModel.LINEAR,
)
