package com.prostuti.student.domain.graph

import kotlinx.serialization.Serializable

/** Best-fit families offered for a data table. */
@Serializable
enum class RegressionModel(val parameterCount: Int) {
    NONE(0),
    LINEAR(2), // y = m x + c
    QUADRATIC(3), // y = a x² + b x + c
    CUBIC(4), // y = a x³ + b x² + c x + d
    EXPONENTIAL(2), // y = a e^(b x)
    POWER(2), // y = a x^b
    LOGARITHMIC(2), // y = a + b ln x
}
