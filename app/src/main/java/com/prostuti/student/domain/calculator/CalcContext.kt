package com.prostuti.student.domain.calculator

/** Everything an expression may read while it is evaluated. */
data class CalcContext(
    val variables: Map<String, Double> = emptyMap(),
    val ans: Double = 0.0,
    val angleUnit: AngleUnit = AngleUnit.DEG,
    val random: () -> Double = { Math.random() },
) {
    fun variable(name: String): Double = variables[name] ?: 0.0
    fun with(name: String, value: Double): CalcContext = copy(variables = variables + (name to value))
}
