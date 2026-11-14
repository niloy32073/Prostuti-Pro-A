package com.prostuti.student.domain.code.run

/** What a paused program does next. */
enum class TraceCommand {
    /** Run until the next line and pause again. */
    STEP,
    /** Stop pausing and run to the end. */
    RUN_TO_END,
}
