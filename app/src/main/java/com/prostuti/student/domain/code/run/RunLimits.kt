package com.prostuti.student.domain.code.run

/** Limits that stop a runaway student program. */
object RunLimits {
    /** Running time; time spent waiting for input does not count. */
    const val TIME_MS = 15_000L
    const val OUTPUT_CHARS = 200_000
}
