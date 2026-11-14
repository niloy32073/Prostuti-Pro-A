package com.prostuti.student.domain.code.flow

/** One `case` of a C switch or Python match. [test] is the diamond text; null is `default` / `case _`. */
data class SwitchCase(val test: String?, val body: List<FlowBlock>, val line: Int)
