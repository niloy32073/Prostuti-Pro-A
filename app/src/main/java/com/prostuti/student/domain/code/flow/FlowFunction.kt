package com.prostuti.student.domain.code.flow

/**
 * One flowchart: a function, or the program's entry ([isEntry]: C `main`, Python's top-level code).
 * [header] is what the start symbol shows for a non-entry function, e.g. `int fact(int n)`.
 */
data class FlowFunction(
    val name: String,
    val header: String,
    val line: Int,
    val body: List<FlowBlock>,
    val isEntry: Boolean,
)
