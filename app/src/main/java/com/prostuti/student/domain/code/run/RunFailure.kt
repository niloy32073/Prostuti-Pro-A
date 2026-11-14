package com.prostuti.student.domain.code.run

enum class RunFailure {
    /** Ran longer than the time limit (time spent waiting for input does not count). */
    TIME_LIMIT,
    /** Printed more than the output limit. */
    OUTPUT_LIMIT,
    /** The Python process died (usually out of memory). */
    CRASHED,
}
