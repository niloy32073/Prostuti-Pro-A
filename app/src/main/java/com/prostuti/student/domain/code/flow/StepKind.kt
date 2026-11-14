package com.prostuti.student.domain.code.flow

/** What a plain step does; it picks the flowchart symbol. */
enum class StepKind {
    /** Assignment, declaration, calculation: a rectangle. */
    PROCESS,
    /** Reads input or prints output: a parallelogram. */
    IO,
    /** Calls a function written in the same program: a rectangle with side bars. */
    CALL,
}
