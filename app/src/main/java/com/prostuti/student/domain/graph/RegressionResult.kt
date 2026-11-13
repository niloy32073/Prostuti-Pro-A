package com.prostuti.student.domain.graph

/**
 * A fitted curve. [equation] is parser text at full precision (what is drawn); [display] is the
 * same equation rounded for reading. [r] is Pearson's correlation (straight-line fits only).
 */
class RegressionResult(
    val model: RegressionModel,
    val coefficients: DoubleArray,
    val equation: String,
    val display: String,
    val rSquared: Double,
    val r: Double?,
)
