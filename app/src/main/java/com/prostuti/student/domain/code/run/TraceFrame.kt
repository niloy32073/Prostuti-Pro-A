package com.prostuti.student.domain.code.run

import kotlinx.serialization.Serializable

/** One function call on the stack: its name, the line it is at, and its variables in scope there. */
@Serializable
data class TraceFrame(val function: String, val line: Int, val vars: List<TraceVar>)
