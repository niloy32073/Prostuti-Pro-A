package com.prostuti.student.domain.code.flow

/** The standard flowchart symbols. */
enum class NodeShape {
    /** Start / end: a rounded pill. */
    TERMINAL,
    /** A rectangle. */
    PROCESS,
    /** Input / output: a parallelogram. */
    IO,
    /** A condition: a diamond. */
    DECISION,
    /** A call to a function of the same program: a rectangle with side bars. */
    CALL,
    /** A connector circle with a label's name: where a `goto` goes, and the label it reaches. */
    CONNECTOR,
}
