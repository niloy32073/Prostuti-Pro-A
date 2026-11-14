package com.prostuti.student.domain.code.flow

/** Every flowchart of a program, the entry first. Empty when the code has nothing to draw. */
data class FlowProgram(val functions: List<FlowFunction>)
