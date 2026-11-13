package com.prostuti.student.domain.graph

/** A data table line: x and y columns as typed (numbers or constant expressions) and its fit. */
data class TableInput(val xs: List<String>, val ys: List<String>, val model: RegressionModel)
