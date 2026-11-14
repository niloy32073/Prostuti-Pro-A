package com.prostuti.student.domain.code.run

import kotlinx.serialization.Serializable

/**
 * The program is paused before running [line]. [frames] is the call stack, outermost first (the
 * last one is running [line]); [globals] are C's global variables (Python's are the main frame's).
 */
@Serializable
data class TraceStep(val line: Int, val frames: List<TraceFrame>, val globals: List<TraceVar> = emptyList())
