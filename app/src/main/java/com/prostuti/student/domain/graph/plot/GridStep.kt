package com.prostuti.student.domain.graph.plot

/** Grid spacing: [major] world units between labelled lines, [minorDivisions] thin lines in each. */
data class GridStep(val major: Double, val minorDivisions: Int, val piMultiple: Boolean)
